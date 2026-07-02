package ng.lendstack.repository;

import java.util.Optional;
import java.util.UUID;
import ng.lendstack.domain.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByReference(String reference);

    Page<PaymentTransaction> findByLoanId(UUID loanId, Pageable pageable);
}
