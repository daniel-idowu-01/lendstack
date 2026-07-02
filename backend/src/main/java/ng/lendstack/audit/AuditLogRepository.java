package ng.lendstack.audit;

import ng.lendstack.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * PACKAGE-PRIVATE by design: no service outside ng.lendstack.audit can write
 * to the audit trail directly. All writes go through {@link AuditService}.
 * The table itself also has a Postgres trigger blocking UPDATE/DELETE.
 */
interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}
