package ng.lendstack.loan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.common.api.PageResponse;
import ng.lendstack.loan.dto.LoanApplicationRequest;
import ng.lendstack.loan.dto.LoanDetailResponse;
import ng.lendstack.loan.dto.LoanResponse;
import ng.lendstack.security.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Borrower — Loans",
    description = "Loan applications from the borrower's side. A borrower drafts an application, "
        + "submits it (BVN required — CBN due diligence), then tracks it through review, "
        + "guarantor consent, collateral verification, approval and disbursement.")
@RestController
@RequestMapping("/api/v1/borrower/loans")
@RequiredArgsConstructor
public class BorrowerLoanController {

    private final BorrowerLoanService borrowerLoanService;

    @Operation(summary = "Create a draft application",
        description = "Creates a DRAFT loan. Amount and tenure are validated against the "
            + "CBN-configured bounds in system_config (default: ₦50,000–₦10,000,000, 1–24 months). "
            + "Drafts are editable and invisible to loan officers until submitted.")
    @PostMapping
    public ResponseEntity<ApiResponse<LoanResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
            borrowerLoanService.createDraft(principal.getId(), request),
            "Draft application created"));
    }

    @Operation(summary = "Edit a draft application",
        description = "Only DRAFT applications can be edited. Every edit is audit-logged.")
    @PutMapping("/{loanId}")
    public ApiResponse<LoanResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable UUID loanId,
                                            @Valid @RequestBody LoanApplicationRequest request) {
        return ApiResponse.ok(borrowerLoanService.updateDraft(principal.getId(), loanId, request));
    }

    @Operation(summary = "Submit an application",
        description = "DRAFT → SUBMITTED. Fails with BVN_REQUIRED if the borrower's KYC profile "
            + "has no BVN — BVN linkage is mandatory before a loan can progress (CBN). On submit, "
            + "the system fixes the number of guarantors required (by amount tier) and whether "
            + "collateral is required (amount ≥ configurable threshold).")
    @PostMapping("/{loanId}/submit")
    public ApiResponse<LoanResponse> submit(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable UUID loanId) {
        return ApiResponse.ok(borrowerLoanService.submit(principal.getId(), loanId),
            "Application submitted — you will be notified as it moves through review");
    }

    @Operation(summary = "List my loans", description = "Paginated, newest first.")
    @GetMapping
    public ApiResponse<PageResponse<LoanResponse>> myLoans(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(borrowerLoanService.myLoans(principal.getId(),
            PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @Operation(summary = "Loan detail with timeline",
        description = "The application plus a chronological timeline of every state it has "
            + "passed through (from the immutable audit trail). Staff identities are not exposed.")
    @GetMapping("/{loanId}")
    public ApiResponse<LoanDetailResponse> detail(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable UUID loanId) {
        return ApiResponse.ok(borrowerLoanService.myLoanDetail(principal.getId(), loanId));
    }
}
