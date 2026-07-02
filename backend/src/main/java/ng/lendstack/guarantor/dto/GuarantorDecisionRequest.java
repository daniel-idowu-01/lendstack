package ng.lendstack.guarantor.dto;

import jakarta.validation.constraints.Size;

public record GuarantorDecisionRequest(
    boolean accept,
    @Size(max = 500) String declineReason
) {
}
