package ng.lendstack.guarantor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.guarantor.dto.GuarantorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Officer — Guarantors",
    description = "Read-only guarantor statuses for application review: who was named, whether "
        + "they accepted/declined/expired, and their declared income (guarantor strength).")
@RestController
@RequestMapping("/api/v1/officer/loans/{loanId}/guarantors")
@RequiredArgsConstructor
public class OfficerGuarantorController {

    private final GuarantorService guarantorService;

    @Operation(summary = "Guarantor statuses for a loan")
    @GetMapping
    public ApiResponse<List<GuarantorResponse>> list(@PathVariable UUID loanId) {
        return ApiResponse.ok(guarantorService.forLoan(loanId));
    }
}
