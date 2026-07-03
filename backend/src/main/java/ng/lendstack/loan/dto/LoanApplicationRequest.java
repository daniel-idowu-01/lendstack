package ng.lendstack.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;


public record LoanApplicationRequest(
    @NotNull @DecimalMin(value = "1000", message = "Loan amount is too small")
    BigDecimal amount,
    @NotBlank @Size(max = 1000) String purpose,
    @Min(1) @Max(120) int tenureMonths
) {
}
