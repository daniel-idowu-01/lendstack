package ng.lendstack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ng.lendstack.domain.enums.GuarantorStatus;

@Entity
@Table(name = "guarantors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guarantor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String relationship;

    private String occupation;

    @Column(name = "monthly_income", precision = 19, scale = 2)
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GuarantorStatus status = GuarantorStatus.PENDING;


    @Column(name = "response_token", nullable = false, unique = true)
    private String responseToken;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;


    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "decline_reason", length = 500)
    private String declineReason;
}
