package ng.lendstack.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

/**
 * Approval terms. If interestRateAnnual is omitted, the configured default
 * rate applies. Whatever the source, the rate is validated against the CBN cap.
 */
public record ApproveRequest(
    @DecimalMin(value = "0", inclusive = false) @Digits(integer = 3, fraction = 2)
    BigDecimal interestRateAnnual
) {
}
