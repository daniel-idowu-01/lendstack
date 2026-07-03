package ng.lendstack.credit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ng.lendstack.domain.enums.RiskTier;


public record ScoreOverrideRequest(
    @Min(0) @Max(100) int score,
    @NotNull RiskTier riskTier,
    @NotBlank @Size(min = 10, max = 1000,
        message = "Provide a meaningful reason (at least 10 characters) — this is audit-logged")
    String reason
) {
}
