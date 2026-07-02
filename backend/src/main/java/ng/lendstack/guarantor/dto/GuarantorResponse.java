package ng.lendstack.guarantor.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ng.lendstack.domain.Guarantor;
import ng.lendstack.domain.enums.GuarantorStatus;

public record GuarantorResponse(
    String id,
    String fullName,
    String email,
    String phone,
    String relationship,
    String occupation,
    BigDecimal monthlyIncome,
    GuarantorStatus status,
    Instant requestedAt,
    Instant respondedAt,
    Instant expiresAt,
    String declineReason
) {

    public static GuarantorResponse from(Guarantor g) {
        return new GuarantorResponse(g.getId().toString(), g.getFullName(), g.getEmail(),
            g.getPhone(), g.getRelationship(), g.getOccupation(), g.getMonthlyIncome(),
            g.getStatus(), g.getRequestedAt(), g.getRespondedAt(), g.getExpiresAt(),
            g.getDeclineReason());
    }
}
