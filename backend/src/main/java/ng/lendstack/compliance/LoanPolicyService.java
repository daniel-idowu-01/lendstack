package ng.lendstack.compliance;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.config.ConfigKeys;
import ng.lendstack.config.SystemConfigService;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class LoanPolicyService {

    private final SystemConfigService config;


    public void validateApplication(BigDecimal amount, int tenureMonths) {
        int minTenure = config.getInt(ConfigKeys.LOAN_TENURE_MIN_MONTHS);
        int maxTenure = config.getInt(ConfigKeys.LOAN_TENURE_MAX_MONTHS);
        if (tenureMonths < minTenure || tenureMonths > maxTenure) {
            throw ApiException.badRequest("TENURE_OUT_OF_RANGE",
                "Loan tenure must be between %d and %d months for personal loans (CBN guidelines)"
                    .formatted(minTenure, maxTenure));
        }
        BigDecimal minAmount = config.getDecimal(ConfigKeys.LOAN_AMOUNT_MIN_NGN);
        BigDecimal maxAmount = config.getDecimal(ConfigKeys.LOAN_AMOUNT_MAX_NGN);
        if (amount.compareTo(minAmount) < 0 || amount.compareTo(maxAmount) > 0) {
            throw ApiException.badRequest("AMOUNT_OUT_OF_RANGE",
                "Loan amount must be between ₦%,.2f and ₦%,.2f".formatted(minAmount, maxAmount));
        }
    }


    public void validateInterestRate(BigDecimal annualRate) {
        BigDecimal cap = config.getDecimal(ConfigKeys.INTEREST_RATE_CAP_ANNUAL);
        if (annualRate.compareTo(BigDecimal.ZERO) <= 0 || annualRate.compareTo(cap) > 0) {
            throw ApiException.badRequest("RATE_ABOVE_CBN_CAP",
                "Interest rate must be above 0%% and at most %s%% per annum (CBN cap)".formatted(cap));
        }
    }


    public int guarantorsRequiredFor(BigDecimal amount) {
        if (amount.compareTo(config.getDecimal(ConfigKeys.GUARANTOR_TIER1_MAX_NGN)) <= 0) {
            return 0;
        }
        if (amount.compareTo(config.getDecimal(ConfigKeys.GUARANTOR_TIER2_MAX_NGN)) <= 0) {
            return 1;
        }
        return 2;
    }

    public boolean collateralRequiredFor(BigDecimal amount) {
        return amount.compareTo(config.getDecimal(ConfigKeys.COLLATERAL_THRESHOLD_NGN)) >= 0;
    }

    public BigDecimal defaultInterestRate() {
        return config.getDecimal(ConfigKeys.INTEREST_RATE_DEFAULT_ANNUAL);
    }

    public int guarantorExpiryHours() {
        return config.getInt(ConfigKeys.GUARANTOR_EXPIRY_HOURS);
    }

    public int gracePeriodDays() {
        return config.getInt(ConfigKeys.PENALTY_GRACE_PERIOD_DAYS);
    }

    public BigDecimal penaltyDailyRatePercent() {
        return config.getDecimal(ConfigKeys.PENALTY_RATE_DAILY_PERCENT);
    }

    public int daysToDefault() {
        return config.getInt(ConfigKeys.DELINQUENCY_DAYS_TO_DEFAULT);
    }
}
