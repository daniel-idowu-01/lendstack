package ng.lendstack.domain.enums;

import java.util.Map;
import java.util.Set;

/**
 * Loan lifecycle. Transitions are a branching graph (confirmed with product):
 *
 * <pre>
 * DRAFT → SUBMITTED → UNDER_REVIEW → CREDIT_CHECK → PENDING_GUARANTOR
 *       → PENDING_COLLATERAL → APPROVED → DISBURSED → ACTIVE ⇄ DELINQUENT
 *
 * UNDER_REVIEW / CREDIT_CHECK → REJECTED                (terminal)
 * PENDING_GUARANTOR → UNDER_REVIEW                      (72h guarantor expiry)
 * ACTIVE → CLOSED                                       (fully repaid)
 * DELINQUENT → ACTIVE                                   (arrears cured)
 * DELINQUENT → DEFAULTED → WRITTEN_OFF | CLOSED         (write-off or settled)
 * </pre>
 *
 * Loans that require no guarantor/collateral still pass THROUGH those states —
 * the transition is applied automatically and audit-logged, never skipped.
 * Any move outside this graph requires an ADMIN override, which is audit-logged.
 */
public enum LoanStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    CREDIT_CHECK,
    PENDING_GUARANTOR,
    PENDING_COLLATERAL,
    APPROVED,
    DISBURSED,
    ACTIVE,
    DELINQUENT,
    DEFAULTED,
    CLOSED,
    REJECTED,
    WRITTEN_OFF;

    private static final Map<LoanStatus, Set<LoanStatus>> ALLOWED = Map.ofEntries(
        Map.entry(DRAFT, Set.of(SUBMITTED)),
        Map.entry(SUBMITTED, Set.of(UNDER_REVIEW)),
        Map.entry(UNDER_REVIEW, Set.of(CREDIT_CHECK, REJECTED)),
        Map.entry(CREDIT_CHECK, Set.of(PENDING_GUARANTOR, REJECTED)),
        Map.entry(PENDING_GUARANTOR, Set.of(PENDING_COLLATERAL, UNDER_REVIEW)),
        Map.entry(PENDING_COLLATERAL, Set.of(APPROVED)),
        Map.entry(APPROVED, Set.of(DISBURSED)),
        Map.entry(DISBURSED, Set.of(ACTIVE)),
        Map.entry(ACTIVE, Set.of(DELINQUENT, CLOSED)),
        Map.entry(DELINQUENT, Set.of(ACTIVE, DEFAULTED, CLOSED)),
        Map.entry(DEFAULTED, Set.of(WRITTEN_OFF, CLOSED)),
        Map.entry(CLOSED, Set.of()),
        Map.entry(REJECTED, Set.of()),
        Map.entry(WRITTEN_OFF, Set.of())
    );

    public boolean canTransitionTo(LoanStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return ALLOWED.getOrDefault(this, Set.of()).isEmpty();
    }
}
