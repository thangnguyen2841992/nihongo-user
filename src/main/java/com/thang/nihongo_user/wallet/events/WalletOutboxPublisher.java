package com.thang.nihongo_user.wallet.events;
import com.thang.nihongo_user.model.WalletOutbox;
import com.thang.nihongo_user.repository.WalletOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service @RequiredArgsConstructor @Slf4j
public class WalletOutboxPublisher {
    private final WalletOutboxRepository outbox;
    private final KafkaTemplate<String, String> walletKafkaTemplate;
    @Value("${wallet.events.topic:wallet.events.v1}") private String topic;
    @Transactional(timeout = 30)
    public boolean publishNext() {
        var rows = outbox.lockDue(Instant.now(), PageRequest.of(0, 1));
        if (rows.isEmpty()) return false;
        WalletOutbox row = rows.get(0);
        try {
            walletKafkaTemplate.send(topic, row.getUserId(), row.getPayload()).get(10, TimeUnit.SECONDS);
            row.setPublishedAt(Instant.now());
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            row.setAttempts(row.getAttempts() + 1);
            row.setNextAttemptAt(Instant.now().plusSeconds(Math.min(60, 1L << Math.min(row.getAttempts(), 6))));
            log.warn("Wallet event {} remains pending, attempt {}", row.getId(), row.getAttempts());
        }
        outbox.save(row);
        return true;
    }
}
