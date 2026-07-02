package ng.lendstack.audit;

import java.time.Instant;
import ng.lendstack.domain.AuditLog;

public record AuditLogResponse(
    Long id,
    String entityType,
    String entityId,
    String action,
    String performedBy,
    String oldValue,
    String newValue,
    String reason,
    Instant timestamp,
    String ipAddress
) {

    static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getEntityType(), log.getEntityId(),
            log.getAction(), log.getPerformedBy(), log.getOldValue(), log.getNewValue(),
            log.getReason(), log.getTimestamp(), log.getIpAddress());
    }
}
