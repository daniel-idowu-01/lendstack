package ng.lendstack.common.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback converter wired in logback-spring.xml as %maskedMsg. Ensures no raw
 * BVN, NIN or bank account number can appear in any log line (NDPC requirement).
 */
public class PiiMaskingConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return PiiMasker.mask(super.convert(event));
    }
}
