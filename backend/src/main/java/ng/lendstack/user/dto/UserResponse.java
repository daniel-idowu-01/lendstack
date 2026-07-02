package ng.lendstack.user.dto;

import java.time.Instant;
import ng.lendstack.domain.User;
import ng.lendstack.domain.enums.Role;

public record UserResponse(
    String id,
    String email,
    String fullName,
    String phone,
    Role role,
    boolean active,
    Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId().toString(), user.getEmail(), user.getFullName(),
            user.getPhone(), user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
