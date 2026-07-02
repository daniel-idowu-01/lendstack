package ng.lendstack.lender.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ==== STUB — REPLACE WITH REAL SETTLEMENT ====
 * In production, lender wallets would be funded through actual bank settlement
 * (virtual accounts / NIP transfers). Here ADMIN credits wallets directly; the
 * operation is audit-logged.
 */
public record WalletTopupRequest(
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount
) {
}
