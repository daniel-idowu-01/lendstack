package ng.lendstack.loan;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.common.api.PageResponse;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.common.logging.PiiMasker;
import ng.lendstack.domain.BorrowerProfile;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.domain.enums.RiskTier;
import ng.lendstack.loan.dto.BorrowerSummary;
import ng.lendstack.loan.dto.LoanResponse;
import ng.lendstack.loan.dto.OfficerLoanDetailResponse;
import ng.lendstack.repository.BorrowerProfileRepository;
import ng.lendstack.repository.LoanRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfficerLoanService {

    private final LoanRepository loanRepository;
    private final BorrowerProfileRepository borrowerProfileRepository;
    private final LoanLifecycleService lifecycleService;
    private final LoanTimelineAssembler timelineAssembler;
    private final LoanMapper loanMapper;


    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> queue(LoanStatus status, RiskTier riskTier,
                                            BigDecimal minAmount, BigDecimal maxAmount,
                                            Pageable pageable) {
        Specification<Loan> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), LoanStatus.DRAFT));
            }
            if (riskTier != null) {
                predicates.add(cb.equal(root.get("riskTier"), riskTier));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(loanRepository.findAll(spec, pageable), loanMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OfficerLoanDetailResponse detail(UUID loanId) {
        Loan loan = getLoan(loanId);
        return new OfficerLoanDetailResponse(
            loanMapper.toResponse(loan),
            borrowerSummary(loan),
            timelineAssembler.timelineFor(loan.getId(), true));
    }


    @Transactional
    public LoanResponse startReview(UUID loanId) {
        Loan loan = getLoan(loanId);
        BorrowerProfile profile = borrowerProfileRepository
            .findByUserId(loan.getBorrower().getId()).orElse(null);
        if (profile == null || profile.getBvn() == null || profile.getBvn().isBlank()) {
            throw ApiException.conflict("BVN_REQUIRED",
                "This application cannot enter review: the borrower has no BVN on file "
                    + "(mandatory under CBN customer due-diligence rules)");
        }
        lifecycleService.transition(loan, LoanStatus.UNDER_REVIEW, "Review started by loan officer");
        return loanMapper.toResponse(loan);
    }


    @Transactional
    public LoanResponse reject(UUID loanId, String reason) {
        Loan loan = getLoan(loanId);
        lifecycleService.transition(loan, LoanStatus.REJECTED, reason);
        return loanMapper.toResponse(loan);
    }


    @Transactional
    public LoanResponse writeOff(UUID loanId, String reason) {
        Loan loan = getLoan(loanId);
        lifecycleService.transition(loan, LoanStatus.WRITTEN_OFF, reason);
        return loanMapper.toResponse(loan);
    }

    Loan getLoan(UUID loanId) {
        return loanRepository.findById(loanId)
            .orElseThrow(() -> ApiException.notFound("Loan not found"));
    }

    private BorrowerSummary borrowerSummary(Loan loan) {
        BorrowerProfile profile = borrowerProfileRepository
            .findByUserId(loan.getBorrower().getId()).orElse(null);
        return new BorrowerSummary(
            loan.getBorrower().getId().toString(),
            loan.getBorrower().getFullName(),
            loan.getBorrower().getEmail(),
            loan.getBorrower().getPhone(),
            profile == null ? null : profile.getEmploymentStatus(),
            profile == null ? null : profile.getEmployerName(),
            profile == null ? null : profile.getMonthlyIncome(),
            profile == null ? null : profile.getBankName(),
            profile == null || profile.getBvn() == null
                ? null : PiiMasker.maskDigits(profile.getBvn()),
            profile != null && profile.isBvnVerified());
    }
}
