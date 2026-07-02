package ng.lendstack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ng.lendstack.domain.enums.RiskTier;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One row per scoring run. Officer overrides create a NEW row with
 * overridden=true and a mandatory reason — earlier assessments are never mutated.
 */
@Entity
@Table(name = "credit_assessments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditAssessment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tier", nullable = false)
    private RiskTier riskTier;

    /** Per-rule breakdown: [{rule, points, maxPoints, detail}, ...] — PII already masked. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String breakdown;

    @Column(nullable = false)
    @Builder.Default
    private boolean overridden = false;

    @Column(name = "override_reason", length = 1000)
    private String overrideReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessed_by")
    private User assessedBy;
}
