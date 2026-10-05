package com.thang.nihongo_user.wallet.events;
import com.thang.nihongo_user.repository.WalletOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service @RequiredArgsConstructor @Slf4j
public class WalletOutboxPublisher {
    private static final long CLAIM_LEASE_SECONDS = 45;
    private final WalletOutboxRepository outbox;
    private final KafkaTemplate<String, String> walletKafkaTemplate;
    private final PlatformTransactionManager transactionManager;
    @Value("${wallet.events.topic:wallet.events.v1}") private String topic;

    private record Claim(String id, String userId, String payload, int attempt) {}

    public boolean publishNext() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Claim claim = transaction.execute(status -> {
            var rows = outbox.lockDue(Instant.now(), PageRequest.of(0, 1));
            if (rows.isEmpty()) return null;
            var row = rows.get(0);
            // The attempt number is also a claim generation: a late worker cannot finish a newer claim.
            int attempt = Math.addExact(row.getAttempts(), 1);
            row.setAttempts(attempt);
            row.setNextAttemptAt(Instant.now().plusSeconds(CLAIM_LEASE_SECONDS));
            outbox.saveAndFlush(row);
            return new Claim(row.getId(), row.getUserId(), row.getPayload(), attempt);
        });
        if (claim == null) return false;

        // No database transaction or row lock is held while waiting for Kafka.
        try {
            walletKafkaTemplate.send(topic, claim.userId(), claim.payload()).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            long backoffSeconds = Math.min(60, 1L << Math.min(claim.attempt(), 6));
            transaction.executeWithoutResult(status ->
                    outbox.retryAt(claim.id(), claim.attempt(), Instant.now().plusSeconds(backoffSeconds)));
            log.warn("Wallet event {} remains pending, attempt {}", claim.id(), claim.attempt());
            return true;
        }
        // A failed DB update leaves the lease to expire, then this event is retried at least once.
        transaction.executeWithoutResult(status ->
                outbox.markPublished(claim.id(), claim.attempt(), Instant.now()));
        return true;
    }
}
