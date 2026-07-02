package ng.lendstack.guarantor.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ng.lendstack.domain.enums.GuarantorStatus;

/**
 * What a guarantor sees when they open their accept/decline link — enough to
 * make an informed decision, without exposing the borrower's PII.
 */
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
