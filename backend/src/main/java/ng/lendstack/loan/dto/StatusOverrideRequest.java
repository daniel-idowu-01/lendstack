package ng.lendstack.loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ng.lendstack.domain.enums.LoanStatus;


public record StatusOverrideRequest(
    @NotNull LoanStatus targetStatus,
    @NotBlank @Size(min = 10, max = 1000,
        message = "Provide a meaningful reason (at least 10 characters) — this is audit-logged")
    String reason
) {
}
