package ng.lendstack.credit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.credit.dto.CreditAssessmentResponse;
import ng.lendstack.credit.dto.ScoreOverrideRequest;
import ng.lendstack.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Officer — Credit check",
    description = "Rule-based credit scoring (0–100 → LOW/MEDIUM/HIGH/DECLINED). The engine "
        + "weighs employment, repayment burden (installment vs income), existing obligations, "
        + "repayment history, BVN verification (stubbed NIBSS call) and guarantor strength. "
        + "Every run and every officer override is stored immutably with its full breakdown.")
@RestController
@RequestMapping("/api/v1/officer/loans/{loanId}")
@RequiredArgsConstructor
public class OfficerCreditController {

    private final CreditCheckService creditCheckService;

    @Operation(summary = "Run the credit check",
        description = "Verifies the borrower's BVN (stub — BVNs starting with 0 fail, for demos), "
            + "scores the application and returns the per-rule breakdown. First run moves the "
            + "loan UNDER_REVIEW → CREDIT_CHECK; re-running adds a fresh assessment row.")
    @PostMapping("/credit-check")
    public ApiResponse<CreditAssessmentResponse> run(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable UUID loanId) {
        return ApiResponse.ok(creditCheckService.runCreditCheck(loanId, principal.getId()),
            "Credit check complete");
    }

    @Operation(summary = "Override the credit score",
        description = "Records a new assessment with the officer's score/tier and a mandatory "
            + "written reason. The original rule-based assessment is preserved; the override is "
            + "audit-logged as SCORE_OVERRIDE.")
    @PostMapping("/score-override")
    public ApiResponse<CreditAssessmentResponse> override(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID loanId,
            @Valid @RequestBody ScoreOverrideRequest request) {
        return ApiResponse.ok(creditCheckService.overrideScore(loanId, principal.getId(), request),
            "Score overridden");
    }

    @Operation(summary = "Assessment history",
        description = "All scoring runs and overrides for this loan, newest first.")
    @GetMapping("/assessments")
    public ApiResponse<List<CreditAssessmentResponse>> assessments(@PathVariable UUID loanId) {
        return ApiResponse.ok(creditCheckService.assessmentsFor(loanId));
    }
}
