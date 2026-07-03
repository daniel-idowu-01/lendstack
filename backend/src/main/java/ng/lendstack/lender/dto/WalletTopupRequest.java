package ng.lendstack.lender.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;


public record WalletTopupRequest(
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount
) {
}
