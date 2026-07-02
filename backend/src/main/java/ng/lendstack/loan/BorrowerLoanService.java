package ng.lendstack.loan;

import java.security.SecureRandom;
import java.time.Year;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.audit.AuditService;
import ng.lendstack.common.api.PageResponse;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.compliance.LoanPolicyService;
import ng.lendstack.domain.BorrowerProfile;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.LoanStatus;
import ng.lendstack.loan.dto.LoanApplicationRequest;
import ng.lendstack.loan.dto.LoanDetailResponse;
import ng.lendstack.loan.dto.LoanResponse;
import ng.lendstack.repository.BorrowerProfileRepository;
import ng.lendstack.repository.LoanRepository;
import ng.lendstack.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BorrowerLoanService {

    private static final String REFERENCE_ALPHABET = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BorrowerProfileRepository borrowerProfileRepository;
    private final LoanPolicyService policyService;
    private final LoanLifecycleService lifecycleService;
    private final LoanTimelineAssembler timelineAssembler;
    private final LoanMapper loanMapper;
    private final AuditService auditService;

    @Transactional
    public LoanResponse createDraft(UUID borrowerId, LoanApplicationRequest request) {
        policyService.validateApplication(request.amount(), request.tenureMonths());
        Loan loan = loanRepository.save(Loan.builder()
            .reference(newReference())
            .borrower(userRepository.getReferenceById(borrowerId))
            .amount(request.amount())
            .purpose(request.purpose())
            .tenureMonths(request.tenureMonths())
            .build());
        auditService.record("LOAN", loan.getId().toString(), "APPLICATION_CREATED",
            null, Map.of("status", LoanStatus.DRAFT.name(), "amount", request.amount(),
                "tenureMonths", request.tenureMonths()), null);
        return loanMapper.toResponse(loan);
    }

    @Transactional
    public LoanResponse updateDraft(UUID borrowerId, UUID loanId, LoanApplicationRequest request) {
        Loan loan = ownedLoan(borrowerId, loanId);
        if (loan.getStatus() != LoanStatus.DRAFT) {
            throw ApiException.conflict("NOT_A_DRAFT",
                "Only DRAFT applications can be edited — this loan is " + loan.getStatus());
        }
        policyService.validateApplication(request.amount(), request.tenureMonths());
        Map<String, Object> before = Map.of("amount", loan.getAmount(),
            "purpose", loan.getPurpose(), "tenureMonths", loan.getTenureMonths());
        loan.setAmount(request.amount());
        loan.setPurpose(request.purpose());
        loan.setTenureMonths(request.tenureMonths());
        loanRepository.save(loan);
        auditService.record("LOAN", loan.getId().toString(), "APPLICATION_UPDATED",
            before, Map.of("amount", request.amount(), "purpose", request.purpose(),
                "tenureMonths", request.tenureMonths()), null);
        return loanMapper.toResponse(loan);
    }

    /**
     * DRAFT → SUBMITTED. BVN is required here (fail fast for the borrower) and
     * checked again when an officer starts the review — a loan can never get
     * past SUBMITTED without BVN linkage (CBN customer due diligence).
     */
    @Transactional
    public LoanResponse submit(UUID borrowerId, UUID loanId) {
        Loan loan = ownedLoan(borrowerId, loanId);
        if (loan.getStatus() != LoanStatus.DRAFT) {
            throw ApiException.conflict("ALREADY_SUBMITTED",
                "This application has already been submitted");
        }
        BorrowerProfile profile = borrowerProfileRepository.findByUserId(borrowerId)
            .orElseThrow(() -> ApiException.badRequest("KYC_INCOMPLETE",
                "Complete your KYC profile before submitting a loan application"));
        if (profile.getBvn() == null || profile.getBvn().isBlank()) {
            throw ApiException.badRequest("BVN_REQUIRED",
                "Add your BVN to your profile before submitting — BVN linkage is mandatory "
                    + "under CBN customer due-diligence rules");
        }
        policyService.validateApplication(loan.getAmount(), loan.getTenureMonths());
        loan.setGuarantorsRequired(policyService.guarantorsRequiredFor(loan.getAmount()));
        loan.setCollateralRequired(policyService.collateralRequiredFor(loan.getAmount()));
        lifecycleService.transition(loan, LoanStatus.SUBMITTED, "Application submitted by borrower");
        return loanMapper.toResponse(loan);
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> myLoans(UUID borrowerId, Pageable pageable) {
        return PageResponse.from(
            loanRepository.findByBorrowerId(borrowerId, pageable), loanMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LoanDetailResponse myLoanDetail(UUID borrowerId, UUID loanId) {
        Loan loan = ownedLoan(borrowerId, loanId);
        return new LoanDetailResponse(
            loanMapper.toResponse(loan),
            timelineAssembler.timelineFor(loan.getId(), false));
    }

    /** 404 (not 403) for other borrowers' loans, so loan IDs cannot be probed. */
    private Loan ownedLoan(UUID borrowerId, UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> ApiException.notFound("Loan not found"));
        if (!loan.getBorrower().getId().equals(borrowerId)) {
            throw ApiException.notFound("Loan not found");
        }
        return loan;
    }

    private String newReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                digits.append(REFERENCE_ALPHABET.charAt(RANDOM.nextInt(REFERENCE_ALPHABET.length())));
            }
            String reference = "LN-%d-%s".formatted(Year.now().getValue(), digits);
            if (loanRepository.findByReference(reference).isEmpty()) {
                return reference;
            }
        }
        throw new IllegalStateException("Could not generate a unique loan reference");
    }
}
