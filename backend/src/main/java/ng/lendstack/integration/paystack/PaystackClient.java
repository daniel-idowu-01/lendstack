package ng.lendstack.integration.paystack;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin Paystack API client. Amounts are converted to kobo (×100) as Paystack
 * requires. Webhook authenticity is HMAC-SHA512 of the raw body with the
 * secret key, compared against the x-paystack-signature header — every webhook
 * MUST pass {@link #isValidSignature} before any processing.
 */
@Slf4j
@Component
public class PaystackClient {

    private final String secretKey;
    private final RestClient restClient;

    public PaystackClient(@Value("${lendstack.paystack.secret-key}") String secretKey,
                          @Value("${lendstack.paystack.base-url}") String baseUrl) {
        this.secretKey = secretKey;
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
            .build();
    }

    public record InitResult(String authorizationUrl, String accessCode) {
    }

    public InitResult initializeTransaction(String email, BigDecimal amountNaira,
                                            String reference, String callbackUrl) {
        requireConfigured();
        JsonNode response = restClient.post()
            .uri("/transaction/initialize")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "email", email,
                "amount", toKobo(amountNaira),
                "reference", reference,
                "callback_url", callbackUrl,
                "currency", "NGN"))
            .retrieve()
            .body(JsonNode.class);
        if (response == null || !response.path("status").asBoolean(false)) {
            throw ApiException.badRequest("PAYSTACK_INIT_FAILED",
                "Paystack could not initialize the transaction"
                    + (response == null ? "" : ": " + response.path("message").asText()));
        }
        JsonNode data = response.get("data");
        return new InitResult(data.path("authorization_url").asText(),
            data.path("access_code").asText());
    }

    /** Server-side verification — used by the redirect-callback path as a webhook fallback. */
    public JsonNode verifyTransaction(String reference) {
        requireConfigured();
        JsonNode response = restClient.get()
            .uri("/transaction/verify/{reference}", reference)
            .retrieve()
            .body(JsonNode.class);
        if (response == null || !response.path("status").asBoolean(false)) {
            throw ApiException.badRequest("PAYSTACK_VERIFY_FAILED",
                "Paystack could not verify transaction " + reference);
        }
        return response.get("data");
    }

    public boolean isValidSignature(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank() || secretKey == null
                || secretKey.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String expected = HexFormat.of().formatHex(
                mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            return constantTimeEquals(expected, signatureHeader.toLowerCase());
        } catch (Exception e) {
            log.error("Webhook signature check failed to compute", e);
            return false;
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private long toKobo(BigDecimal naira) {
        return naira.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    private void requireConfigured() {
        if (secretKey == null || secretKey.isBlank()) {
            throw ApiException.badRequest("PAYSTACK_NOT_CONFIGURED",
                "PAYSTACK_SECRET_KEY is not set — add your Paystack test key to the environment");
        }
    }
}
