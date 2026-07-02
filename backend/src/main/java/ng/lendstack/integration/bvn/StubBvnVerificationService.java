package ng.lendstack.integration.bvn;

import lombok.extern.slf4j.Slf4j;
import ng.lendstack.common.logging.PiiMasker;
import org.springframework.stereotype.Service;

/**
 * ==== STUB — REPLACE WITH REAL NIBSS INTEGRATION ====
 * Deterministic fake so the full lifecycle can be exercised end-to-end:
 * <ul>
 *   <li>BVN starting with '0' → verification fails ("no NIBSS record") — use
 *       this in demos to show the failure path</li>
 *   <li>any other well-formed 11-digit BVN → verified</li>
 * </ul>
 * The real implementation must call the NIBSS BVN validation service (igree/
 * BVN-iValidate) or a licensed aggregator, match name + date of birth, and be
 * wired in by replacing this bean. The interface is already shaped for that.
 */
@Slf4j
@Service
public class StubBvnVerificationService implements BvnVerificationService {

    @Override
    public BvnCheckResult verify(String bvn, String fullName) {
        // Logged masked; PiiMaskingConverter would also catch a raw 11-digit slip.
        log.info("[STUB] BVN verification for {} — replace with NIBSS integration",
            PiiMasker.maskDigits(bvn));
        if (bvn == null || !bvn.matches("\\d{11}")) {
            return new BvnCheckResult(false, "BVN is not a valid 11-digit number");
        }
        if (bvn.startsWith("0")) {
            return new BvnCheckResult(false, "No matching record found (stub rule: BVN starts with 0)");
        }
        return new BvnCheckResult(true, "BVN record matched (stubbed NIBSS response)");
    }
}
