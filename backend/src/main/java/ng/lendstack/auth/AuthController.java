package ng.lendstack.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ng.lendstack.auth.dto.AuthResponse;
import ng.lendstack.auth.dto.LoginRequest;
import ng.lendstack.auth.dto.RegisterRequest;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Registration and login. Self-registration creates a BORROWER "
    + "account; LOAN_OFFICER and ADMIN accounts are provisioned by an ADMIN.")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register as a borrower",
        description = "Creates a BORROWER account with an empty KYC profile. The borrower must "
            + "complete KYC (including BVN) before a loan application can leave SUBMITTED — "
            + "BVN linkage is mandatory under CBN customer due-diligence rules.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(authService.register(request), "Account created"));
    }

    @Operation(summary = "Login",
        description = "Returns a JWT access token. The frontend routes the user to the portal "
            + "matching their role (BORROWER / LOAN_OFFICER / ADMIN).")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "Current session", description = "Returns the authenticated user's identity claims.")
    @GetMapping("/me")
    public ApiResponse<AuthResponse.UserDto> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(new AuthResponse.UserDto(
            principal.getId().toString(), principal.getEmail(),
            principal.getFullName(), principal.getRole()));
    }
}
