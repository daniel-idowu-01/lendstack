package ng.lendstack.repayment.dto;

import java.math.BigDecimal;


public record PaymentInitResponse(
    String reference,
    BigDecimal amount,
    String authorizationUrl
) {
}
