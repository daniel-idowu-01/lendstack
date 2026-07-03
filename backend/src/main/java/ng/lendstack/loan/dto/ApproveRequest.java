package ng.lendstack.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;


public record ApproveRequest(
    @DecimalMin(value = "0", inclusive = false) @Digits(integer = 3, fraction = 2)
    BigDecimal interestRateAnnual
) {
}
