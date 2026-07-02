package ng.lendstack.auth;

import lombok.RequiredArgsConstructor;
import ng.lendstack.audit.AuditService;
import ng.lendstack.auth.dto.AuthResponse;
import ng.lendstack.auth.dto.LoginRequest;
import ng.lendstack.auth.dto.RegisterRequest;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.domain.BorrowerProfile;
import ng.lendstack.domain.User;
import ng.lendstack.domain.enums.Role;
import ng.lendstack.repository.BorrowerProfileRepository;
import ng.lendstack.repository.UserRepository;
import ng.lendstack.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BorrowerProfileRepository borrowerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    /** Self-service registration always creates a BORROWER. Staff accounts are created by ADMIN. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        User user = userRepository.save(User.builder()
            .email(request.email().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .phone(request.phone())
            .role(Role.BORROWER)
            .build());
        borrowerProfileRepository.save(BorrowerProfile.builder().user(user).build());

        auditService.record("USER", user.getId().toString(), "REGISTERED",
            null, new AuditView(user.getEmail(), user.getRole().name()), null);

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("bad credentials");
        }
        if (!user.isActive()) {
            throw ApiException.forbidden("This account has been deactivated. Contact support.");
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
            jwtService.issueToken(user),
            "Bearer",
            new AuthResponse.UserDto(user.getId().toString(), user.getEmail(),
                user.getFullName(), user.getRole()));
    }

    record AuditView(String email, String role) {
    }
}
