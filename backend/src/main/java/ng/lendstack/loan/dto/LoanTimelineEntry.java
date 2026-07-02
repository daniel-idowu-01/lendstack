package ng.lendstack.loan.dto;

import java.time.Instant;

/**
 * One event in a loan's history, derived from the audit trail.
 * performedBy is null in borrower-facing views (staff identities are internal).
 */
public record LoanTimelineEntry(
    String action,
    String fromStatus,
    String toStatus,
    String reason,
    String performedBy,
    Instant timestamp
) {
}
