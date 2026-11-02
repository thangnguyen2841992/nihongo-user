package com.thang.nihongo_user.expense.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nihongo.events.BudgetAlertEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

/** Pushes budget alerts to the user's open tabs over the existing wallet STOMP endpoint. */
@Component @RequiredArgsConstructor
public class ExpenseRealtimeConsumer {
    private static final org.slf4j.Logger events = org.slf4j.LoggerFactory.getLogger("nihongo.kafka.expense-realtime");
    private final ObjectMapper mapper;
    private final SimpMessagingTemplate messaging;

    @KafkaListener(topics = "${expense.events.topic:expense.events.v1}",
            groupId = "expense-realtime-${wallet.instance-id:${random.uuid}}", containerFactory = "walletRealtimeFactory",
            autoStartup = "${expense.events.enabled:true}")
    public void receive(String json) throws Exception {
        BudgetAlertEvent event = mapper.readValue(json, BudgetAlertEvent.class);
        // No email address in the notice; amounts let the toast say something useful without a refetch.
        var notice = Map.of("eventId", event.eventId(), "type", event.type(), "month", event.month(),
                "percent", event.percent(), "totalSpent", event.totalSpent(), "limitAmount", event.limitAmount());
        messaging.convertAndSendToUser(event.userId(), "/queue/expense", notice);
        events.info("event=consumed eventId={} type={} month={}", event.eventId(), event.type(), event.month());
    }
}
