package ng.lendstack.repayment;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.audit.AuditService;
import ng.lendstack.collateral.CollateralService;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.loan.LoanLifecycleService;
import ng.lendstack.loan.LoanMapper;
import ng.lendstack.loan.dto.LoanResponse;
import ng.lendstack.notification.NotificationService;
import ng.lendstack.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Disbursement: money out to the borrower's bank account.
 *
 * ==== STUB — REPLACE WITH REAL TRANSFER RAIL ====
 * The actual bank transfer is simulated (logged + audit-flagged). To go live,
 * wire {@link #executeTransfer} to Paystack Transfers or NIP, funded from the
 * committed lender wallets. Everything around it — the collateral gate, the
 * schedule generation, the state transitions — is production logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisbursementService {

    private final LoanRepository loanRepository;
    private final LoanLifecycleService lifecycleService;
    private final CollateralService collateralService;
    private final ScheduleService scheduleService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final LoanMapper loanMapper;

    @Transactional
    public LoanResponse disburse(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> ApiException.notFound("Loan not found"));
        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw ApiException.conflict("WRONG_STATE", "Only APPROVED loans can be disbursed");
        }
        // Final gate: CBN §8 — no disbursement without verified collateral where required.
        if (loan.isCollateralRequired() && !collateralService.hasVerifiedCollateral(loanId)) {
            throw ApiException.conflict("COLLATERAL_UNVERIFIED",
                "This loan requires VERIFIED collateral before disbursement");
        }

        executeTransfer(loan);

        loan.setOutstandingPrincipal(loan.getAmount());
        lifecycleService.transition(loan, LoanStatus.DISBURSED, "Funds released to borrower (stub transfer)");
        int installments = scheduleService.generate(loan).size();
        lifecycleService.transition(loan, LoanStatus.ACTIVE,
            "Repayment schedule generated (%d monthly installments, reducing balance)"
                .formatted(installments));

        notificationService.enqueue(loan.getBorrower().getEmail(),
            loan.getBorrower().getFullName(),
            "Your loan has been disbursed",
            ("₦%,.2f has been sent to your registered bank account. Your first of %d monthly "
                + "installments is due next month — see your repayment dashboard.")
                .formatted(loan.getAmount(), installments),
            "DISBURSEMENT", "LOAN", loan.getId().toString());
        return loanMapper.toResponse(loan);
    }

    private void executeTransfer(Loan loan) {
        log.info("[STUB TRANSFER] ₦{} to borrower for loan {} — replace with Paystack Transfers/NIP",
            loan.getAmount(), loan.getReference());
        auditService.record("LOAN", loan.getId().toString(), "DISBURSEMENT_EXECUTED",
            null, Map.of("amount", loan.getAmount(), "channel", "STUB_TRANSFER"),
            "Stubbed bank transfer — pending real payment-rail integration");
    }
}
