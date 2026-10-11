package com.thang.nihongo_user.wallet.realtime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nihongo.events.WalletEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component @RequiredArgsConstructor
public class WalletRealtimeConsumer {
    private static final org.slf4j.Logger events = org.slf4j.LoggerFactory.getLogger("nihongo.kafka.wallet-realtime");
    private static final org.slf4j.Logger sockets = org.slf4j.LoggerFactory.getLogger("nihongo.socket.wallet");
    private final ObjectMapper mapper;
    private final SimpMessagingTemplate messaging;
    @KafkaListener(topics = "${wallet.events.topic:wallet.events.v1}",
        groupId = "wallet-realtime-${wallet.instance-id:${random.uuid}}", containerFactory = "walletRealtimeFactory",
        autoStartup = "${wallet.events.enabled:true}")
    public void receive(String json) throws Exception {
        WalletEvent event = mapper.readValue(json, WalletEvent.class);
        // Send an invalidation, not a balance delta or email address. Duplicates are harmless.
        var notice = Map.of("eventId", event.eventId(), "depositId", event.depositId(), "type", event.type());
        messaging.convertAndSendToUser(event.userId(), "/queue/wallet", notice);
        messaging.convertAndSend("/topic/wallet-admin", notice);
        events.info("event=consumed eventId={} depositId={} type={}", event.eventId(), event.depositId(), event.type());
        sockets.debug("event=published eventId={} depositId={} type={}", event.eventId(), event.depositId(), event.type());
    }
}
