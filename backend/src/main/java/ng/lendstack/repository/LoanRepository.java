package ng.lendstack.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ng.lendstack.domain.Loan;
import ng.lendstack.domain.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoanRepository extends JpaRepository<Loan, UUID>, JpaSpecificationExecutor<Loan> {

    Optional<Loan> findByReference(String reference);

    Page<Loan> findByBorrowerId(UUID borrowerId, Pageable pageable);

    List<Loan> findByBorrowerIdAndStatusIn(UUID borrowerId, List<LoanStatus> statuses);

    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    List<Loan> findByStatusIn(List<LoanStatus> statuses);

    long countByBorrowerIdAndStatusIn(UUID borrowerId, List<LoanStatus> statuses);
}
