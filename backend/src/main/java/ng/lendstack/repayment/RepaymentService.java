package ng.lendstack.repayment;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.audit.AuditService;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.domain.Lender;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.LoanFunding;
import ng.lendstack.domain.PaymentTransaction;
import ng.lendstack.domain.RepaymentInstallment;
import ng.lendstack.domain.enums.InstallmentStatus;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.domain.enums.PaymentStatus;
import ng.lendstack.integration.paystack.PaystackClient;
import ng.lendstack.loan.LoanLifecycleService;
import ng.lendstack.repayment.dto.InstallmentResponse;
import ng.lendstack.repayment.dto.PaymentInitResponse;
import ng.lendstack.repayment.dto.ScheduleResponse;
import ng.lendstack.repository.LoanFundingRepository;
import ng.lendstack.repository.LoanRepository;
import ng.lendstack.repository.PaymentTransactionRepository;
import ng.lendstack.repository.RepaymentInstallmentRepository;
import ng.lendstack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repayments via Paystack checkout. Payment application is idempotent (webhook
 * and redirect-verify can both fire) and distributes principal + interest to
 * funding lenders pro-rata. Penalties are retained by the platform. When no
 * PENDING/OVERDUE installments remain, the loan closes automatically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepaymentService {

    private final RepaymentInstallmentRepository installmentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final LoanFundingRepository fundingRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final PaystackClient paystackClient;
    private final LoanLifecycleService lifecycleService;
    private final AuditService auditService;

    @Value("${lendstack.paystack.callback-url}")
    private String callbackUrl;

    @Transactional(readOnly = true)
    public ScheduleResponse schedule(UUID borrowerId, UUID loanId) {
        Loan loan = ownedLoan(borrowerId, loanId);
        List<RepaymentInstallment> installments =
            installmentRepository.findByLoanIdOrderByInstallmentNumber(loanId);
        BigDecimal totalOutstanding = installments.stream()
            .filter(i -> i.getStatus() == InstallmentStatus.PENDING
                || i.getStatus() == InstallmentStatus.OVERDUE)
            .map(i -> i.getTotalDue().add(i.getPenaltyDue()).subtract(i.getAmountPaid()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate nextDue = installments.stream()
            .filter(i -> i.getStatus() == InstallmentStatus.PENDING
                || i.getStatus() == InstallmentStatus.OVERDUE)
            .map(RepaymentInstallment::getDueDate)
            .min(LocalDate::compareTo)
            .orElse(null);
        return new ScheduleResponse(loan.getId().toString(), loan.getReference(),
            loan.getStatus(), loan.getAmount(), loan.getInterestRateAnnual(),
            loan.getOutstandingPrincipal(), totalOutstanding, nextDue,
            installments.stream().map(InstallmentResponse::from).toList());
    }

    /** Creates a Paystack checkout for what remains on one installment (incl. penalty). */
    @Transactional
    public PaymentInitResponse initializePayment(UUID borrowerId, UUID installmentId) {
        RepaymentInstallment installment = installmentRepository.findById(installmentId)
            .orElseThrow(() -> ApiException.notFound("Installment not found"));
        Loan loan = installment.getLoan();
        if (!loan.getBorrower().getId().equals(borrowerId)) {
            throw ApiException.notFound("Installment not found");
        }
        if (installment.getStatus() != InstallmentStatus.PENDING
                && installment.getStatus() != InstallmentStatus.OVERDUE) {
            throw ApiException.conflict("NOT_PAYABLE",
                "This installment is " + installment.getStatus());
        }
        BigDecimal amount = installment.getTotalDue().add(installment.getPenaltyDue())
            .subtract(installment.getAmountPaid());
        if (amount.signum() <= 0) {
            throw ApiException.conflict("NOTHING_DUE", "Nothing is due on this installment");
        }
        String reference = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        var init = paystackClient.initializeTransaction(
            loan.getBorrower().getEmail(), amount, reference, callbackUrl);
        PaymentTransaction tx = transactionRepository.save(PaymentTransaction.builder()
            .loan(loan)
            .installment(installment)
            .reference(reference)
            .amount(amount)
            .authorizationUrl(init.authorizationUrl())
            .build());
        auditService.record("REPAYMENT", tx.getId().toString(), "PAYMENT_INITIALIZED",
            null, Map.of("reference", reference, "amount", amount,
                "installment", installment.getInstallmentNumber()), null);
        return new PaymentInitResponse(reference, amount, init.authorizationUrl());
    }

    /** Webhook entry point — signature already verified by the controller. */
    @Transactional
    public void handleWebhookEvent(JsonNode event) {
        if (!"charge.success".equals(event.path("event").asText())) {
            return;
        }
        JsonNode data = event.path("data");
        applySuccessfulPayment(
            data.path("reference").asText(),
            data.path("channel").asText(null),
            data.path("gateway_response").asText(null));
    }

    /**
     * Redirect-callback fallback: the frontend hits this after Paystack sends
     * the borrower back. Confirms the charge server-side with Paystack, then
     * applies it (no-ops if the webhook already did).
     */
    @Transactional
    public InstallmentResponse verifyAndApply(UUID borrowerId, String reference) {
        PaymentTransaction tx = transactionRepository.findByReference(reference)
            .orElseThrow(() -> ApiException.notFound("Unknown payment reference"));
        if (!tx.getLoan().getBorrower().getId().equals(borrowerId)) {
            throw ApiException.notFound("Unknown payment reference");
        }
        if (tx.getStatus() != PaymentStatus.SUCCESS) {
            JsonNode data = paystackClient.verifyTransaction(reference);
            String status = data.path("status").asText();
            if ("success".equals(status)) {
                applySuccessfulPayment(reference, data.path("channel").asText(null),
                    data.path("gateway_response").asText(null));
            } else if ("failed".equals(status) || "abandoned".equals(status)) {
                tx.setStatus("failed".equals(status) ? PaymentStatus.FAILED : PaymentStatus.ABANDONED);
                tx.setGatewayResponse(data.path("gateway_response").asText(null));
                transactionRepository.save(tx);
            }
        }
        return InstallmentResponse.from(tx.getInstallment());
    }

    private void applySuccessfulPayment(String reference, String channel, String gatewayResponse) {
        PaymentTransaction tx = transactionRepository.findByReference(reference).orElse(null);
        if (tx == null) {
            log.warn("Paystack event for unknown reference {} — ignoring", reference);
            return;
        }
        if (tx.getStatus() == PaymentStatus.SUCCESS) {
            return; // idempotent: webhook + verify can both land
        }
        tx.setStatus(PaymentStatus.SUCCESS);
        tx.setPaidAt(Instant.now());
        tx.setChannel(channel);
        tx.setGatewayResponse(gatewayResponse);
        transactionRepository.save(tx);

        RepaymentInstallment installment = tx.getInstallment();
        Loan loan = installment.getLoan();
        BigDecimal paidBefore = installment.getAmountPaid();
        installment.setAmountPaid(paidBefore.add(tx.getAmount()));

        boolean settled = installment.getAmountPaid()
            .compareTo(installment.getTotalDue().add(installment.getPenaltyDue())) >= 0;
        if (settled) {
            InstallmentStatus before = installment.getStatus();
            installment.setStatus(InstallmentStatus.PAID);
            installment.setPaidAt(Instant.now());
            loan.setOutstandingPrincipal(
                loan.getOutstandingPrincipal().subtract(installment.getPrincipalDue()).max(BigDecimal.ZERO));
            distributeToLenders(loan, installment);
            auditService.record("REPAYMENT", installment.getId().toString(), "REPAYMENT_RECEIVED",
                Map.of("status", before.name(), "amountPaid", paidBefore),
                Map.of("status", "PAID", "amountPaid", installment.getAmountPaid(),
                    "reference", reference), null);
        } else {
            auditService.record("REPAYMENT", installment.getId().toString(), "PARTIAL_PAYMENT",
                Map.of("amountPaid", paidBefore),
                Map.of("amountPaid", installment.getAmountPaid(), "reference", reference), null);
        }
        installmentRepository.save(installment);
        cureOrClose(loan);
    }

    /**
     * Pro-rata split by funded share. The last lender absorbs rounding so the
     * distributed total always equals the collected amount. Penalties are not
     * distributed (platform revenue).
     */
    private void distributeToLenders(Loan loan, RepaymentInstallment installment) {
        List<LoanFunding> fundings = fundingRepository.findByLoanId(loan.getId());
        if (fundings.isEmpty()) {
            return;
        }
        BigDecimal principalLeft = installment.getPrincipalDue();
        BigDecimal interestLeft = installment.getInterestDue();
        for (int i = 0; i < fundings.size(); i++) {
            LoanFunding funding = fundings.get(i);
            BigDecimal principalShare;
            BigDecimal interestShare;
            if (i == fundings.size() - 1) {
                principalShare = principalLeft;
                interestShare = interestLeft;
            } else {
                BigDecimal ratio = funding.getAmount()
                    .divide(loan.getAmount(), 10, RoundingMode.HALF_UP);
                principalShare = installment.getPrincipalDue().multiply(ratio)
                    .setScale(2, RoundingMode.HALF_UP);
                interestShare = installment.getInterestDue().multiply(ratio)
                    .setScale(2, RoundingMode.HALF_UP);
            }
            funding.setPrincipalRepaid(funding.getPrincipalRepaid().add(principalShare));
            funding.setInterestEarned(funding.getInterestEarned().add(interestShare));
            fundingRepository.save(funding);

            Lender lender = funding.getLender();
            lender.setWalletBalance(lender.getWalletBalance().add(principalShare).add(interestShare));
            lender.setCurrentExposure(lender.getCurrentExposure().subtract(principalShare)
                .max(BigDecimal.ZERO));

            principalLeft = principalLeft.subtract(principalShare);
            interestLeft = interestLeft.subtract(interestShare);
        }
    }

    /** DELINQUENT loans cure when nothing is overdue; loans with nothing left open close. */
    private void cureOrClose(Loan loan) {
        long stillOpen = installmentRepository.countByLoanIdAndStatusIn(loan.getId(),
            List.of(InstallmentStatus.PENDING, InstallmentStatus.OVERDUE));
        if (stillOpen == 0
                && (loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.DELINQUENT)) {
            loan.setOutstandingPrincipal(BigDecimal.ZERO);
            lifecycleService.transition(loan, LoanStatus.CLOSED,
                "All installments settled — loan fully repaid");
            return;
        }
        if (loan.getStatus() == LoanStatus.DELINQUENT) {
            long overdue = installmentRepository.countByLoanIdAndStatusIn(loan.getId(),
                List.of(InstallmentStatus.OVERDUE));
            if (overdue == 0) {
                lifecycleService.transition(loan, LoanStatus.ACTIVE,
                    "Arrears cleared — loan back in good standing");
            }
        }
    }

    /** Officer waiver: installment is forgiven; lender exposure is released without repayment. */
    @Transactional
    public InstallmentResponse waive(UUID officerId, UUID installmentId, String reason) {
        RepaymentInstallment installment = installmentRepository.findById(installmentId)
            .orElseThrow(() -> ApiException.notFound("Installment not found"));
        if (installment.getStatus() != InstallmentStatus.PENDING
                && installment.getStatus() != InstallmentStatus.OVERDUE) {
            throw ApiException.conflict("NOT_WAIVABLE",
                "Only PENDING or OVERDUE installments can be waived");
        }
        InstallmentStatus before = installment.getStatus();
        installment.setStatus(InstallmentStatus.WAIVED);
        installment.setWaivedBy(userRepository.getReferenceById(officerId));
        installment.setWaivedReason(reason);
        installmentRepository.save(installment);

        Loan loan = installment.getLoan();
        loan.setOutstandingPrincipal(loan.getOutstandingPrincipal()
            .subtract(installment.getPrincipalDue()).max(BigDecimal.ZERO));
        releaseExposure(loan, installment.getPrincipalDue());
        auditService.record("REPAYMENT", installment.getId().toString(), "INSTALLMENT_WAIVED",
            Map.of("status", before.name()), Map.of("status", "WAIVED"), reason);
        cureOrClose(loan);
        return InstallmentResponse.from(installment);
    }

    /** Waived principal is a realized lender loss: exposure released, wallet not credited. */
    private void releaseExposure(Loan loan, BigDecimal principal) {
        List<LoanFunding> fundings = fundingRepository.findByLoanId(loan.getId());
        BigDecimal left = principal;
        for (int i = 0; i < fundings.size(); i++) {
            LoanFunding funding = fundings.get(i);
            BigDecimal share = i == fundings.size() - 1 ? left
                : principal.multiply(funding.getAmount()
                    .divide(loan.getAmount(), 10, RoundingMode.HALF_UP))
                    .setScale(2, RoundingMode.HALF_UP);
            Lender lender = funding.getLender();
            lender.setCurrentExposure(lender.getCurrentExposure().subtract(share)
                .max(BigDecimal.ZERO));
            left = left.subtract(share);
        }
    }

    private Loan ownedLoan(UUID borrowerId, UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> ApiException.notFound("Loan not found"));
        if (!loan.getBorrower().getId().equals(borrowerId)) {
            throw ApiException.notFound("Loan not found");
        }
        return loan;
    }
}
