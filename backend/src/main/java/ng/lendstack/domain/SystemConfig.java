package ng.lendstack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Runtime-editable configuration (CBN caps, penalty rates, tiers…). Seeded from
 * environment variables by Flyway on first run; thereafter the DB is the source
 * of truth and ADMIN edits it through the API (every change audit-logged).
 */
@Entity
@Table(name = "system_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    @Id
    @Column(name = "config_key")
    private String key;

    @Column(name = "config_value", nullable = false)
    private String value;

    /** NUMBER | BOOLEAN | STRING — for admin UI rendering/validation. */
    @Column(name = "value_type", nullable = false)
    private String valueType;

    @Column(length = 500)
    private String description;

    @Column(name = "updated_by")
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
