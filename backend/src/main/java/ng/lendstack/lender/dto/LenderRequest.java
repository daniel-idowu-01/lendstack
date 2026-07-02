package ng.lendstack.lender.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import ng.lendstack.domain.enums.LenderType;
import ng.lendstack.domain.enums.RiskTier;

public record LenderRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull LenderType type,
    @NotBlank @Email String email,
    @NotNull @DecimalMin(value = "0", inclusive = false,
        message = "Maximum exposure must be positive") BigDecimal maxExposure,
    @NotNull RiskTier preferredRiskTier
) {
}
