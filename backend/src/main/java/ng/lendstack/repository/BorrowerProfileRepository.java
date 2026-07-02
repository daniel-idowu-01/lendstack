package ng.lendstack.repository;

import java.util.Optional;
import java.util.UUID;
import ng.lendstack.domain.BorrowerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerProfileRepository extends JpaRepository<BorrowerProfile, UUID> {

    Optional<BorrowerProfile> findByUserId(UUID userId);
}
