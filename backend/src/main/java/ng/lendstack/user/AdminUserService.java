package ng.lendstack.user;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.audit.AuditService;
import ng.lendstack.common.api.PageResponse;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.domain.BorrowerProfile;
import ng.lendstack.domain.User;
import ng.lendstack.domain.enums.Role;
import ng.lendstack.repository.BorrowerProfileRepository;
import ng.lendstack.repository.UserRepository;
import ng.lendstack.security.RequestContext;
import ng.lendstack.user.dto.CreateStaffRequest;
import ng.lendstack.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final BorrowerProfileRepository borrowerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final RequestContext requestContext;

    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        User user = userRepository.save(User.builder()
            .email(request.email().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .phone(request.phone())
            .role(request.role())
            .build());
        if (request.role() == Role.BORROWER) {
            borrowerProfileRepository.save(BorrowerProfile.builder().user(user).build());
        }
        auditService.record("USER", user.getId().toString(), "USER_CREATED",
            null, Map.of("email", user.getEmail(), "role", user.getRole().name()), null);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Role role, Pageable pageable) {
        var page = role == null ? userRepository.findAll(pageable)
            : userRepository.findByRole(role, pageable);
        return PageResponse.from(page, UserResponse::from);
    }

    @Transactional
    public UserResponse setActive(UUID userId, boolean active, String reason) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ApiException.notFound("User not found"));
        if (user.getEmail().equalsIgnoreCase(requestContext.actor()) && !active) {
            throw ApiException.badRequest("SELF_DEACTIVATION", "You cannot deactivate your own account");
        }
        boolean before = user.isActive();
        user.setActive(active);
        userRepository.save(user);
        auditService.record("USER", user.getId().toString(),
            active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
            Map.of("active", before), Map.of("active", active), reason);
        return UserResponse.from(user);
    }
}
