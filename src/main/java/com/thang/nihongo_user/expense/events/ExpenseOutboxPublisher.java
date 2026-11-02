package com.thang.nihongo_user.expense.events;

import com.thang.nihongo_user.expense.ExpenseOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/** Claim → send → mark, exactly like {@code WalletOutboxPublisher}, for topic {@code expense.events.v1}. */
@Service
public class ExpenseOutboxPublisher {
    private static final org.slf4j.Logger events = org.slf4j.LoggerFactory.getLogger("nihongo.kafka.expense");
    private static final long CLAIM_LEASE_SECONDS = 45;
    private final ExpenseOutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final PlatformTransactionManager transactionManager;
    @Value("${expense.events.topic:expense.events.v1}") private String topic;

    public ExpenseOutboxPublisher(ExpenseOutboxRepository outbox,
                                  @Qualifier("walletKafkaTemplate") KafkaTemplate<String, String> kafka,
                                  PlatformTransactionManager transactionManager) {
        this.outbox = outbox; this.kafka = kafka; this.transactionManager = transactionManager;
    }

    private record Claim(String id, String userId, String payload, int attempt) {}

    public boolean publishNext() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Claim claim = transaction.execute(status -> {
            var rows = outbox.lockDue(Instant.now(), PageRequest.of(0, 1));
            if (rows.isEmpty()) return null;
            var row = rows.get(0);
            int attempt = Math.addExact(row.getAttempts(), 1);
            row.setAttempts(attempt);
            row.setNextAttemptAt(Instant.now().plusSeconds(CLAIM_LEASE_SECONDS));
            row.setClaimedUntil(row.getNextAttemptAt());
            outbox.saveAndFlush(row);
            return new Claim(row.getId(), row.getUserId(), row.getPayload(), attempt);
        });
        if (claim == null) return false;
        try {
            var sent = kafka.send(topic, claim.userId(), claim.payload()).get(10, TimeUnit.SECONDS);
            var metadata = sent == null ? null : sent.getRecordMetadata();
            events.info("event=published topic={} eventId={} partition={} offset={} attempt={}", topic, claim.id(),
                    metadata == null ? -1 : metadata.partition(), metadata == null ? -1 : metadata.offset(), claim.attempt());
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            long backoffSeconds = Math.min(60, 1L << Math.min(claim.attempt(), 6));
            transaction.executeWithoutResult(status ->
                    outbox.retryAt(claim.id(), claim.attempt(), Instant.now().plusSeconds(backoffSeconds)));
            events.warn("event=retry topic={} eventId={} attempt={} failure={}", topic, claim.id(), claim.attempt(), e.getClass().getSimpleName());
            return true;
        }
        transaction.executeWithoutResult(status -> outbox.markPublished(claim.id(), claim.attempt(), Instant.now()));
        return true;
    }
}
