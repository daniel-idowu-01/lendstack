package ng.lendstack.loan.dto;

import java.util.List;

/** Full review view: application + borrower KYC summary + audited timeline. */
public record OfficerLoanDetailResponse(
    LoanResponse loan,
    BorrowerSummary borrower,
    List<LoanTimelineEntry> timeline
) {
}
