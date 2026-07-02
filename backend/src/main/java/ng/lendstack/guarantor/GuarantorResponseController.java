package ng.lendstack.guarantor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.guarantor.dto.GuarantorDecisionRequest;
import ng.lendstack.guarantor.dto.GuarantorInviteView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public — Guarantor response",
    description = "Reached from the emailed link — no login required; the single-use token in "
        + "the URL is the credential. Guarantors see the loan summary (no borrower PII) and "
        + "accept or decline once, within the expiry window.")
@RestController
@RequestMapping("/api/v1/guarantor-response/{token}")
@RequiredArgsConstructor
public class GuarantorResponseController {

    private final GuarantorService guarantorService;

    @Operation(summary = "View the guarantee request")
    @GetMapping
    public ApiResponse<GuarantorInviteView> view(@PathVariable String token) {
        return ApiResponse.ok(guarantorService.invite(token));
    }

    @Operation(summary = "Accept or decline",
        description = "One-shot: once responded, the decision is final (audit-logged). "
            + "When the last required guarantor accepts, the loan advances automatically.")
    @PostMapping
    public ApiResponse<GuarantorInviteView> respond(@PathVariable String token,
                                                    @Valid @RequestBody GuarantorDecisionRequest request) {
        return ApiResponse.ok(guarantorService.respond(token, request),
            request.accept() ? "Thank you — your acceptance has been recorded"
                : "Your response has been recorded");
    }
}
