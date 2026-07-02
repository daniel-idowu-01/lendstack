package ng.lendstack.borrower;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ng.lendstack.borrower.dto.ProfileRequest;
import ng.lendstack.borrower.dto.ProfileResponse;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Borrower — KYC profile",
    description = "Onboarding/KYC: BVN (mandatory before any loan can progress — CBN customer "
        + "due diligence), employment and income details, and the bank account for disbursement. "
        + "BVN, NIN and account numbers are AES-encrypted at rest and only ever returned masked.")
@RestController
@RequestMapping("/api/v1/borrower/profile")
@RequiredArgsConstructor
public class BorrowerProfileController {

    private final BorrowerProfileService profileService;

    @Operation(summary = "My KYC profile", description = "PII fields come back masked to the last 4 digits.")
    @GetMapping
    public ApiResponse<ProfileResponse> get(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(profileService.get(principal.getId()));
    }

    @Operation(summary = "Update my KYC profile",
        description = "Submitting a new BVN resets its verified flag; verification is re-run "
            + "during the credit check (stubbed NIBSS lookup, to be replaced with the real "
            + "integration). Every update is audit-logged with masked snapshots.")
    @PutMapping
    public ApiResponse<ProfileResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok(profileService.update(principal.getId(), request), "Profile updated");
    }
}
