package ng.lendstack.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AmortizationTest {

    @Test
    void monthlyPaymentMatchesKnownAnnuity() {
        BigDecimal payment = Amortization.monthlyPayment(
            new BigDecimal("1000000"), new BigDecimal("24"), 12);
        assertEquals(new BigDecimal("94559.60"), payment);
    }

    @Test
    void zeroRateDegradesToStraightDivision() {
        BigDecimal payment = Amortization.monthlyPayment(
            new BigDecimal("120000"), BigDecimal.ZERO, 12);
        assertEquals(new BigDecimal("10000.00"), payment);
    }

    @Test
    void reducingBalanceScheduleClearsPrincipalExactly() {
        BigDecimal principal = new BigDecimal("500000");
        BigDecimal rate = new BigDecimal("30");
        int months = 6;
        BigDecimal payment = Amortization.monthlyPayment(principal, rate, months);

        BigDecimal outstanding = principal;
        BigDecimal totalInterest = BigDecimal.ZERO;
        for (int i = 1; i <= months; i++) {
            BigDecimal interest = Amortization.interestOn(outstanding, rate);
            BigDecimal principalPart = i == months ? outstanding : payment.subtract(interest);
            outstanding = outstanding.subtract(principalPart);
            totalInterest = totalInterest.add(interest);
        }
        assertEquals(0, outstanding.compareTo(BigDecimal.ZERO), "principal must sum exactly");
        assertTrue(totalInterest.compareTo(new BigDecimal("75000")) < 0,
            "reducing balance must charge less than flat rate, got " + totalInterest);
    }
}
