package ng.lendstack.common.logging;

import java.util.regex.Pattern;

/**
 * NDPC compliance: masks Nigerian PII in free text before it reaches logs or
 * audit JSON payloads. Masks:
 * <ul>
 *   <li>BVN / NIN — any standalone 11-digit number</li>
 *   <li>NUBAN bank account numbers — any standalone 10-digit number</li>
 * </ul>
 * Only the last 4 digits are retained. Shorter numbers (amounts, phone numbers
 * with +234 prefix keep their separators) are left untouched.
 */
public final class PiiMasker {

    // 10 or 11 consecutive digits not adjacent to other digits
    private static final Pattern PII_NUMBER = Pattern.compile("(?<!\\d)(\\d{10,11})(?!\\d)");

    private PiiMasker() {
    }

    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return PII_NUMBER.matcher(text).replaceAll(m -> maskDigits(m.group(1)));
    }

    /** e.g. 22212345678 -> *******5678 */
    public static String maskDigits(String digits) {
        if (digits == null || digits.length() <= 4) {
            return "****";
        }
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }
}
