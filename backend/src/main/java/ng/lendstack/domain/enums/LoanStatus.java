package ng.lendstack.domain.enums;

import java.util.Map;
import java.util.Set;


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
