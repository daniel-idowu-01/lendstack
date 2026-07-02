package ng.lendstack.lender;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.common.api.PageResponse;
import ng.lendstack.lender.dto.FundingResponse;
import ng.lendstack.lender.dto.LenderRequest;
import ng.lendstack.lender.dto.LenderResponse;
import ng.lendstack.lender.dto.LenderUpdateRequest;
import ng.lendstack.lender.dto.WalletTopupRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — Lenders",
    description = "Funding sources (individuals or institutions). Each lender has a wallet, a "
        + "maximum exposure limit and a risk appetite; the matching engine splits approved loans "
        + "across eligible lenders and tracks each portion separately for repayment distribution.")
@RestController
@RequestMapping("/api/v1/admin/lenders")
@RequiredArgsConstructor
public class AdminLenderController {

    private final LenderService lenderService;

    @Operation(summary = "Register a lender")
    @PostMapping
    public ResponseEntity<ApiResponse<LenderResponse>> register(
            @Valid @RequestBody LenderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(lenderService.register(request), "Lender registered"));
    }

    @Operation(summary = "List lenders")
    @GetMapping
    public ApiResponse<PageResponse<LenderResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(lenderService.list(
            PageRequest.of(page, Math.min(size, 100), Sort.by("name"))));
    }

    @Operation(summary = "Update exposure limit / risk appetite / active flag")
    @PutMapping("/{lenderId}")
    public ApiResponse<LenderResponse> update(@PathVariable UUID lenderId,
                                              @Valid @RequestBody LenderUpdateRequest request) {
        return ApiResponse.ok(lenderService.update(lenderId, request), "Lender updated");
    }

    @Operation(summary = "Top up a lender's wallet",
        description = "STUB settlement: credits the wallet directly and audit-logs it. In "
            + "production this would be driven by reconciled bank inflows, not an API call.")
    @PostMapping("/{lenderId}/wallet-topup")
    public ApiResponse<LenderResponse> topUp(@PathVariable UUID lenderId,
                                             @Valid @RequestBody WalletTopupRequest request) {
        return ApiResponse.ok(lenderService.topUpWallet(lenderId, request.amount()), "Wallet credited");
    }

    @Operation(summary = "Lender portfolio",
        description = "Every loan slice this lender has funded, with principal repaid and "
            + "interest earned so far.")
    @GetMapping("/{lenderId}/portfolio")
    public ApiResponse<PageResponse<FundingResponse>> portfolio(
            @PathVariable UUID lenderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(lenderService.portfolio(lenderId,
            PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}
