package ng.lendstack.repository;

import java.util.List;
import java.util.UUID;
import ng.lendstack.domain.Collateral;
import ng.lendstack.domain.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollateralRepository extends JpaRepository<Collateral, UUID> {

    List<Collateral> findByLoanId(UUID loanId);

    long countByLoanIdAndVerificationStatus(UUID loanId, VerificationStatus status);
}
