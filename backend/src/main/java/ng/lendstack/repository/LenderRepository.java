package ng.lendstack.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ng.lendstack.domain.Lender;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LenderRepository extends JpaRepository<Lender, UUID> {

    Optional<Lender> findByEmailIgnoreCase(String email);

    List<Lender> findByActiveTrue();
}
