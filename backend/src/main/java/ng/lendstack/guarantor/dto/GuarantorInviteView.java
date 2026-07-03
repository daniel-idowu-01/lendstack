package ng.lendstack.guarantor.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ng.lendstack.domain.enums.GuarantorStatus;


public record GuarantorInviteView(
    String guarantorName,
    String borrowerName,
    BigDecimal loanAmount,
    int tenureMonths,
    String purpose,
    GuarantorStatus status,
    Instant expiresAt
) {
}
