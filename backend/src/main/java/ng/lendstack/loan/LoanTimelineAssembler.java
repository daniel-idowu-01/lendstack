package ng.lendstack.loan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.audit.AuditService;
import ng.lendstack.domain.AuditLog;
import ng.lendstack.loan.dto.LoanTimelineEntry;
import org.springframework.stereotype.Component;

/**
 * Builds the visual application timeline from the loan's audit history.
 * Borrower views omit performedBy (staff identities stay internal).
 */
@Component
@RequiredArgsConstructor
public class LoanTimelineAssembler {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public List<LoanTimelineEntry> timelineFor(UUID loanId, boolean includeActor) {
        return auditService.historyFor("LOAN", loanId.toString()).stream()
            .map(entry -> toTimelineEntry(entry, includeActor))
            .toList();
    }

    private LoanTimelineEntry toTimelineEntry(AuditLog entry, boolean includeActor) {
        return new LoanTimelineEntry(
            entry.getAction(),
            statusOf(entry.getOldValue()),
            statusOf(entry.getNewValue()),
            entry.getReason(),
            includeActor ? entry.getPerformedBy() : null,
            entry.getTimestamp());
    }

    private String statusOf(String json) {
        if (json == null) {
            return null;
        }
        try {
            JsonNode status = objectMapper.readTree(json).get("status");
            return status == null ? null : status.asText();
        } catch (Exception e) {
            return null;
        }
    }
}
