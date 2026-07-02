package ng.lendstack.credit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import ng.lendstack.compliance.LoanPolicyService;
import ng.lendstack.domain.BorrowerProfile;
import ng.lendstack.domain.Guarantor;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.EmploymentStatus;
import ng.lendstack.domain.enums.RiskTier;
import org.springframework.stereotype.Service;

/**
 * Rule-based credit scoring (deliberately not ML — every point is explainable
 * to the loan officer and to the borrower on request). Six rules, 100 points:
 *
 *   employment stability      20
 *   repayment burden          25   (est. monthly installment vs monthly income)
 *   existing obligations      15
 *   repayment history         20
 *   BVN verification          10
 *   guarantor strength        10
 *
 * Tiers: 75+ LOW · 55–74 MEDIUM · 35–54 HIGH · below 35 DECLINED.
 * Officers may override with a mandatory written reason (audit-logged).
 */
@Service
@RequiredArgsConstructor
public class CreditScoringService {

    private static final int TIER_LOW_MIN = 75;
    private static final int TIER_MEDIUM_MIN = 55;
    private static final int TIER_HIGH_MIN = 35;

    private final LoanPolicyService policyService;

    public record RuleResult(String rule, int points, int maxPoints, String detail) {
    }

    public record ScoreCard(int score, RiskTier riskTier, List<RuleResult> breakdown) {
    }

    /** History facts about the borrower, assembled by the caller. */
    public record BorrowerHistory(long openLoans, long closedLoans, boolean everDefaulted) {
    }

    public ScoreCard assess(Loan loan, BorrowerProfile profile, List<Guarantor> guarantors,
                            BorrowerHistory history) {
        BigDecimal estimatedInstallment = ng.lendstack.finance.Amortization.monthlyPayment(
            loan.getAmount(), policyService.defaultInterestRate(), loan.getTenureMonths());

        List<RuleResult> rules = new ArrayList<>();
        rules.add(employment(profile));
        rules.add(repaymentBurden(profile, estimatedInstallment));
        rules.add(existingObligations(history));
        rules.add(repaymentHistory(history));
        rules.add(bvnVerification(profile));
        rules.add(guarantorStrength(loan, guarantors, estimatedInstallment));

        int score = rules.stream().mapToInt(RuleResult::points).sum();
        return new ScoreCard(score, tierFor(score), rules);
    }

    private RiskTier tierFor(int score) {
        if (score >= TIER_LOW_MIN) {
            return RiskTier.LOW;
        }
        if (score >= TIER_MEDIUM_MIN) {
            return RiskTier.MEDIUM;
        }
        if (score >= TIER_HIGH_MIN) {
            return RiskTier.HIGH;
        }
        return RiskTier.DECLINED;
    }

    private RuleResult employment(BorrowerProfile profile) {
        EmploymentStatus status = profile.getEmploymentStatus();
        int points = status == null ? 0 : switch (status) {
            case EMPLOYED -> 20;
            case SELF_EMPLOYED, BUSINESS_OWNER -> 16;
            case RETIRED -> 10;
            case STUDENT -> 4;
            case UNEMPLOYED -> 0;
        };
        return new RuleResult("EMPLOYMENT", points, 20,
            status == null ? "No employment information provided" : "Status: " + status);
    }

    private RuleResult repaymentBurden(BorrowerProfile profile, BigDecimal installment) {
        BigDecimal income = profile.getMonthlyIncome();
        if (income == null || income.signum() <= 0) {
            return new RuleResult("REPAYMENT_BURDEN", 0, 25, "No verifiable monthly income");
        }
        BigDecimal ratio = installment.divide(income, 4, RoundingMode.HALF_UP);
        int points;
        if (ratio.compareTo(new BigDecimal("0.20")) <= 0) {
            points = 25;
        } else if (ratio.compareTo(new BigDecimal("0.33")) <= 0) {
            points = 18;
        } else if (ratio.compareTo(new BigDecimal("0.50")) <= 0) {
            points = 10;
        } else {
            points = 0;
        }
        return new RuleResult("REPAYMENT_BURDEN", points, 25,
            "Estimated installment is %s%% of monthly income"
                .formatted(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)));
    }

    private RuleResult existingObligations(BorrowerHistory history) {
        int points = history.openLoans() == 0 ? 15 : history.openLoans() == 1 ? 8 : 0;
        return new RuleResult("EXISTING_OBLIGATIONS", points, 15,
            history.openLoans() + " open loan(s) on this platform");
    }

    private RuleResult repaymentHistory(BorrowerHistory history) {
        if (history.everDefaulted()) {
            return new RuleResult("REPAYMENT_HISTORY", 0, 20,
                "Borrower has a defaulted or written-off loan on record");
        }
        if (history.closedLoans() > 0) {
            return new RuleResult("REPAYMENT_HISTORY", 20, 20,
                history.closedLoans() + " loan(s) fully repaid — strong returning borrower");
        }
        return new RuleResult("REPAYMENT_HISTORY", 12, 20,
            "First-time borrower — neutral history score");
    }

    private RuleResult bvnVerification(BorrowerProfile profile) {
        return profile.isBvnVerified()
            ? new RuleResult("BVN_VERIFICATION", 10, 10, "BVN verified against NIBSS (stub)")
            : new RuleResult("BVN_VERIFICATION", 0, 10, "BVN not verified");
    }

    private RuleResult guarantorStrength(Loan loan, List<Guarantor> guarantors,
                                         BigDecimal installment) {
        if (loan.getGuarantorsRequired() == 0) {
            return new RuleResult("GUARANTOR_STRENGTH", 10, 10,
                "No guarantor required at this loan amount");
        }
        if (guarantors.isEmpty()) {
            return new RuleResult("GUARANTOR_STRENGTH", 0, 10,
                "Guarantor(s) required but none provided yet");
        }
        boolean strong = guarantors.stream().anyMatch(g -> g.getMonthlyIncome() != null
            && g.getMonthlyIncome().compareTo(installment.multiply(BigDecimal.valueOf(3))) >= 0);
        return strong
            ? new RuleResult("GUARANTOR_STRENGTH", 10, 10,
                "At least one guarantor earns 3× the estimated installment")
            : new RuleResult("GUARANTOR_STRENGTH", 6, 10,
                "Guarantor(s) provided; declared income below the 3× installment bar");
    }
}
