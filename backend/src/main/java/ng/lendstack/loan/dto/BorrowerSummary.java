package ng.lendstack.loan.dto;

import java.math.BigDecimal;
import ng.lendstack.domain.enums.EmploymentStatus;

/**
 * Borrower KYC summary for officer review. Never carries raw BVN/NIN/account
 * numbers — only masked values and verification flags (NDPC).
 */
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
