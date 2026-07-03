package ng.lendstack.lender.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ng.lendstack.domain.LoanFunding;


public record FundingResponse(
    String id,
    String loanId,
    String loanReference,
    String lenderId,
    String lenderName,
    BigDecimal amount,
    BigDecimal principalRepaid,
    BigDecimal interestEarned,
    Instant createdAt
) {

    public static FundingResponse from(LoanFunding f) {
        return new FundingResponse(f.getId().toString(),
            f.getLoan().getId().toString(), f.getLoan().getReference(),
            f.getLender().getId().toString(), f.getLender().getName(),
            f.getAmount(), f.getPrincipalRepaid(), f.getInterestEarned(), f.getCreatedAt());
    }
}
