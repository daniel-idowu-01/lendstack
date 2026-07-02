package ng.lendstack.config.dto;

import java.time.Instant;
import ng.lendstack.domain.SystemConfig;

public record ConfigResponse(
    String key,
    String value,
    String valueType,
    String description,
    String updatedBy,
    Instant updatedAt
) {

    public static ConfigResponse from(SystemConfig c) {
        return new ConfigResponse(c.getKey(), c.getValue(), c.getValueType(),
            c.getDescription(), c.getUpdatedBy(), c.getUpdatedAt());
    }
}
