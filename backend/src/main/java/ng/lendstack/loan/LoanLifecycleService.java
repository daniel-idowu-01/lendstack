package ng.lendstack.loan;

import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import ng.lendstack.audit.AuditService;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The ONLY place a loan's status may change. Enforces the transition graph in
 * {@link LoanStatus}; every change (including ADMIN overrides that bypass the
 * graph) writes an immutable audit entry in the same transaction.
 */
@Service
@RequiredArgsConstructor
public class LoanLifecycleService {

    private final LoanRepository loanRepository;
    private final AuditService auditService;

    /** Normal transition following the lifecycle graph. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void transition(Loan loan, LoanStatus target, String reason) {
        LoanStatus from = loan.getStatus();
        if (!from.canTransitionTo(target)) {
            throw ApiException.conflict("INVALID_STATE_TRANSITION",
                "A loan in %s cannot move to %s".formatted(from, target));
        }
        apply(loan, from, target, "STATE_CHANGE", reason);
    }

    /**
     * ADMIN-only escape hatch: moves a loan anywhere, including backwards or
     * out of a terminal state. Mandatory reason; logged as ADMIN_OVERRIDE so
     * these stand out in the audit trail.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void adminOverride(Loan loan, LoanStatus target, String reason) {
        LoanStatus from = loan.getStatus();
        if (from == target) {
            throw ApiException.badRequest("NO_OP_TRANSITION", "Loan is already in " + target);
        }
        apply(loan, from, target, "ADMIN_OVERRIDE", reason);
    }

    private void apply(Loan loan, LoanStatus from, LoanStatus target, String action, String reason) {
        loan.setStatus(target);
        Instant now = Instant.now();
        switch (target) {
            case SUBMITTED -> loan.setSubmittedAt(now);
            case APPROVED -> loan.setApprovedAt(now);
            case DISBURSED -> loan.setDisbursedAt(now);
            case CLOSED, WRITTEN_OFF -> loan.setClosedAt(now);
            case REJECTED -> {
                loan.setClosedAt(now);
                loan.setRejectionReason(reason);
            }
            default -> { /* no denormalized timestamp */ }
        }
        loanRepository.save(loan);
        auditService.record("LOAN", loan.getId().toString(), action,
            Map.of("status", from.name()), Map.of("status", target.name()), reason);
    }
}
