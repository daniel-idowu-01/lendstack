package ng.lendstack.loan;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.collateral.CollateralService;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.compliance.LoanPolicyService;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.domain.enums.RiskTier;
import ng.lendstack.guarantor.GuarantorService;
import ng.lendstack.lender.LenderMatchingService;
import ng.lendstack.loan.dto.LoanResponse;
import ng.lendstack.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class LoanDecisionService {

    private final LoanRepository loanRepository;
    private final LoanLifecycleService lifecycleService;
    private final GuarantorService guarantorService;
    private final CollateralService collateralService;
    private final LenderMatchingService lenderMatchingService;
    private final LoanPolicyService policyService;
    private final LoanMapper loanMapper;


    @Transactional
    public LoanResponse proceedToGuarantors(UUID loanId) {
        Loan loan = get(loanId);
        if (loan.getStatus() != LoanStatus.CREDIT_CHECK) {
            throw ApiException.conflict("WRONG_STATE",
                "Only loans in CREDIT_CHECK can proceed to the guarantor stage");
        }
        if (loan.getRiskTier() == null) {
            throw ApiException.conflict("NO_ASSESSMENT", "Run the credit check first");
        }
        if (loan.getRiskTier() == RiskTier.DECLINED) {
            throw ApiException.conflict("SCORE_DECLINED",
                "The credit score puts this application in DECLINED — reject it, or override "
                    + "the score with a written reason first");
        }
        lifecycleService.transition(loan, LoanStatus.PENDING_GUARANTOR,
            loan.getGuarantorsRequired() == 0 ? "No guarantors required at this amount"
                : "Awaiting %d guarantor acceptance(s)".formatted(loan.getGuarantorsRequired()));
        guarantorService.activateRequests(loan);
        guarantorService.maybeAdvance(loan);
        return loanMapper.toResponse(loan);
    }


    @Transactional
    public LoanResponse approve(UUID loanId, BigDecimal requestedRate) {
        Loan loan = get(loanId);
        if (loan.getStatus() != LoanStatus.PENDING_COLLATERAL) {
            throw ApiException.conflict("WRONG_STATE",
                "Only loans in PENDING_COLLATERAL can be approved (current: " + loan.getStatus() + ")");
        }
        if (guarantorService.acceptedCount(loanId) < loan.getGuarantorsRequired()) {
            throw ApiException.conflict("GUARANTORS_OUTSTANDING",
                "Not all required guarantors have accepted");
        }
        if (loan.isCollateralRequired() && !collateralService.hasVerifiedCollateral(loanId)) {
            throw ApiException.conflict("COLLATERAL_UNVERIFIED",
                "This loan requires VERIFIED collateral before approval");
        }
        BigDecimal rate = requestedRate != null ? requestedRate : policyService.defaultInterestRate();
        policyService.validateInterestRate(rate);
        loan.setInterestRateAnnual(rate);
        loanRepository.save(loan);

        lifecycleService.transition(loan, LoanStatus.APPROVED,
            "Approved at %s%% p.a. (reducing balance)".formatted(rate));
        lenderMatchingService.matchAndCommit(loan);
        return loanMapper.toResponse(loan);
    }

    private Loan get(UUID loanId) {
        return loanRepository.findById(loanId)
            .orElseThrow(() -> ApiException.notFound("Loan not found"));
    }
}
