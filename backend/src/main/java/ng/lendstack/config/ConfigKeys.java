package ng.lendstack.config;

/**
 * Keys in the system_config table (seeded by V2 migration from environment
 * variables, editable afterwards by ADMIN through the API).
 */
public final class ConfigKeys {

    public static final String INTEREST_RATE_CAP_ANNUAL = "interest.rate.cap.annual";
    public static final String INTEREST_RATE_DEFAULT_ANNUAL = "interest.rate.default.annual";
    public static final String LOAN_TENURE_MIN_MONTHS = "loan.tenure.min.months";
    public static final String LOAN_TENURE_MAX_MONTHS = "loan.tenure.max.months";
    public static final String LOAN_AMOUNT_MIN_NGN = "loan.amount.min.ngn";
    public static final String LOAN_AMOUNT_MAX_NGN = "loan.amount.max.ngn";
    public static final String PENALTY_RATE_DAILY_PERCENT = "penalty.rate.daily.percent";
    public static final String PENALTY_GRACE_PERIOD_DAYS = "penalty.grace.period.days";
    public static final String COLLATERAL_THRESHOLD_NGN = "collateral.threshold.ngn";
    public static final String GUARANTOR_TIER1_MAX_NGN = "guarantor.tier1.max.ngn";
    public static final String GUARANTOR_TIER2_MAX_NGN = "guarantor.tier2.max.ngn";
    public static final String GUARANTOR_EXPIRY_HOURS = "guarantor.expiry.hours";
    public static final String DELINQUENCY_DAYS_TO_DEFAULT = "delinquency.days.to.default";

    private ConfigKeys() {
    }
}
