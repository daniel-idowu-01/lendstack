package ng.lendstack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ==== STUB — REPLACE WITH REAL PROVIDER ====
 * Outbox for email/SMS notifications (guarantor requests, approval notices,
 * repayment reminders). A scheduled worker currently marks rows SENT and logs
 * a masked line. Swap the worker for a real integration (e.g. Termii, SendGrid,
 * AWS SES) without touching callers.
 */
@Entity
@Table(name = "notification_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOutbox extends BaseEntity {

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    /** GUARANTOR_REQUEST | LOAN_APPROVED | LOAN_REJECTED | REPAYMENT_REMINDER | DISBURSEMENT | ... */
    @Column(nullable = false)
    private String type;

    /** PENDING | SENT | FAILED */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private String relatedEntityId;

    @Column(name = "sent_at")
    private Instant sentAt;
}
