package ng.lendstack.lender.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ng.lendstack.domain.Lender;
import ng.lendstack.domain.enums.LenderType;
import ng.lendstack.domain.enums.RiskTier;

public record LenderResponse(
    String id,
    String name,
    LenderType type,
    String email,
    BigDecimal walletBalance,
    BigDecimal maxExposure,
    BigDecimal currentExposure,
    RiskTier preferredRiskTier,
    boolean active,
    Instant createdAt
) {

    public static LenderResponse from(Lender lender) {
        return new LenderResponse(lender.getId().toString(), lender.getName(), lender.getType(),
            lender.getEmail(), lender.getWalletBalance(), lender.getMaxExposure(),
            lender.getCurrentExposure(), lender.getPreferredRiskTier(), lender.isActive(),
            lender.getCreatedAt());
    }
}
