package ng.lendstack.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ng.lendstack.domain.CreditAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditAssessmentRepository extends JpaRepository<CreditAssessment, UUID> {

    List<CreditAssessment> findByLoanIdOrderByCreatedAtDesc(UUID loanId);

    Optional<CreditAssessment> findFirstByLoanIdOrderByCreatedAtDesc(UUID loanId);
}
