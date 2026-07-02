package ng.lendstack.collateral;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.collateral.dto.CollateralRequest;
import ng.lendstack.collateral.dto.CollateralResponse;
import ng.lendstack.common.api.ApiResponse;
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

@Tag(name = "Borrower — Collateral",
    description = "Loans at or above the configurable threshold (default ₦2,000,000) require "
        + "collateral: property, vehicle, fixed deposit or equipment. Declared collateral must "
        + "be VERIFIED by a loan officer before the loan can be approved and disbursed.")
@RestController
@RequestMapping("/api/v1/borrower/loans/{loanId}/collaterals")
@RequiredArgsConstructor
public class BorrowerCollateralController {

    private final CollateralService collateralService;

    @Operation(summary = "Declare collateral",
        description = "Attach supporting documents afterwards via the documents endpoint, "
            + "referencing the returned collateral id.")
    @PostMapping
    public ResponseEntity<ApiResponse<CollateralResponse>> declare(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID loanId,
            @Valid @RequestBody CollateralRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
            collateralService.declare(principal.getId(), loanId, request), "Collateral declared"));
    }

    @Operation(summary = "List this loan's collateral records")
    @GetMapping
    public ApiResponse<List<CollateralResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID loanId) {
        return ApiResponse.ok(collateralService.forOwnLoan(principal.getId(), loanId));
    }
}
