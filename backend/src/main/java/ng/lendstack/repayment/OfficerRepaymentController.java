package ng.lendstack.repayment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.loan.dto.LoanResponse;
import ng.lendstack.loan.dto.ReasonRequest;
import ng.lendstack.repayment.dto.InstallmentResponse;
import ng.lendstack.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Officer — Disbursement & installments",
    description = "Disbursement releases funds to the borrower (stubbed bank transfer) and "
        + "generates the reducing-balance repayment schedule. Officers may also waive an "
        + "installment with a written reason (a realized lender loss — audit-logged).")
@RestController
@RequestMapping("/api/v1/officer")
@RequiredArgsConstructor
public class OfficerRepaymentController {

    private final DisbursementService disbursementService;
    private final RepaymentService repaymentService;

    @Operation(summary = "Disburse an approved loan",
        description = "APPROVED → DISBURSED → ACTIVE. Blocked without VERIFIED collateral where "
            + "required. Generates the full amortization schedule (first installment due one "
            + "month from today) and notifies the borrower. The bank transfer itself is a "
            + "clearly-flagged stub pending a real payment-rail integration.")
    @PostMapping("/loans/{loanId}/disburse")
    public ApiResponse<LoanResponse> disburse(@PathVariable UUID loanId) {
        return ApiResponse.ok(disbursementService.disburse(loanId),
            "Loan disbursed — repayment schedule generated");
    }

    @Operation(summary = "Waive an installment",
        description = "Marks a PENDING/OVERDUE installment WAIVED with a mandatory reason. "
            + "Reduces the loan's outstanding principal and releases lender exposure without "
            + "repayment (realized loss).")
    @PostMapping("/installments/{installmentId}/waive")
    public ApiResponse<InstallmentResponse> waive(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable UUID installmentId,
                                                  @Valid @RequestBody ReasonRequest request) {
        return ApiResponse.ok(repaymentService.waive(principal.getId(), installmentId, request.reason()),
            "Installment waived");
    }
}
