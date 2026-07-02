package ng.lendstack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ng.lendstack.domain.enums.CollateralType;
import ng.lendstack.domain.enums.VerificationStatus;

@Entity
@Table(name = "collaterals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Collateral extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollateralType type;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "estimated_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
