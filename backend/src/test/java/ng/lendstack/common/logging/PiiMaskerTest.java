package ng.lendstack.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PiiMaskerTest {

    @Test
    void masksElevenDigitBvnToLast4() {
        assertEquals("BVN *******5678 verified", PiiMasker.mask("BVN 22212345678 verified"));
    }

    @Test
    void masksTenDigitNuban() {
        assertEquals("acct ******6789", PiiMasker.mask("acct 0123456789"));
    }

    @Test
    void masksInsideJson() {
        assertEquals("{\"bvn\":\"*******5678\"}", PiiMasker.mask("{\"bvn\":\"22212345678\"}"));
    }

    @Test
    void leavesShortNumbersAndAmountsAlone() {
        assertEquals("₦450,000 over 12 months", PiiMasker.mask("₦450,000 over 12 months"));
        assertEquals("ref 123456", PiiMasker.mask("ref 123456"));
    }

    @Test
    void leavesLongerDigitRunsAlone() {
        // 12+ digits is not a BVN/NUBAN — don't mangle transaction ids
        assertEquals("tx 123456789012", PiiMasker.mask("tx 123456789012"));
    }
}
