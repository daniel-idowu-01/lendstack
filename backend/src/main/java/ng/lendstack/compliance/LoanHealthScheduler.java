package ng.lendstack.compliance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.audit.AuditService;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.RepaymentInstallment;
import ng.lendstack.domain.enums.InstallmentStatus;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.loan.LoanLifecycleService;
import ng.lendstack.repository.RepaymentInstallmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily loan-health sweep (idempotent — safe to run any number of times):
 * 1. PENDING installments past due date → OVERDUE; their loan → DELINQUENT.
 * 2. OVERDUE installments past the grace period accrue the configured daily
 *    penalty: totalDue × rate% × days-beyond-grace (recomputed, not compounded).
 * 3. Installments overdue ≥ the configured default threshold (default 90 days)
 *    push the loan DELINQUENT → DEFAULTED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanHealthScheduler {

    private static final ZoneId LAGOS = ZoneId.of("Africa/Lagos");

    private final RepaymentInstallmentRepository installmentRepository;
    private final LoanLifecycleService lifecycleService;
    private final LoanPolicyService policyService;
    private final AuditService auditService;

    @Scheduled(cron = "0 15 0 * * *", zone = "Africa/Lagos")
    @Transactional
    public void sweep() {
        LocalDate today = LocalDate.now(LAGOS);
        markOverdue(today);
        accruePenaltiesAndDefaults(today);
    }

    private void markOverdue(LocalDate today) {
        List<RepaymentInstallment> newlyOverdue = installmentRepository
            .findByStatusAndDueDateBefore(InstallmentStatus.PENDING, today);
        for (RepaymentInstallment installment : newlyOverdue) {
            installment.setStatus(InstallmentStatus.OVERDUE);
            installmentRepository.save(installment);
            auditService.record("REPAYMENT", installment.getId().toString(), "INSTALLMENT_OVERDUE",
                Map.of("status", "PENDING"), Map.of("status", "OVERDUE"),
                "Due date " + installment.getDueDate() + " passed without full payment");
            Loan loan = installment.getLoan();
            if (loan.getStatus() == LoanStatus.ACTIVE) {
                lifecycleService.transition(loan, LoanStatus.DELINQUENT,
                    "Installment %d overdue".formatted(installment.getInstallmentNumber()));
            }
        }
        if (!newlyOverdue.isEmpty()) {
            log.info("Marked {} installment(s) OVERDUE", newlyOverdue.size());
        }
    }

    private void accruePenaltiesAndDefaults(LocalDate today) {
        int graceDays = policyService.gracePeriodDays();
        BigDecimal dailyRate = policyService.penaltyDailyRatePercent()
            .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        int daysToDefault = policyService.daysToDefault();

        for (RepaymentInstallment installment
                : installmentRepository.findByStatus(InstallmentStatus.OVERDUE)) {
            long daysOverdue = ChronoUnit.DAYS.between(installment.getDueDate(), today);
            long chargeableDays = Math.max(0, daysOverdue - graceDays);
            BigDecimal penalty = installment.getTotalDue()
                .multiply(dailyRate)
                .multiply(BigDecimal.valueOf(chargeableDays))
                .setScale(2, RoundingMode.HALF_UP);
            if (penalty.compareTo(installment.getPenaltyDue()) != 0) {
                BigDecimal before = installment.getPenaltyDue();
                installment.setPenaltyDue(penalty);
                installmentRepository.save(installment);
                auditService.record("REPAYMENT", installment.getId().toString(), "PENALTY_ACCRUED",
                    Map.of("penaltyDue", before), Map.of("penaltyDue", penalty),
                    "%d day(s) beyond the %d-day grace period".formatted(chargeableDays, graceDays));
            }
            Loan loan = installment.getLoan();
            if (daysOverdue >= daysToDefault && loan.getStatus() == LoanStatus.DELINQUENT) {
                lifecycleService.transition(loan, LoanStatus.DEFAULTED,
                    "Installment %d overdue for %d days (threshold: %d)"
                        .formatted(installment.getInstallmentNumber(), daysOverdue, daysToDefault));
            }
        }
    }
}
