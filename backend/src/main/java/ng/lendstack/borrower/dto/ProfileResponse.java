package ng.lendstack.borrower.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import ng.lendstack.domain.enums.EmploymentStatus;

/** PII is returned masked (last 4 digits) — the raw values never leave the backend. */
public record ProfileResponse(
    String bvnMasked,
    boolean bvnVerified,
    String ninMasked,
    EmploymentStatus employmentStatus,
    String employerName,
    BigDecimal monthlyIncome,
    String bankAccountMasked,
    String bankName,
    LocalDate dateOfBirth,
    String address,
    boolean kycComplete
) {
}
