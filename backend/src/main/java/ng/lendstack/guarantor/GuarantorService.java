package ng.lendstack.guarantor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.audit.AuditService;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.compliance.LoanPolicyService;
import ng.lendstack.domain.Guarantor;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.GuarantorStatus;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.guarantor.dto.GuarantorDecisionRequest;
import ng.lendstack.guarantor.dto.GuarantorInviteView;
import ng.lendstack.guarantor.dto.GuarantorRequest;
import ng.lendstack.guarantor.dto.GuarantorResponse;
import ng.lendstack.loan.LoanLifecycleService;
import ng.lendstack.notification.NotificationService;
import ng.lendstack.repository.GuarantorRepository;
import ng.lendstack.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class GuarantorService {


    private static final List<LoanStatus> EDITABLE_STATES = List.of(
        LoanStatus.DRAFT, LoanStatus.SUBMITTED, LoanStatus.UNDER_REVIEW,
        LoanStatus.CREDIT_CHECK, LoanStatus.PENDING_GUARANTOR);

    private final GuarantorRepository guarantorRepository;
    private final LoanRepository loanRepository;
    private final LoanLifecycleService lifecycleService;
    private final LoanPolicyService policyService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Value("${lendstack.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Transactional
    public GuarantorResponse add(UUID borrowerId, UUID loanId, GuarantorRequest request) {
        Loan loan = ownedLoan(borrowerId, loanId);
        if (!EDITABLE_STATES.contains(loan.getStatus())) {
            throw ApiException.conflict("GUARANTORS_LOCKED",
                "Guarantors can no longer be changed once the loan is " + loan.getStatus());
        }
        if (guarantorRepository.findByLoanId(loanId).stream()
                .filter(g -> g.getStatus() != GuarantorStatus.DECLINED
                    && g.getStatus() != GuarantorStatus.EXPIRED)
                .count() >= 2) {
            throw ApiException.conflict("GUARANTOR_LIMIT", "A loan can have at most 2 active guarantors");
        }
        Guarantor guarantor = guarantorRepository.save(Guarantor.builder()
            .loan(loan)
            .fullName(request.fullName())
            .email(request.email())
            .phone(request.phone())
            .relationship(request.relationship())
            .occupation(request.occupation())
            .monthlyIncome(request.monthlyIncome())
            .responseToken(UUID.randomUUID().toString().replace("-", ""))
            .build());
        auditService.record("GUARANTOR", guarantor.getId().toString(), "GUARANTOR_ADDED",
            null, Map.of("loan", loan.getReference(), "email", request.email()), null);

        if (loan.getStatus() == LoanStatus.PENDING_GUARANTOR) {
            activate(guarantor);
        }
        return GuarantorResponse.from(guarantor);
    }

    @Transactional(readOnly = true)
    public List<GuarantorResponse> forLoan(UUID loanId) {
        return guarantorRepository.findByLoanId(loanId).stream()
            .map(GuarantorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<GuarantorResponse> forOwnLoan(UUID borrowerId, UUID loanId) {
        ownedLoan(borrowerId, loanId);
        return forLoan(loanId);
    }


    @Transactional
    public void activateRequests(Loan loan) {
        guarantorRepository.findByLoanId(loan.getId()).stream()
            .filter(g -> g.getStatus() == GuarantorStatus.PENDING && g.getRequestedAt() == null)
            .forEach(this::activate);
    }

    @Transactional(readOnly = true)
    public long acceptedCount(UUID loanId) {
        return guarantorRepository.countByLoanIdAndStatus(loanId, GuarantorStatus.ACCEPTED);
    }


    @Transactional(readOnly = true)
    public GuarantorInviteView invite(String token) {
        Guarantor g = byToken(token);
        return new GuarantorInviteView(g.getFullName(), g.getLoan().getBorrower().getFullName(),
            g.getLoan().getAmount(), g.getLoan().getTenureMonths(), g.getLoan().getPurpose(),
            g.getStatus(), g.getExpiresAt());
    }

    @Transactional
    public GuarantorInviteView respond(String token, GuarantorDecisionRequest decision) {
        Guarantor guarantor = byToken(token);
        if (guarantor.getStatus() != GuarantorStatus.PENDING) {
            throw ApiException.conflict("ALREADY_RESPONDED",
                "This guarantor request is " + guarantor.getStatus());
        }
        if (guarantor.getExpiresAt() != null && guarantor.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.conflict("EXPIRED", "This guarantor request has expired");
        }
        Loan loan = guarantor.getLoan();
        guarantor.setRespondedAt(Instant.now());
        if (decision.accept()) {
            guarantor.setStatus(GuarantorStatus.ACCEPTED);
            guarantorRepository.save(guarantor);
            auditService.record("GUARANTOR", guarantor.getId().toString(), "GUARANTOR_ACCEPTED",
                Map.of("status", "PENDING"), Map.of("status", "ACCEPTED"), null);
            maybeAdvance(loan);
        } else {
            guarantor.setStatus(GuarantorStatus.DECLINED);
            guarantor.setDeclineReason(decision.declineReason());
            guarantorRepository.save(guarantor);
            auditService.record("GUARANTOR", guarantor.getId().toString(), "GUARANTOR_DECLINED",
                Map.of("status", "PENDING"), Map.of("status", "DECLINED"), decision.declineReason());
            if (loan.getStatus() == LoanStatus.PENDING_GUARANTOR) {
                lifecycleService.transition(loan, LoanStatus.UNDER_REVIEW,
                    "Guarantor %s declined".formatted(guarantor.getFullName()));
            }
        }
        return invite(token);
    }


    @Transactional
    public void maybeAdvance(Loan loan) {
        if (loan.getStatus() == LoanStatus.PENDING_GUARANTOR
                && acceptedCount(loan.getId()) >= loan.getGuarantorsRequired()) {
            lifecycleService.transition(loan, LoanStatus.PENDING_COLLATERAL,
                "All required guarantors accepted");
        }
    }


    @Scheduled(fixedDelayString = "PT5M")
    @Transactional
    public void expireStaleRequests() {
        List<Guarantor> stale = guarantorRepository
            .findByStatusAndExpiresAtBefore(GuarantorStatus.PENDING, Instant.now());
        for (Guarantor guarantor : stale) {
            guarantor.setStatus(GuarantorStatus.EXPIRED);
            guarantorRepository.save(guarantor);
            auditService.record("GUARANTOR", guarantor.getId().toString(), "GUARANTOR_EXPIRED",
                Map.of("status", "PENDING"), Map.of("status", "EXPIRED"),
                "No response within the configured window");
            Loan loan = guarantor.getLoan();
            if (loan.getStatus() == LoanStatus.PENDING_GUARANTOR) {
                lifecycleService.transition(loan, LoanStatus.UNDER_REVIEW,
                    "Guarantor request to %s expired without a response"
                        .formatted(guarantor.getFullName()));
            }
        }
        if (!stale.isEmpty()) {
            log.info("Expired {} stale guarantor request(s)", stale.size());
        }
    }

    private void activate(Guarantor guarantor) {
        Instant now = Instant.now();
        guarantor.setRequestedAt(now);
        guarantor.setExpiresAt(now.plus(Duration.ofHours(policyService.guarantorExpiryHours())));
        guarantorRepository.save(guarantor);
        Loan loan = guarantor.getLoan();
        String link = "%s/guarantor/%s".formatted(frontendBaseUrl, guarantor.getResponseToken());
        notificationService.enqueue(guarantor.getEmail(), guarantor.getFullName(),
            "You have been named as a loan guarantor",
            ("%s has named you as guarantor for a loan of ₦%,.2f over %d months. "
                + "Review and respond within %d hours: %s")
                .formatted(loan.getBorrower().getFullName(), loan.getAmount(),
                    loan.getTenureMonths(), policyService.guarantorExpiryHours(), link),
            "GUARANTOR_REQUEST", "GUARANTOR", guarantor.getId().toString());
        auditService.record("GUARANTOR", guarantor.getId().toString(), "GUARANTOR_REQUESTED",
            null, Map.of("expiresAt", guarantor.getExpiresAt().toString()), null);
    }

    private Guarantor byToken(String token) {
        return guarantorRepository.findByResponseToken(token)
            .orElseThrow(() -> ApiException.notFound("This guarantor link is not valid"));
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
