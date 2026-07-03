package ng.lendstack.collateral;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.audit.AuditService;
import ng.lendstack.collateral.dto.CollateralRequest;
import ng.lendstack.collateral.dto.CollateralResponse;
import ng.lendstack.collateral.dto.CollateralVerifyRequest;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.domain.Collateral;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.domain.enums.VerificationStatus;
import ng.lendstack.repository.CollateralRepository;
import ng.lendstack.repository.LoanRepository;
import ng.lendstack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CollateralService {

    private static final List<LoanStatus> EDITABLE_STATES = List.of(
        LoanStatus.DRAFT, LoanStatus.SUBMITTED, LoanStatus.UNDER_REVIEW,
        LoanStatus.CREDIT_CHECK, LoanStatus.PENDING_GUARANTOR, LoanStatus.PENDING_COLLATERAL);

    private final CollateralRepository collateralRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public CollateralResponse declare(UUID borrowerId, UUID loanId, CollateralRequest request) {
        Loan loan = ownedLoan(borrowerId, loanId);
        if (!EDITABLE_STATES.contains(loan.getStatus())) {
            throw ApiException.conflict("COLLATERAL_LOCKED",
                "Collateral can no longer be added once the loan is " + loan.getStatus());
        }
        Collateral collateral = collateralRepository.save(Collateral.builder()
            .loan(loan)
            .type(request.type())
            .description(request.description())
            .estimatedValue(request.estimatedValue())
            .build());
        auditService.record("COLLATERAL", collateral.getId().toString(), "COLLATERAL_DECLARED",
            null, Map.of("loan", loan.getReference(), "type", request.type().name(),
                "estimatedValue", request.estimatedValue()), null);
        return CollateralResponse.from(collateral);
    }

    @Transactional(readOnly = true)
    public List<CollateralResponse> forOwnLoan(UUID borrowerId, UUID loanId) {
        ownedLoan(borrowerId, loanId);
        return forLoan(loanId);
    }

    @Transactional(readOnly = true)
    public List<CollateralResponse> forLoan(UUID loanId) {
        return collateralRepository.findByLoanId(loanId).stream()
            .map(CollateralResponse::from).toList();
    }


    @Transactional
    public CollateralResponse verify(UUID officerId, UUID collateralId, CollateralVerifyRequest request) {
        if (request.status() == VerificationStatus.UNVERIFIED) {
            throw ApiException.badRequest("INVALID_VERDICT", "Verdict must be VERIFIED or REJECTED");
        }
        if (request.status() == VerificationStatus.REJECTED
                && (request.reason() == null || request.reason().isBlank())) {
            throw ApiException.badRequest("REASON_REQUIRED", "Rejecting collateral requires a reason");
        }
        Collateral collateral = collateralRepository.findById(collateralId)
            .orElseThrow(() -> ApiException.notFound("Collateral record not found"));
        VerificationStatus before = collateral.getVerificationStatus();
        collateral.setVerificationStatus(request.status());
        collateral.setVerifiedBy(userRepository.getReferenceById(officerId));
        collateral.setVerifiedAt(Instant.now());
        collateral.setRejectionReason(
            request.status() == VerificationStatus.REJECTED ? request.reason() : null);
        collateralRepository.save(collateral);
        auditService.record("COLLATERAL", collateral.getId().toString(), "COLLATERAL_VERIFIED",
            Map.of("status", before.name()), Map.of("status", request.status().name()),
            request.reason());
        return CollateralResponse.from(collateral);
    }

    @Transactional(readOnly = true)
    public boolean hasVerifiedCollateral(UUID loanId) {
        return collateralRepository.countByLoanIdAndVerificationStatus(
            loanId, VerificationStatus.VERIFIED) > 0;
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
