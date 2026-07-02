package ng.lendstack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only audit trail. Enforced at two levels:
 * 1. A Postgres trigger rejects UPDATE/DELETE on audit_log outright.
 * 2. In the app, only {@code AuditService} may write — the repository is
 *    package-private inside the audit package.
 * No setters: rows are immutable once built.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. LOAN, CREDIT_ASSESSMENT, REPAYMENT, LENDER, COLLATERAL, GUARANTOR, CONFIG, USER. */
    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    /** e.g. STATE_CHANGE, SCORE_OVERRIDE, REPAYMENT_RECEIVED, CONFIG_UPDATED, ADMIN_OVERRIDE. */
    @Column(nullable = false)
    private String action;

    /** Email of the acting user, or "SYSTEM" for scheduled/webhook actions. */
    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    /** JSON snapshot before the change — PII masked before persistence. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;

    /** JSON snapshot after the change — PII masked before persistence. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;

    @Column(length = 1000)
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "ip_address")
    private String ipAddress;
}
