package ng.lendstack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ng.lendstack.domain.enums.LenderType;
import ng.lendstack.domain.enums.RiskTier;


@Entity
@Table(name = "lenders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lender extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LenderType type;

    @Column(nullable = false, unique = true)
    private String email;


    @Column(name = "wallet_balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal walletBalance = BigDecimal.ZERO;


    @Column(name = "max_exposure", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxExposure;


    @Column(name = "current_exposure", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal currentExposure = BigDecimal.ZERO;


    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_risk_tier", nullable = false)
    @Builder.Default
    private RiskTier preferredRiskTier = RiskTier.MEDIUM;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
