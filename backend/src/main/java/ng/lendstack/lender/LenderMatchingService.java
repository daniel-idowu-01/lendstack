package ng.lendstack.lender;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.audit.AuditService;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.domain.Lender;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.LoanFunding;
import ng.lendstack.domain.enums.RiskTier;
import ng.lendstack.repository.LenderRepository;
import ng.lendstack.repository.LoanFundingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class LenderMatchingService {

    private final LenderRepository lenderRepository;
    private final LoanFundingRepository fundingRepository;
    private final AuditService auditService;

    @Transactional(propagation = Propagation.MANDATORY)
    public List<LoanFunding> matchAndCommit(Loan loan) {
        RiskTier loanTier = loan.getRiskTier();
        if (loanTier == null || loanTier == RiskTier.DECLINED) {
            throw ApiException.conflict("UNFUNDABLE_TIER",
                "Loan has no fundable risk tier — run the credit check first");
        }
        List<Lender> eligible = lenderRepository.findByActiveTrue().stream()
            .filter(lender -> accepts(lender.getPreferredRiskTier(), loanTier))
            .filter(lender -> lender.getWalletBalance().signum() > 0)
            .filter(lender -> headroom(lender).signum() > 0)
            .sorted(Comparator.comparing(this::headroom).reversed())
            .toList();

        BigDecimal remaining = loan.getAmount();
        List<LoanFunding> fundings = new ArrayList<>();
        for (Lender lender : eligible) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal slice = remaining.min(lender.getWalletBalance()).min(headroom(lender));
            if (slice.signum() <= 0) {
                continue;
            }
            lender.setWalletBalance(lender.getWalletBalance().subtract(slice));
            lender.setCurrentExposure(lender.getCurrentExposure().add(slice));
            lenderRepository.save(lender);
            LoanFunding funding = fundingRepository.save(LoanFunding.builder()
                .loan(loan)
                .lender(lender)
                .amount(slice)
                .build());
            fundings.add(funding);
            auditService.record("LENDER", lender.getId().toString(), "LENDER_ASSIGNED",
                null, Map.of("loan", loan.getReference(), "amount", slice), null);
            remaining = remaining.subtract(slice);
        }

        if (remaining.signum() > 0) {
            throw ApiException.conflict("INSUFFICIENT_LENDER_FUNDING",
                ("Only ₦%,.2f of ₦%,.2f could be matched to lenders willing to fund a %s-risk "
                    + "loan. Register more lenders, top up wallets or raise exposure limits.")
                    .formatted(loan.getAmount().subtract(remaining), loan.getAmount(), loanTier));
        }
        log.info("Loan {} funded by {} lender(s)", loan.getReference(), fundings.size());
        return fundings;
    }


    private boolean accepts(RiskTier appetite, RiskTier loanTier) {
        return loanTier.ordinal() <= appetite.ordinal();
    }

    private BigDecimal headroom(Lender lender) {
        return lender.getMaxExposure().subtract(lender.getCurrentExposure());
    }
}
