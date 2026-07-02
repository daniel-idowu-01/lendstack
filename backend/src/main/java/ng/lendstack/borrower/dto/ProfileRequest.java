package ng.lendstack.borrower.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import ng.lendstack.domain.enums.EmploymentStatus;

/** Borrower KYC. BVN/NIN/account number are encrypted at rest and never echoed back raw. */
public record ProfileRequest(
    @Pattern(regexp = "^\\d{11}$", message = "BVN must be exactly 11 digits") String bvn,
    @Pattern(regexp = "^\\d{11}$", message = "NIN must be exactly 11 digits") String nin,
    EmploymentStatus employmentStatus,
    @Size(max = 255) String employerName,
    @PositiveOrZero BigDecimal monthlyIncome,
    @Pattern(regexp = "^\\d{10}$", message = "Account number must be 10 digits (NUBAN)")
    String bankAccountNumber,
    @Size(max = 255) String bankName,
    @Past LocalDate dateOfBirth,
    @Size(max = 255) String address
) {
}
