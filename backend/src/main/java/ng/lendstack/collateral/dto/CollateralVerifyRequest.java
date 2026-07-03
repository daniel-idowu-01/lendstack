package ng.lendstack.collateral.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ng.lendstack.domain.enums.VerificationStatus;


public record CollateralVerifyRequest(
    @NotNull VerificationStatus status,
    @Size(max = 500) String reason
) {
}
