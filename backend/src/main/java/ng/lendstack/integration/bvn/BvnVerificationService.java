package ng.lendstack.integration.bvn;


public interface BvnVerificationService {

    BvnCheckResult verify(String bvn, String fullName);

    record BvnCheckResult(boolean verified, String detail) {
    }
}
