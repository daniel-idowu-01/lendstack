package ng.lendstack.loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A mandatory written reason accompanying a decision — always audit-logged. */
public record ReasonRequest(
    @NotBlank @Size(min = 10, max = 1000,
        message = "Provide a meaningful reason (at least 10 characters) — this is audit-logged")
    String reason
) {
}
