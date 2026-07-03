package ng.lendstack.notification;

import lombok.RequiredArgsConstructor;
import ng.lendstack.domain.NotificationOutbox;
import ng.lendstack.repository.NotificationOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationOutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String recipientEmail, String recipientName, String subject, String body,
                        String type, String relatedEntityType, String relatedEntityId) {
        outboxRepository.save(NotificationOutbox.builder()
            .recipientEmail(recipientEmail)
            .recipientName(recipientName)
            .subject(subject)
            .body(body)
            .type(type)
            .relatedEntityType(relatedEntityType)
            .relatedEntityId(relatedEntityId)
            .build());
    }
}
