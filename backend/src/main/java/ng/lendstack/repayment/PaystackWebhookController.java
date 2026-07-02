package ng.lendstack.repayment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.integration.paystack.PaystackClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public — Paystack webhook",
    description = "Receives Paystack events. Every request's x-paystack-signature (HMAC-SHA512 "
        + "of the raw body with the secret key) is verified before any processing; invalid "
        + "signatures get 401 and are never parsed.")
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class PaystackWebhookController {

    private final PaystackClient paystackClient;
    private final RepaymentService repaymentService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Paystack event webhook")
    @PostMapping("/paystack")
    public ResponseEntity<Void> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {
        if (!paystackClient.isValidSignature(rawBody, signature)) {
            log.warn("Rejected Paystack webhook with invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            repaymentService.handleWebhookEvent(objectMapper.readTree(rawBody));
        } catch (Exception e) {
            // 5xx makes Paystack retry — correct for transient failures.
            log.error("Webhook processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok().build();
    }
}
