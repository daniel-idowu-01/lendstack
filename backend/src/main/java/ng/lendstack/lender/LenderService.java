package ng.lendstack.lender;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.audit.AuditService;
import ng.lendstack.common.api.PageResponse;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.domain.Lender;
import ng.lendstack.domain.enums.RiskTier;
import ng.lendstack.lender.dto.FundingResponse;
import ng.lendstack.lender.dto.LenderRequest;
import ng.lendstack.lender.dto.LenderResponse;
import ng.lendstack.lender.dto.LenderUpdateRequest;
import ng.lendstack.repository.LenderRepository;
import ng.lendstack.repository.LoanFundingRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LenderService {

    private final LenderRepository lenderRepository;
    private final LoanFundingRepository fundingRepository;
    private final AuditService auditService;

    @Transactional
    public LenderResponse register(LenderRequest request) {
        if (request.preferredRiskTier() == RiskTier.DECLINED) {
            throw ApiException.badRequest("INVALID_RISK_TIER",
                "Preferred risk tier must be LOW, MEDIUM or HIGH");
        }
        if (lenderRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw ApiException.conflict("EMAIL_TAKEN", "A lender with this email already exists");
        }
        Lender lender = lenderRepository.save(Lender.builder()
            .name(request.name())
            .type(request.type())
            .email(request.email().toLowerCase())
            .maxExposure(request.maxExposure())
            .preferredRiskTier(request.preferredRiskTier())
            .build());
        auditService.record("LENDER", lender.getId().toString(), "LENDER_REGISTERED",
            null, Map.of("name", lender.getName(), "type", lender.getType().name(),
                "maxExposure", lender.getMaxExposure()), null);
        return LenderResponse.from(lender);
    }

    @Transactional(readOnly = true)
    public PageResponse<LenderResponse> list(Pageable pageable) {
        return PageResponse.from(lenderRepository.findAll(pageable), LenderResponse::from);
    }

    @Transactional
    public LenderResponse update(UUID lenderId, LenderUpdateRequest request) {
        Lender lender = get(lenderId);
        Map<String, Object> before = snapshot(lender);
        if (request.maxExposure() != null) {
            lender.setMaxExposure(request.maxExposure());
        }
        if (request.preferredRiskTier() != null) {
            if (request.preferredRiskTier() == RiskTier.DECLINED) {
                throw ApiException.badRequest("INVALID_RISK_TIER",
                    "Preferred risk tier must be LOW, MEDIUM or HIGH");
            }
            lender.setPreferredRiskTier(request.preferredRiskTier());
        }
        if (request.active() != null) {
            lender.setActive(request.active());
        }
        lenderRepository.save(lender);
        auditService.record("LENDER", lender.getId().toString(), "LENDER_UPDATED",
            before, snapshot(lender), null);
        return LenderResponse.from(lender);
    }


    @Transactional
    public LenderResponse topUpWallet(UUID lenderId, BigDecimal amount) {
        Lender lender = get(lenderId);
        BigDecimal before = lender.getWalletBalance();
        lender.setWalletBalance(before.add(amount));
        lenderRepository.save(lender);
        auditService.record("LENDER", lender.getId().toString(), "WALLET_TOPUP",
            Map.of("walletBalance", before), Map.of("walletBalance", lender.getWalletBalance()),
            "Stub settlement — replace with real bank funding flow");
        return LenderResponse.from(lender);
    }

    @Transactional(readOnly = true)
    public PageResponse<FundingResponse> portfolio(UUID lenderId, Pageable pageable) {
        get(lenderId);
        return PageResponse.from(fundingRepository.findByLenderId(lenderId, pageable),
            FundingResponse::from);
    }

    private Lender get(UUID lenderId) {
        return lenderRepository.findById(lenderId)
            .orElseThrow(() -> ApiException.notFound("Lender not found"));
    }

    private Map<String, Object> snapshot(Lender lender) {
        return Map.of(
            "maxExposure", lender.getMaxExposure(),
            "preferredRiskTier", lender.getPreferredRiskTier().name(),
            "active", lender.isActive());
    }
}
