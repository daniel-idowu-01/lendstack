-- Default runtime configuration, seeded from environment variables via Flyway
-- placeholders (see application.yml). After this first seed, ADMIN edits these
-- rows through /api/v1/admin/config — never redeploy to change a CBN parameter.

INSERT INTO system_config (config_key, config_value, value_type, description, updated_by)
VALUES
    ('interest.rate.cap.annual', '${interestRateCapAnnual}', 'NUMBER',
     'CBN consumer-lending cap on annual interest rate (%). Loans cannot be approved above this.', 'SYSTEM'),
    ('interest.rate.default.annual', '${defaultInterestRateAnnual}', 'NUMBER',
     'Default annual interest rate (%) proposed at approval; officer may adjust up to the cap.', 'SYSTEM'),
    ('loan.tenure.min.months', '${minTenureMonths}', 'NUMBER',
     'Minimum personal-loan tenure in months (CBN microfinance guidelines).', 'SYSTEM'),
    ('loan.tenure.max.months', '${maxTenureMonths}', 'NUMBER',
     'Maximum personal-loan tenure in months (CBN microfinance guidelines: 24).', 'SYSTEM'),
    ('loan.amount.min.ngn', '${minLoanAmount}', 'NUMBER',
     'Minimum loan principal in Naira.', 'SYSTEM'),
    ('loan.amount.max.ngn', '${maxLoanAmount}', 'NUMBER',
     'Maximum loan principal in Naira.', 'SYSTEM'),
    ('penalty.rate.daily.percent', '${penaltyRateDailyPercent}', 'NUMBER',
     'Late-payment penalty per day (%) applied to the overdue installment total after the grace period.', 'SYSTEM'),
    ('penalty.grace.period.days', '${gracePeriodDays}', 'NUMBER',
     'Days after the due date before penalties start accruing.', 'SYSTEM'),
    ('collateral.threshold.ngn', '${collateralThreshold}', 'NUMBER',
     'Loans at or above this principal require VERIFIED collateral before disbursement.', 'SYSTEM'),
    ('guarantor.tier1.max.ngn', '${guarantorTier1Max}', 'NUMBER',
     'Loans up to this amount need 0 guarantors.', 'SYSTEM'),
    ('guarantor.tier2.max.ngn', '${guarantorTier2Max}', 'NUMBER',
     'Loans up to this amount need 1 guarantor; above it, 2.', 'SYSTEM'),
    ('guarantor.expiry.hours', '${guarantorExpiryHours}', 'NUMBER',
     'Hours a guarantor has to accept/decline before the request expires and the loan returns to UNDER_REVIEW.', 'SYSTEM'),
    ('delinquency.days.to.default', '${daysToDefault}', 'NUMBER',
     'Days an installment stays overdue before the loan moves DELINQUENT → DEFAULTED.', 'SYSTEM');
