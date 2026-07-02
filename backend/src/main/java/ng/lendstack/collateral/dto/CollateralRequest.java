package ng.lendstack.collateral.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import ng.lendstack.domain.enums.CollateralType;

public record CollateralRequest(
    @NotNull CollateralType type,
    @NotBlank @Size(max = 1000) String description,
    @NotNull @DecimalMin(value = "1", message = "Estimated value must be positive")
    BigDecimal estimatedValue
) {
}
