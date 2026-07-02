package ng.lendstack.collateral.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ng.lendstack.domain.Collateral;
import ng.lendstack.domain.enums.CollateralType;
import ng.lendstack.domain.enums.VerificationStatus;

public record CollateralResponse(
    String id,
    CollateralType type,
    String description,
    BigDecimal estimatedValue,
    VerificationStatus verificationStatus,
    String verifiedBy,
    Instant verifiedAt,
    String rejectionReason
) {

    public static CollateralResponse from(Collateral c) {
        return new CollateralResponse(c.getId().toString(), c.getType(), c.getDescription(),
            c.getEstimatedValue(), c.getVerificationStatus(),
            c.getVerifiedBy() == null ? null : c.getVerifiedBy().getEmail(),
            c.getVerifiedAt(), c.getRejectionReason());
    }
}
