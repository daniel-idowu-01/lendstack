package ng.lendstack.loan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.common.api.PageResponse;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.domain.enums.RiskTier;
import ng.lendstack.loan.dto.ApproveRequest;
import ng.lendstack.loan.dto.LoanResponse;
import ng.lendstack.loan.dto.OfficerLoanDetailResponse;
import ng.lendstack.loan.dto.ReasonRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Officer — Application review",
    description = "The loan officer's work queue. Officers pick up SUBMITTED applications, "
        + "run the credit check, manage guarantor/collateral verification, and approve or reject.")
@RestController
@RequestMapping("/api/v1/officer/loans")
@RequiredArgsConstructor
public class OfficerLoanController {

    private final OfficerLoanService officerLoanService;
    private final LoanDecisionService loanDecisionService;

    @Operation(summary = "Application queue",
        description = "All non-draft applications, filterable by state, risk tier and amount "
            + "range. Sorted oldest-submitted first so nothing sits unattended.")
    @GetMapping
    public ApiResponse<PageResponse<LoanResponse>> queue(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) RiskTier riskTier,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(officerLoanService.queue(status, riskTier, minAmount, maxAmount,
            PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "createdAt"))));
    }

    @Operation(summary = "Full application detail",
        description = "Application, borrower KYC summary (BVN masked — NDPC), and the complete "
            + "audited timeline including which officer performed each action.")
    @GetMapping("/{loanId}")
    public ApiResponse<OfficerLoanDetailResponse> detail(@PathVariable UUID loanId) {
        return ApiResponse.ok(officerLoanService.detail(loanId));
    }

    @Operation(summary = "Start review",
        description = "SUBMITTED → UNDER_REVIEW. Blocked with BVN_REQUIRED if the borrower has "
            + "no BVN on file: no loan may progress past SUBMITTED without BVN linkage (CBN).")
    @PostMapping("/{loanId}/start-review")
    public ApiResponse<LoanResponse> startReview(@PathVariable UUID loanId) {
        return ApiResponse.ok(officerLoanService.startReview(loanId), "Application is now under review");
    }

    @Operation(summary = "Reject an application",
        description = "Allowed from UNDER_REVIEW or CREDIT_CHECK. The mandatory reason is stored "
            + "on the loan, audit-logged, and shown to the borrower.")
    @PostMapping("/{loanId}/reject")
    public ApiResponse<LoanResponse> reject(@PathVariable UUID loanId,
                                            @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.ok(officerLoanService.reject(loanId, request.reason()),
            "Application rejected");
    }

    @Operation(summary = "Write off a defaulted loan",
        description = "DEFAULTED → WRITTEN_OFF (terminal). Recognizes the debt as unrecoverable; "
            + "requires a written reason. Recovery-after-write-off is an ADMIN override case.")
    @PostMapping("/{loanId}/write-off")
    public ApiResponse<LoanResponse> writeOff(@PathVariable UUID loanId,
                                              @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.ok(officerLoanService.writeOff(loanId, request.reason()),
            "Loan written off");
    }

    @Operation(summary = "Proceed to guarantor stage",
        description = "CREDIT_CHECK → PENDING_GUARANTOR. Blocked while the risk tier is DECLINED "
            + "(reject, or override the score first). Guarantor invitation emails (stub) go out "
            + "with the configurable expiry clock (default 72h); loans needing no guarantors "
            + "pass straight through to PENDING_COLLATERAL with an audit-logged transition.")
    @PostMapping("/{loanId}/proceed")
    public ApiResponse<LoanResponse> proceed(@PathVariable UUID loanId) {
        return ApiResponse.ok(loanDecisionService.proceedToGuarantors(loanId),
            "Loan moved to the guarantor stage");
    }

    @Operation(summary = "Approve the loan",
        description = "PENDING_COLLATERAL → APPROVED. Requires: all guarantors accepted, "
            + "collateral VERIFIED (when required), and rate within the CBN cap (omit the rate "
            + "to use the configured default). Approval commits lender funding atomically — if "
            + "eligible lenders cannot cover the full amount, nothing changes and "
            + "INSUFFICIENT_LENDER_FUNDING is returned.")
    @PostMapping("/{loanId}/approve")
    public ApiResponse<LoanResponse> approve(@PathVariable UUID loanId,
                                             @Valid @RequestBody(required = false) ApproveRequest request) {
        return ApiResponse.ok(loanDecisionService.approve(loanId,
                request == null ? null : request.interestRateAnnual()),
            "Loan approved and matched to lenders");
    }
}
