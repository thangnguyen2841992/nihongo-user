package com.thang.nihongo_user.expense.events;

import com.thang.nihongo_user.expense.ExpenseOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.Duration;
import java.time.Instant;

@Component @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name = "expense.events.enabled", havingValue = "true", matchIfMissing = true)
public class ExpenseOutboxJob {
    private final ExpenseOutboxPublisher publisher;
    private final ExpenseOutboxRepository outbox;
    @Value("${expense.events.retention-days:30}") private int retentionDays = 30;
    @Value("${expense.events.max-idle-ms:30000}") private long maxIdleMs = 30_000;
    private volatile long idleUntil;
    private volatile boolean woken;

    @TransactionalEventListener(fallbackExecution = true)
    public void queued(ExpenseEventRecorder.Queued event) { woken = true; }

    @Scheduled(fixedDelayString = "${expense.events.publish-delay-ms:1000}")
    public void run() {
        if (!woken && System.currentTimeMillis() < idleUntil) return;
        woken = false;
        try {
            for (int i = 0; i < 20 && !Thread.currentThread().isInterrupted(); i++) {
                if (!publisher.publishNext()) { idleUntil = System.currentTimeMillis() + idleMillis(); return; }
            }
        } catch (RuntimeException e) { log.warn("Expense outbox unavailable; retrying on next scheduled run"); }
    }

    private long idleMillis() {
        long limit = Math.max(0, maxIdleMs);
        Instant next = outbox.nextAttemptAt();
        return next == null ? limit : Math.max(0, Math.min(limit, Duration.between(Instant.now(), next).toMillis()));
    }

    @Scheduled(initialDelayString = "${expense.events.cleanup-initial-delay-ms:600000}", fixedDelayString = "${expense.events.cleanup-delay-ms:3600000}")
    public void cleanup() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(Math.max(1, retentionDays)));
        try { for (int i = 0; i < 100 && outbox.removePublishedBefore(cutoff) == 500; i++) { } }
        catch (RuntimeException e) { log.warn("Expense outbox cleanup unavailable; retrying on next scheduled run"); }
    }
}
