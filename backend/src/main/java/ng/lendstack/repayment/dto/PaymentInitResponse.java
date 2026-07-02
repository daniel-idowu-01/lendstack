package ng.lendstack.repayment.dto;

import java.math.BigDecimal;

/** Redirect the borrower's browser to authorizationUrl to complete payment on Paystack. */
public record PaymentInitResponse(
    String reference,
    BigDecimal amount,
    String authorizationUrl
) {
}
