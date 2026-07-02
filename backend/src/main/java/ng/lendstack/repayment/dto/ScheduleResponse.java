package ng.lendstack.repayment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ng.lendstack.domain.enums.LoanStatus;

/** The borrower's repayment dashboard for one loan. */
public record ScheduleResponse(
    String loanId,
    String loanReference,
    LoanStatus loanStatus,
    BigDecimal loanAmount,
    BigDecimal interestRateAnnual,
    BigDecimal outstandingPrincipal,
    BigDecimal totalOutstanding,
    LocalDate nextDueDate,
    List<InstallmentResponse> installments
) {
}
