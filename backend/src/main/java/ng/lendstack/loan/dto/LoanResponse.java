package ng.lendstack.loan.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.domain.enums.RiskTier;

public record LoanResponse(
    String id,
    String reference,
    String borrowerName,
    BigDecimal amount,
    String purpose,
    int tenureMonths,
    BigDecimal interestRateAnnual,
    LoanStatus status,
    RiskTier riskTier,
    Integer creditScore,
    int guarantorsRequired,
    boolean collateralRequired,
    BigDecimal outstandingPrincipal,
    Instant submittedAt,
    Instant approvedAt,
    Instant disbursedAt,
    Instant closedAt,
    String rejectionReason,
    Instant createdAt
) {
}
