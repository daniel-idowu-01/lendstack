package ng.lendstack.loan;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.loan.dto.LoanResponse;
import ng.lendstack.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLoanService {

    private final LoanRepository loanRepository;
    private final LoanLifecycleService lifecycleService;
    private final LoanMapper loanMapper;

    @Transactional
    public LoanResponse overrideStatus(UUID loanId, LoanStatus target, String reason) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> ApiException.notFound("Loan not found"));
        lifecycleService.adminOverride(loan, target, reason);
        return loanMapper.toResponse(loan);
    }
}
