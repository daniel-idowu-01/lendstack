package ng.lendstack.lender.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import ng.lendstack.domain.enums.RiskTier;

/** Partial update — null fields are left unchanged. */
public record LenderUpdateRequest(
    @DecimalMin(value = "0", inclusive = false) BigDecimal maxExposure,
    RiskTier preferredRiskTier,
    Boolean active
) {
}
