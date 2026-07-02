package ng.lendstack.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfigUpdateRequest(
    @NotBlank @Size(max = 255) String value
) {
}
