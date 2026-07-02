package ng.lendstack.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ng.lendstack.domain.Guarantor;
import ng.lendstack.domain.enums.GuarantorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuarantorRepository extends JpaRepository<Guarantor, UUID> {

    List<Guarantor> findByLoanId(UUID loanId);

    Optional<Guarantor> findByResponseToken(String responseToken);

    List<Guarantor> findByStatusAndExpiresAtBefore(GuarantorStatus status, Instant cutoff);

    long countByLoanIdAndStatus(UUID loanId, GuarantorStatus status);
}
