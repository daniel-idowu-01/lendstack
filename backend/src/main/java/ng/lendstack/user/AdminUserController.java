package ng.lendstack.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.common.api.PageResponse;
import ng.lendstack.domain.enums.Role;
import ng.lendstack.loan.dto.ReasonRequest;
import ng.lendstack.user.dto.CreateStaffRequest;
import ng.lendstack.user.dto.UserResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — Users",
    description = "Account administration: provision loan officers (and admins), list accounts, "
        + "deactivate/reactivate. Deactivated users keep their history but cannot sign in.")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Create a staff account",
        description = "Borrowers self-register; LOAN_OFFICER and ADMIN accounts are created here.")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(adminUserService.createStaff(request), "Account created"));
    }

    @Operation(summary = "List users", description = "Optionally filtered by role.")
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminUserService.list(role,
            PageRequest.of(page, Math.min(size, 100), Sort.by("email"))));
    }

    @Operation(summary = "Deactivate a user", description = "Requires a written reason (audited).")
    @PostMapping("/{userId}/deactivate")
    public ApiResponse<UserResponse> deactivate(@PathVariable UUID userId,
                                                @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.ok(adminUserService.setActive(userId, false, request.reason()),
            "Account deactivated");
    }

    @Operation(summary = "Reactivate a user")
    @PostMapping("/{userId}/activate")
    public ApiResponse<UserResponse> activate(@PathVariable UUID userId) {
        return ApiResponse.ok(adminUserService.setActive(userId, true, null), "Account reactivated");
    }
}
