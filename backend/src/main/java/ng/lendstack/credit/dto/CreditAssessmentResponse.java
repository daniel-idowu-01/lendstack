package ng.lendstack.credit.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import ng.lendstack.domain.enums.RiskTier;

public record CreditAssessmentResponse(
    String id,
    int score,
    RiskTier riskTier,
    JsonNode breakdown,
    boolean overridden,
    String overrideReason,
    String assessedBy,
    Instant createdAt
) {
}
