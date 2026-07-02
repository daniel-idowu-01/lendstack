package ng.lendstack.collateral.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ng.lendstack.domain.enums.VerificationStatus;

/** Officer verdict on a collateral record. Rejection requires a reason. */
public record CollateralVerifyRequest(
    @NotNull VerificationStatus status,
    @Size(max = 500) String reason
) {
}
