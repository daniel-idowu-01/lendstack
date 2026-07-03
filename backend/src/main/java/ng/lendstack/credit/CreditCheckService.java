package ng.lendstack.credit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.audit.AuditService;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.credit.CreditScoringService.BorrowerHistory;
import ng.lendstack.credit.CreditScoringService.ScoreCard;
import ng.lendstack.credit.dto.CreditAssessmentResponse;
import ng.lendstack.credit.dto.ScoreOverrideRequest;
import ng.lendstack.domain.BorrowerProfile;
import ng.lendstack.domain.CreditAssessment;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.integration.bvn.BvnVerificationService;
import ng.lendstack.loan.LoanLifecycleService;
import ng.lendstack.repository.BorrowerProfileRepository;
import ng.lendstack.repository.CreditAssessmentRepository;
import ng.lendstack.repository.GuarantorRepository;
import ng.lendstack.repository.LoanRepository;
import ng.lendstack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCheckService {

    private static final List<LoanStatus> OPEN_STATUSES = List.of(
        LoanStatus.APPROVED, LoanStatus.DISBURSED, LoanStatus.ACTIVE,
        LoanStatus.DELINQUENT, LoanStatus.DEFAULTED);
    private static final List<LoanStatus> BAD_HISTORY_STATUSES = List.of(
        LoanStatus.DEFAULTED, LoanStatus.WRITTEN_OFF);

    private final LoanRepository loanRepository;
    private final BorrowerProfileRepository profileRepository;
    private final GuarantorRepository guarantorRepository;
    private final CreditAssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final CreditScoringService scoringService;
    private final BvnVerificationService bvnVerificationService;
    private final LoanLifecycleService lifecycleService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;


    @Transactional
    public CreditAssessmentResponse runCreditCheck(UUID loanId, UUID officerId) {
        Loan loan = loan(loanId);
        if (loan.getStatus() != LoanStatus.UNDER_REVIEW && loan.getStatus() != LoanStatus.CREDIT_CHECK) {
            throw ApiException.conflict("WRONG_STATE",
                "Credit check can only run while a loan is UNDER_REVIEW or CREDIT_CHECK");
        }
        BorrowerProfile profile = profileRepository.findByUserId(loan.getBorrower().getId())
            .orElseThrow(() -> ApiException.conflict("KYC_INCOMPLETE",
                "Borrower has no KYC profile"));

        verifyBvn(profile, loan);

        UUID borrowerId = loan.getBorrower().getId();
        BorrowerHistory history = new BorrowerHistory(
            openLoansExcluding(borrowerId, loan.getId()),
            loanRepository.countByBorrowerIdAndStatusIn(borrowerId, List.of(LoanStatus.CLOSED)),
            loanRepository.countByBorrowerIdAndStatusIn(borrowerId, BAD_HISTORY_STATUSES) > 0);

        ScoreCard card = scoringService.assess(loan, profile,
            guarantorRepository.findByLoanId(loan.getId()), history);

        CreditAssessment assessment = saveAssessment(loan, officerId, card.score(),
            card.riskTier(), toJson(card.breakdown()), false, null);
        auditService.record("CREDIT_ASSESSMENT", assessment.getId().toString(), "CREDIT_CHECK_RUN",
            null, Map.of("score", card.score(), "riskTier", card.riskTier().name()), null);

        if (loan.getStatus() == LoanStatus.UNDER_REVIEW) {
            lifecycleService.transition(loan, LoanStatus.CREDIT_CHECK,
                "Credit check run — score %d, tier %s".formatted(card.score(), card.riskTier()));
        }
        return toResponse(assessment);
    }


    @Transactional
    public CreditAssessmentResponse overrideScore(UUID loanId, UUID officerId,
                                                  ScoreOverrideRequest request) {
        Loan loan = loan(loanId);
        if (loan.getStatus() != LoanStatus.CREDIT_CHECK) {
            throw ApiException.conflict("WRONG_STATE",
                "Score can only be overridden while the loan is in CREDIT_CHECK");
        }
        CreditAssessment latest = assessmentRepository
            .findFirstByLoanIdOrderByCreatedAtDesc(loan.getId())
            .orElseThrow(() -> ApiException.conflict("NO_ASSESSMENT",
                "Run the credit check before overriding its result"));

        CreditAssessment assessment = saveAssessment(loan, officerId, request.score(),
            request.riskTier(), latest.getBreakdown(), true, request.reason());
        auditService.record("CREDIT_ASSESSMENT", assessment.getId().toString(), "SCORE_OVERRIDE",
            Map.of("score", latest.getScore(), "riskTier", latest.getRiskTier().name()),
            Map.of("score", request.score(), "riskTier", request.riskTier().name()),
            request.reason());
        return toResponse(assessment);
    }

    @Transactional(readOnly = true)
    public List<CreditAssessmentResponse> assessmentsFor(UUID loanId) {
        return assessmentRepository.findByLoanIdOrderByCreatedAtDesc(loanId).stream()
            .map(this::toResponse)
            .toList();
    }

    private void verifyBvn(BorrowerProfile profile, Loan loan) {
        if (profile.getBvn() == null) {
            throw ApiException.conflict("BVN_REQUIRED",
                "Borrower has no BVN on file — the loan should not have reached this stage");
        }
        var result = bvnVerificationService.verify(profile.getBvn(),
            loan.getBorrower().getFullName());
        boolean before = profile.isBvnVerified();
        profile.setBvnVerified(result.verified());
        profileRepository.save(profile);
        if (before != result.verified()) {
            auditService.record("USER", loan.getBorrower().getId().toString(), "BVN_CHECKED",
                Map.of("bvnVerified", before), Map.of("bvnVerified", result.verified()),
                result.detail());
        }
    }

    private long openLoansExcluding(UUID borrowerId, UUID currentLoanId) {
        return loanRepository.findByBorrowerIdAndStatusIn(borrowerId, OPEN_STATUSES).stream()
            .filter(l -> !l.getId().equals(currentLoanId))
            .count();
    }

    private CreditAssessment saveAssessment(Loan loan, UUID officerId, int score,
                                            ng.lendstack.domain.enums.RiskTier tier,
                                            String breakdown, boolean overridden, String reason) {
        CreditAssessment assessment = assessmentRepository.save(CreditAssessment.builder()
            .loan(loan)
            .score(score)
            .riskTier(tier)
            .breakdown(breakdown)
            .overridden(overridden)
            .overrideReason(reason)
            .assessedBy(userRepository.getReferenceById(officerId))
            .build());
        loan.setCreditScore(score);
        loan.setRiskTier(tier);
        loanRepository.save(loan);
        return assessment;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize score breakdown", e);
        }
    }

    private CreditAssessmentResponse toResponse(CreditAssessment a) {
        com.fasterxml.jackson.databind.JsonNode breakdown = null;
        try {
            if (a.getBreakdown() != null) {
                breakdown = objectMapper.readTree(a.getBreakdown());
            }
        } catch (Exception e) {
            log.warn("Unparseable breakdown on assessment {}", a.getId());
        }
        return new CreditAssessmentResponse(
            a.getId().toString(), a.getScore(), a.getRiskTier(), breakdown,
            a.isOverridden(), a.getOverrideReason(),
            a.getAssessedBy() == null ? null : a.getAssessedBy().getEmail(),
            a.getCreatedAt());
    }

    private Loan loan(UUID loanId) {
        return loanRepository.findById(loanId)
            .orElseThrow(() -> ApiException.notFound("Loan not found"));
    }
}
