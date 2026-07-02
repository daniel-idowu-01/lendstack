package ng.lendstack.repository;

import java.util.List;
import java.util.UUID;
import ng.lendstack.domain.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {

    List<NotificationOutbox> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
