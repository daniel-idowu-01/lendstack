package ng.lendstack.common.logging;

import java.util.regex.Pattern;


public final class PiiMasker {

    private static final Pattern PII_NUMBER = Pattern.compile("(?<!\\d)(\\d{10,11})(?!\\d)");

    private PiiMasker() {
    }

    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return PII_NUMBER.matcher(text).replaceAll(m -> maskDigits(m.group(1)));
    }


    public static String maskDigits(String digits) {
        if (digits == null || digits.length() <= 4) {
            return "****";
        }
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }
}
