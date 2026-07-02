package ng.lendstack.borrower;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ng.lendstack.audit.AuditService;
import ng.lendstack.borrower.dto.ProfileRequest;
import ng.lendstack.borrower.dto.ProfileResponse;
import ng.lendstack.common.exception.ApiException;
import ng.lendstack.common.logging.PiiMasker;
import ng.lendstack.domain.BorrowerProfile;
import ng.lendstack.repository.BorrowerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BorrowerProfileService {

    private final BorrowerProfileRepository profileRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID userId) {
        return toResponse(profile(userId));
    }

    /**
     * Updates KYC. Changing the BVN resets bvnVerified — verification happens
     * during the credit check via the (stubbed) NIBSS lookup. Audit snapshots
     * only ever contain masked values.
     */
    @Transactional
    public ProfileResponse update(UUID userId, ProfileRequest request) {
        BorrowerProfile profile = profile(userId);
        Map<String, Object> before = snapshot(profile);

        if (request.bvn() != null && !request.bvn().equals(profile.getBvn())) {
            profile.setBvn(request.bvn());
            profile.setBvnVerified(false);
        }
        if (request.nin() != null) {
            profile.setNin(request.nin());
        }
        if (request.bankAccountNumber() != null) {
            profile.setBankAccountNumber(request.bankAccountNumber());
        }
        profile.setEmploymentStatus(request.employmentStatus());
        profile.setEmployerName(request.employerName());
        profile.setMonthlyIncome(request.monthlyIncome());
        profile.setBankName(request.bankName());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setAddress(request.address());
        profileRepository.save(profile);

        auditService.record("USER", userId.toString(), "KYC_UPDATED",
            before, snapshot(profile), null);
        return toResponse(profile);
    }

    private BorrowerProfile profile(UUID userId) {
        return profileRepository.findByUserId(userId)
            .orElseThrow(() -> ApiException.notFound("Borrower profile not found"));
    }

    /** Masked snapshot for the audit trail — never raw PII (NDPC). */
    private Map<String, Object> snapshot(BorrowerProfile p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("bvn", p.getBvn() == null ? null : PiiMasker.maskDigits(p.getBvn()));
        map.put("nin", p.getNin() == null ? null : PiiMasker.maskDigits(p.getNin()));
        map.put("bankAccount", p.getBankAccountNumber() == null
            ? null : PiiMasker.maskDigits(p.getBankAccountNumber()));
        map.put("employmentStatus", p.getEmploymentStatus());
        map.put("employerName", p.getEmployerName());
        map.put("monthlyIncome", p.getMonthlyIncome());
        map.put("bankName", p.getBankName());
        return map;
    }

    private ProfileResponse toResponse(BorrowerProfile p) {
        boolean kycComplete = p.getBvn() != null && p.getEmploymentStatus() != null
            && p.getMonthlyIncome() != null && p.getBankAccountNumber() != null;
        return new ProfileResponse(
            p.getBvn() == null ? null : PiiMasker.maskDigits(p.getBvn()),
            p.isBvnVerified(),
            p.getNin() == null ? null : PiiMasker.maskDigits(p.getNin()),
            p.getEmploymentStatus(),
            p.getEmployerName(),
            p.getMonthlyIncome(),
            p.getBankAccountNumber() == null ? null : PiiMasker.maskDigits(p.getBankAccountNumber()),
            p.getBankName(),
            p.getDateOfBirth(),
            p.getAddress(),
            kycComplete);
    }
}
