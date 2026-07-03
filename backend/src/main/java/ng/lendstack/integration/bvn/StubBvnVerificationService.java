package ng.lendstack.integration.bvn;

import lombok.extern.slf4j.Slf4j;
import ng.lendstack.common.logging.PiiMasker;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class StubBvnVerificationService implements BvnVerificationService {

    @Override
    public BvnCheckResult verify(String bvn, String fullName) {
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
