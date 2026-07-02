package ng.lendstack.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.common.logging.PiiMasker;
import ng.lendstack.domain.AuditLog;
import ng.lendstack.security.RequestContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sole writer of the append-only audit trail. Called from every service that
 * mutates state. Snapshots are JSON-serialized and PII-masked before persisting
 * (NDPC: no raw BVN/NIN/account numbers, even in audit data).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final RequestContext requestContext;

    /** Writes in the caller's transaction so the audit entry commits atomically with the change. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, String entityId, String action,
                       Object oldValue, Object newValue, String reason) {
        repository.save(AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .performedBy(requestContext.actor())
            .oldValue(toMaskedJson(oldValue))
            .newValue(toMaskedJson(newValue))
            .reason(reason)
            .ipAddress(requestContext.clientIp())
            .build());
    }

    private String toMaskedJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String json = value instanceof String s ? s : objectMapper.writeValueAsString(value);
            return PiiMasker.mask(json);
        } catch (Exception e) {
            log.warn("Audit serialization fell back to toString for {}", value.getClass());
            return "\"" + PiiMasker.mask(String.valueOf(value)) + "\"";
        }
    }

    /** Admin audit viewer: filterable by entity type, actor, and date range. */
    @Transactional(readOnly = true)
    public Page<AuditLog> search(String entityType, String entityId, String performedBy,
                                 Instant from, Instant to, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (entityId != null && !entityId.isBlank()) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (performedBy != null && !performedBy.isBlank()) {
                predicates.add(cb.equal(root.get("performedBy"), performedBy));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    /** Timeline of a single entity (e.g. a loan's full state history), oldest first. */
    @Transactional(readOnly = true)
    public List<AuditLog> historyFor(String entityType, String entityId) {
        Specification<AuditLog> spec = (root, query, cb) -> cb.and(
            cb.equal(root.get("entityType"), entityType),
            cb.equal(root.get("entityId"), entityId));
        return repository.findAll(spec,
            org.springframework.data.domain.Sort.by("timestamp").ascending());
    }
}
