package com.thang.nihongo_user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SupportRealtimePublisher {
    private final ApplicationEventPublisher events;

    public record Notice(String eventId, Long conversationId, Long messageId, String type, String userId) {}

    public void changed(Long conversationId, Long messageId, String type, String userId) {
        events.publishEvent(new Notice(UUID.randomUUID().toString(), conversationId, messageId, type, userId));
    }

    @Component
    @RequiredArgsConstructor
    public static class Listener {
        private final SimpMessagingTemplate messaging;

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
        public void publish(Notice notice) {
            messaging.convertAndSendToUser(notice.userId(), "/queue/support", notice);
            messaging.convertAndSend("/topic/support-staff", notice);
        }
    }
}
