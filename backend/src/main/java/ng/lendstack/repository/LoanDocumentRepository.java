package ng.lendstack.repository;

import java.util.List;
import java.util.UUID;
import ng.lendstack.domain.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, UUID> {

    List<LoanDocument> findByLoanId(UUID loanId);

    List<LoanDocument> findByCollateralId(UUID collateralId);
}
