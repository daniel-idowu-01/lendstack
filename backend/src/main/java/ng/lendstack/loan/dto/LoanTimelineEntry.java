package ng.lendstack.loan.dto;

import java.time.Instant;


public record LoanTimelineEntry(
    String action,
    String fromStatus,
    String toStatus,
    String reason,
    String performedBy,
    Instant timestamp
) {
}
