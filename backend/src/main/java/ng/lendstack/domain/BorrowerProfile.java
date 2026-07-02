package ng.lendstack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ng.lendstack.common.crypto.PiiAttributeConverter;
import ng.lendstack.domain.enums.EmploymentStatus;

/**
 * Borrower KYC data. BVN, NIN and bank account number are encrypted at rest
 * (AES-256-GCM) and MUST never be logged or serialized raw (NDPC).
 */
@Entity
@Table(name = "borrower_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Bank Verification Number — 11 digits, mandatory before a loan can leave SUBMITTED. */
    @ToString.Exclude
    @Convert(converter = PiiAttributeConverter.class)
    @Column(name = "bvn", length = 512)
    private String bvn;

    @Column(name = "bvn_verified", nullable = false)
    @Builder.Default
    private boolean bvnVerified = false;

    /** National Identification Number — optional. */
    @ToString.Exclude
    @Convert(converter = PiiAttributeConverter.class)
    @Column(name = "nin", length = 512)
    private String nin;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status")
    private EmploymentStatus employmentStatus;

    @Column(name = "employer_name")
    private String employerName;

    @Column(name = "monthly_income", precision = 19, scale = 2)
    private BigDecimal monthlyIncome;

    @ToString.Exclude
    @Convert(converter = PiiAttributeConverter.class)
    @Column(name = "bank_account_number", length = 512)
    private String bankAccountNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String address;
}
