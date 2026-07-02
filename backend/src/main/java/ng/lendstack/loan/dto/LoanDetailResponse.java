package ng.lendstack.loan.dto;

import java.util.List;

public record LoanDetailResponse(
    LoanResponse loan,
    List<LoanTimelineEntry> timeline
) {
}
