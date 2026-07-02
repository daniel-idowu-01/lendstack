package ng.lendstack.guarantor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.guarantor.dto.GuarantorRequest;
import ng.lendstack.guarantor.dto.GuarantorResponse;
import ng.lendstack.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Borrower — Guarantors",
    description = "Guarantor requirements scale with the loan amount (configurable tiers: "
        + "0, 1 or 2 guarantors). Each named guarantor gets an email link (stubbed sender) to "
        + "accept or decline within a configurable window (default 72h). The loan cannot be "
        + "approved until all required guarantors have accepted; a decline or expiry returns "
        + "it to UNDER_REVIEW.")
@RestController
@RequestMapping("/api/v1/borrower/loans/{loanId}/guarantors")
@RequiredArgsConstructor
public class BorrowerGuarantorController {

    private final GuarantorService guarantorService;

    @Operation(summary = "Add a guarantor",
        description = "Allowed until the loan passes PENDING_GUARANTOR. If the loan is already "
            + "waiting on guarantors, the invitation (and its expiry clock) starts immediately.")
    @PostMapping
    public ResponseEntity<ApiResponse<GuarantorResponse>> add(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID loanId,
            @Valid @RequestBody GuarantorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
            guarantorService.add(principal.getId(), loanId, request), "Guarantor added"));
    }

    @Operation(summary = "List this loan's guarantors")
    @GetMapping
    public ApiResponse<List<GuarantorResponse>> list(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable UUID loanId) {
        return ApiResponse.ok(guarantorService.forOwnLoan(principal.getId(), loanId));
    }
}
