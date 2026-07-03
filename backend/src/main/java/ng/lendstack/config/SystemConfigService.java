package ng.lendstack.config;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import ng.lendstack.audit.AuditService;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.domain.SystemConfig;
import ng.lendstack.repository.SystemConfigRepository;
import ng.lendstack.security.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository repository;
    private final AuditService auditService;
    private final RequestContext requestContext;

    @Transactional(readOnly = true)
    public BigDecimal getDecimal(String key) {
        return new BigDecimal(get(key).getValue());
    }

    @Transactional(readOnly = true)
    public int getInt(String key) {
        return Integer.parseInt(get(key).getValue());
    }

    @Transactional(readOnly = true)
    public List<SystemConfig> all() {
        return repository.findAll();
    }

    @Transactional
    public SystemConfig update(String key, String newValue) {
        SystemConfig config = get(key);
        validate(config, newValue);
        String oldValue = config.getValue();
        config.setValue(newValue);
        config.setUpdatedBy(requestContext.actor());
        repository.save(config);
        auditService.record("CONFIG", key, "CONFIG_UPDATED",
            java.util.Map.of("value", oldValue), java.util.Map.of("value", newValue), null);
        return config;
    }

    private SystemConfig get(String key) {
        return repository.findById(key).orElseThrow(() ->
            new IllegalStateException("Missing system_config key: " + key
                + " — check the V2 migration seeded correctly."));
    }

    private void validate(SystemConfig config, String newValue) {
        try {
            switch (config.getValueType()) {
                case "NUMBER" -> {
                    if (new BigDecimal(newValue).signum() < 0) {
                        throw ApiException.badRequest("CONFIG_INVALID", "Value must not be negative");
                    }
                }
                case "BOOLEAN" -> {
                    if (!"true".equalsIgnoreCase(newValue) && !"false".equalsIgnoreCase(newValue)) {
                        throw ApiException.badRequest("CONFIG_INVALID", "Value must be true or false");
                    }
                }
                default -> { }
            }
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("CONFIG_INVALID",
                "Value for " + config.getKey() + " must be a number");
        }
    }
}
