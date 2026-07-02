package ng.lendstack.auth.dto;

import ng.lendstack.domain.enums.Role;

public record AuthResponse(
    String accessToken,
    String tokenType,
    UserDto user
) {
    public record UserDto(String id, String email, String fullName, Role role) {
    }
}
