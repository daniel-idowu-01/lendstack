package ng.lendstack.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 100,
        message = "Password must be at least 8 characters") String password,
    @NotBlank @Size(max = 255) String fullName,
    @Pattern(regexp = "^(\\+234|0)[789][01]\\d{8}$",
        message = "Enter a valid Nigerian phone number, e.g. 08012345678") String phone
) {
}
