package ng.lendstack.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.ApiResponse;
import ng.lendstack.common.api.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — Audit trail",
    description = "System-wide immutable audit log: every state change, score override, "
        + "repayment, lender assignment, collateral update, guarantor response and config "
        + "change. Rows can never be updated or deleted (enforced by a database trigger). "
        + "PII in snapshots is masked (NDPC).")
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditService auditService;

    @Operation(summary = "Search the audit trail",
        description = "Filter by entity type (LOAN, CREDIT_ASSESSMENT, REPAYMENT, LENDER, "
            + "COLLATERAL, GUARANTOR, CONFIG, USER), entity id, acting user (email or SYSTEM) "
            + "and date range. Newest first.")
    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(PageResponse.from(
            auditService.search(entityType, entityId, performedBy, from, to,
                PageRequest.of(page, Math.min(size, 200), Sort.by(Sort.Direction.DESC, "timestamp"))),
            AuditLogResponse::from));
    }
}
