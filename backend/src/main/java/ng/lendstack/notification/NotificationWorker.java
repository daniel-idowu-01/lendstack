package ng.lendstack.notification;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ng.lendstack.domain.NotificationOutbox;
import ng.lendstack.repository.NotificationOutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private final NotificationOutboxRepository outboxRepository;

    @Scheduled(fixedDelayString = "PT30S")
    @Transactional
    public void drainOutbox() {
        List<NotificationOutbox> batch = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        for (NotificationOutbox notification : batch) {
            try {
                deliver(notification);
                notification.setStatus("SENT");
                notification.setSentAt(Instant.now());
            } catch (Exception e) {
                log.error("Notification {} failed to send: {}", notification.getId(), e.getMessage());
                notification.setStatus("FAILED");
            }
            outboxRepository.save(notification);
        }
    }

    private void deliver(NotificationOutbox n) {
        log.info("[STUB EMAIL] to={} type={} subject={}", n.getRecipientEmail(), n.getType(),
            n.getSubject());
    }
}
