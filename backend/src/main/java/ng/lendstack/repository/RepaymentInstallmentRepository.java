package ng.lendstack.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import ng.lendstack.domain.RepaymentInstallment;
import ng.lendstack.domain.enums.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentInstallmentRepository extends JpaRepository<RepaymentInstallment, UUID> {

    List<RepaymentInstallment> findByLoanIdOrderByInstallmentNumber(UUID loanId);

    List<RepaymentInstallment> findByLoanIdAndStatusInOrderByInstallmentNumber(
        UUID loanId, List<InstallmentStatus> statuses);

    List<RepaymentInstallment> findByStatusAndDueDateBefore(InstallmentStatus status, LocalDate cutoff);

    List<RepaymentInstallment> findByStatus(InstallmentStatus status);

    long countByLoanIdAndStatusIn(UUID loanId, List<InstallmentStatus> statuses);
}
