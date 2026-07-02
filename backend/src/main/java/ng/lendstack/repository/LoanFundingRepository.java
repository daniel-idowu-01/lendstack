package ng.lendstack.repository;

import java.util.List;
import java.util.UUID;
import ng.lendstack.domain.LoanFunding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanFundingRepository extends JpaRepository<LoanFunding, UUID> {

    List<LoanFunding> findByLoanId(UUID loanId);

    Page<LoanFunding> findByLenderId(UUID lenderId, Pageable pageable);
}
