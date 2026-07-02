package ng.lendstack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.domain.enums.RiskTier;

@Entity
@Table(name = "loans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan extends BaseEntity {

    /** Human-readable reference, e.g. LN-2026-000042. */
    @Column(nullable = false, unique = true)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 1000)
    private String purpose;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    /** Annual nominal rate (%), fixed at approval; must be <= the CBN cap in system_config. */
    @Column(name = "interest_rate_annual", precision = 5, scale = 2)
    private BigDecimal interestRateAnnual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoanStatus status = LoanStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tier")
    private RiskTier riskTier;

    /** Latest credit score (0–100), denormalized from credit_assessments. */
    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(name = "guarantors_required", nullable = false)
    @Builder.Default
    private int guarantorsRequired = 0;

    @Column(name = "collateral_required", nullable = false)
    @Builder.Default
    private boolean collateralRequired = false;

    /** Principal not yet repaid; set at disbursement, reduced by each repayment. */
    @Column(name = "outstanding_principal", precision = 19, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "disbursed_at")
    private Instant disbursedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Version
    private long version;
}
