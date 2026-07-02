package ng.lendstack.repayment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import ng.lendstack.domain.RepaymentInstallment;
import ng.lendstack.domain.enums.InstallmentStatus;

public record InstallmentResponse(
    String id,
    int installmentNumber,
    LocalDate dueDate,
    BigDecimal principalDue,
    BigDecimal interestDue,
    BigDecimal penaltyDue,
    BigDecimal totalDue,
    BigDecimal amountPaid,
    InstallmentStatus status,
    Instant paidAt
) {

    public static InstallmentResponse from(RepaymentInstallment i) {
        return new InstallmentResponse(i.getId().toString(), i.getInstallmentNumber(),
            i.getDueDate(), i.getPrincipalDue(), i.getInterestDue(), i.getPenaltyDue(),
            i.getTotalDue(), i.getAmountPaid(), i.getStatus(), i.getPaidAt());
    }
}
