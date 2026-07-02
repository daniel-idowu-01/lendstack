package ng.lendstack.integration.bvn;

/**
 * BVN verification against the national register. The production implementation
 * will call NIBSS (or an aggregator such as VerifyMe/Prembly); the current
 * implementation is {@link StubBvnVerificationService}.
 */
public interface BvnVerificationService {

    BvnCheckResult verify(String bvn, String fullName);

    record BvnCheckResult(boolean verified, String detail) {
    }
}
