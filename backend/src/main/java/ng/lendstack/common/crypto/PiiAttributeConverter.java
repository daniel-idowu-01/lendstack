package ng.lendstack.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA converter that transparently encrypts PII columns at rest.
 * Instantiated by Hibernate through Spring's bean container, so constructor
 * injection works.
 */
@Component
@Converter
public class PiiAttributeConverter implements AttributeConverter<String, String> {

    private final PiiEncryptionService encryptionService;

    public PiiAttributeConverter(PiiEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
