package ng.lendstack.domain.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoanStatusTest {

    @Test
    void happyPathIsFullyConnected() {
        assertTrue(LoanStatus.DRAFT.canTransitionTo(LoanStatus.SUBMITTED));
        assertTrue(LoanStatus.SUBMITTED.canTransitionTo(LoanStatus.UNDER_REVIEW));
        assertTrue(LoanStatus.UNDER_REVIEW.canTransitionTo(LoanStatus.CREDIT_CHECK));
        assertTrue(LoanStatus.CREDIT_CHECK.canTransitionTo(LoanStatus.PENDING_GUARANTOR));
        assertTrue(LoanStatus.PENDING_GUARANTOR.canTransitionTo(LoanStatus.PENDING_COLLATERAL));
        assertTrue(LoanStatus.PENDING_COLLATERAL.canTransitionTo(LoanStatus.APPROVED));
        assertTrue(LoanStatus.APPROVED.canTransitionTo(LoanStatus.DISBURSED));
        assertTrue(LoanStatus.DISBURSED.canTransitionTo(LoanStatus.ACTIVE));
        assertTrue(LoanStatus.ACTIVE.canTransitionTo(LoanStatus.CLOSED));
    }

    @Test
    void statesCannotBeSkipped() {
        assertFalse(LoanStatus.DRAFT.canTransitionTo(LoanStatus.APPROVED));
        assertFalse(LoanStatus.SUBMITTED.canTransitionTo(LoanStatus.DISBURSED));
        assertFalse(LoanStatus.CREDIT_CHECK.canTransitionTo(LoanStatus.APPROVED));
        assertFalse(LoanStatus.APPROVED.canTransitionTo(LoanStatus.ACTIVE));
    }

    @Test
    void guarantorFailureReturnsToReview() {
        assertTrue(LoanStatus.PENDING_GUARANTOR.canTransitionTo(LoanStatus.UNDER_REVIEW));
    }

    @Test
    void delinquencyCuresAndDefaults() {
        assertTrue(LoanStatus.ACTIVE.canTransitionTo(LoanStatus.DELINQUENT));
        assertTrue(LoanStatus.DELINQUENT.canTransitionTo(LoanStatus.ACTIVE));
        assertTrue(LoanStatus.DELINQUENT.canTransitionTo(LoanStatus.DEFAULTED));
        assertTrue(LoanStatus.DEFAULTED.canTransitionTo(LoanStatus.WRITTEN_OFF));
        assertTrue(LoanStatus.DEFAULTED.canTransitionTo(LoanStatus.CLOSED));
    }

    @Test
    void terminalStatesHaveNoExit() {
        assertTrue(LoanStatus.CLOSED.isTerminal());
        assertTrue(LoanStatus.REJECTED.isTerminal());
        assertTrue(LoanStatus.WRITTEN_OFF.isTerminal());
        assertFalse(LoanStatus.ACTIVE.isTerminal());
    }
}
