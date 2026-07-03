package ng.lendstack.finance;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;


public final class Amortization {

    private static final MathContext MC = new MathContext(20);

    private Amortization() {
    }


    public static BigDecimal monthlyRate(BigDecimal annualRatePercent) {
        return annualRatePercent.divide(BigDecimal.valueOf(1200), MC);
    }


    public static BigDecimal monthlyPayment(BigDecimal principal, BigDecimal annualRatePercent,
                                            int months) {
        if (annualRatePercent.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        BigDecimal r = monthlyRate(annualRatePercent);
        BigDecimal onePlusRPowN = BigDecimal.ONE.add(r).pow(months, MC);
        BigDecimal payment = principal.multiply(r, MC).multiply(onePlusRPowN, MC)
            .divide(onePlusRPowN.subtract(BigDecimal.ONE), MC);
        return payment.setScale(2, RoundingMode.HALF_UP);
    }


    public static BigDecimal interestOn(BigDecimal outstandingBalance, BigDecimal annualRatePercent) {
        return outstandingBalance.multiply(monthlyRate(annualRatePercent), MC)
            .setScale(2, RoundingMode.HALF_UP);
    }
}
