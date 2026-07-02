package ng.lendstack.repayment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.repayment.dto.InstallmentResponse;
import ng.lendstack.repayment.dto.PaymentInitResponse;
import ng.lendstack.repayment.dto.ScheduleResponse;
import ng.lendstack.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Borrower — Repayments",
    description = "Monthly repayment via Paystack checkout (card, bank transfer, USSD). "
        + "Initialize a payment for an installment, redirect to the returned authorization_url, "
        + "and Paystack's webhook marks it PAID — the verify endpoint is the fallback used by "
        + "the redirect page. Amounts include any accrued late-payment penalty.")
@RestController
@RequestMapping("/api/v1/borrower")
@RequiredArgsConstructor
public class BorrowerRepaymentController {

    private final RepaymentService repaymentService;

    @Operation(summary = "Repayment schedule + outstanding balance",
        description = "Full amortization schedule (reducing balance), what's outstanding, and "
            + "the next due date — the borrower's repayment dashboard in one call.")
    @GetMapping("/loans/{loanId}/schedule")
    public ApiResponse<ScheduleResponse> schedule(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable UUID loanId) {
        return ApiResponse.ok(repaymentService.schedule(principal.getId(), loanId));
    }

    @Operation(summary = "Pay an installment",
        description = "Creates a Paystack transaction for the amount still due (installment + "
            + "penalty − already paid) and returns the checkout URL to redirect to.")
    @PostMapping("/installments/{installmentId}/pay")
    public ApiResponse<PaymentInitResponse> pay(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable UUID installmentId) {
        return ApiResponse.ok(repaymentService.initializePayment(principal.getId(), installmentId),
            "Redirect to Paystack to complete payment");
    }

    @Operation(summary = "Verify a payment after redirect",
        description = "Called by the frontend callback page with the Paystack reference. "
            + "Confirms the charge with Paystack server-side and applies it if the webhook "
            + "hasn't already (safe to call repeatedly).")
    @GetMapping("/payments/{reference}/verify")
    public ApiResponse<InstallmentResponse> verify(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable String reference) {
        return ApiResponse.ok(repaymentService.verifyAndApply(principal.getId(), reference));
    }
}
