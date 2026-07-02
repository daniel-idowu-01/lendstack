package ng.lendstack.loan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.loan.dto.LoanResponse;
import ng.lendstack.loan.dto.StatusOverrideRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — Loan overrides",
    description = "ADMIN escape hatch for exceptional cases (e.g. reversing an erroneous "
        + "rejection, forcing closure after an out-of-band settlement). Overrides bypass the "
        + "normal transition graph, require a written reason, and are logged as ADMIN_OVERRIDE.")
@RestController
@RequestMapping("/api/v1/admin/loans")
@RequiredArgsConstructor
public class AdminLoanController {

    private final AdminLoanService adminLoanService;

    @Operation(summary = "Override a loan's state",
        description = "Moves the loan to any state, including backwards or out of a terminal "
            + "state. The mandatory reason and the acting admin are recorded immutably.")
    @PostMapping("/{loanId}/status-override")
    public ApiResponse<LoanResponse> override(@PathVariable UUID loanId,
                                              @Valid @RequestBody StatusOverrideRequest request) {
        return ApiResponse.ok(
            adminLoanService.overrideStatus(loanId, request.targetStatus(), request.reason()),
            "Loan moved to " + request.targetStatus() + " by admin override");
    }
}
