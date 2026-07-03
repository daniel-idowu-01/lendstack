package ng.lendstack.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ng.lendstack.domain.enums.Role;


public record CreateStaffRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(max = 255) String fullName,
    String phone,
    @NotNull Role role
) {
}
