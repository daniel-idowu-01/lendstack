package ng.lendstack.loan.dto;

import java.math.BigDecimal;
import ng.lendstack.domain.enums.EmploymentStatus;


public record BorrowerSummary(
    String userId,
    String fullName,
    String email,
    String phone,
    EmploymentStatus employmentStatus,
    String employerName,
    BigDecimal monthlyIncome,
    String bankName,
    String bvnMasked,
    boolean bvnVerified
) {
}
