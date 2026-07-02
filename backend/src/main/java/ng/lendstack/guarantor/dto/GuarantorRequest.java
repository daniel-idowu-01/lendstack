package ng.lendstack.guarantor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GuarantorRequest(
    @NotBlank @Size(max = 255) String fullName,
    @NotBlank @Email String email,
    @Size(max = 32) String phone,
    @Size(max = 100) String relationship,
    @Size(max = 100) String occupation,
    @PositiveOrZero BigDecimal monthlyIncome
) {
}
