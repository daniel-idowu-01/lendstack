package ng.lendstack.collateral;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.collateral.dto.CollateralResponse;
import ng.lendstack.collateral.dto.CollateralVerifyRequest;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Officer — Collateral verification",
    description = "Officers inspect declared collateral (with its uploaded documents) and mark "
        + "it VERIFIED or REJECTED. Disbursement is blocked until required collateral is VERIFIED.")
@RestController
@RequestMapping("/api/v1/officer")
@RequiredArgsConstructor
public class OfficerCollateralController {

    private final CollateralService collateralService;

    @Operation(summary = "Collateral records for a loan")
    @GetMapping("/loans/{loanId}/collaterals")
    public ApiResponse<List<CollateralResponse>> forLoan(@PathVariable UUID loanId) {
        return ApiResponse.ok(collateralService.forLoan(loanId));
    }

    @Operation(summary = "Verify or reject collateral",
        description = "Rejection requires a written reason. Both verdicts are audit-logged with "
            + "the verifying officer's identity.")
    @PostMapping("/collaterals/{collateralId}/verify")
    public ApiResponse<CollateralResponse> verify(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable UUID collateralId,
                                                  @Valid @RequestBody CollateralVerifyRequest request) {
        return ApiResponse.ok(collateralService.verify(principal.getId(), collateralId, request),
            "Collateral " + request.status());
    }
}
