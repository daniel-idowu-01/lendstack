package ng.lendstack.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;


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
