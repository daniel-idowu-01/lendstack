package ng.lendstack.loan.dto;

import java.util.List;


public record OfficerLoanDetailResponse(
    LoanResponse loan,
    BorrowerSummary borrower,
    List<LoanTimelineEntry> timeline
) {
}
