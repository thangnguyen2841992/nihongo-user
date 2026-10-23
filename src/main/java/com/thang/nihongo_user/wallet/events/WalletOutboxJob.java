package com.thang.nihongo_user.wallet.events;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name = "wallet.events.enabled", havingValue = "true", matchIfMissing = true)
public class WalletOutboxJob {
    private final WalletOutboxPublisher publisher;
    private final com.thang.nihongo_user.repository.WalletOutboxRepository outbox;
    @org.springframework.beans.factory.annotation.Value("${wallet.events.retention-days:30}") private int retentionDays = 30;
    // Empty queue: do not ask the database again until the earliest retry, at most this long. New events from
    // this instance wake the job right after commit, so delivery latency stays about one tick.
    @org.springframework.beans.factory.annotation.Value("${wallet.events.max-idle-ms:30000}") private long maxIdleMs = 30_000;
    private volatile long idleUntil;
    private volatile boolean woken;

    @org.springframework.transaction.event.TransactionalEventListener(fallbackExecution = true)
    public void queued(WalletEventRecorder.Queued event) { woken = true; }

    @Scheduled(fixedDelayString = "${wallet.events.publish-delay-ms:1000}")
    public void run() {
        if (!woken && System.currentTimeMillis() < idleUntil) return;
        woken = false;
        try {
            for (int i = 0; i < 20 && !Thread.currentThread().isInterrupted(); i++) {
                if (!publisher.publishNext()) { idleUntil = System.currentTimeMillis() + idleMillis(); return; }
            }
        }
        catch (RuntimeException e) { log.warn("Wallet outbox unavailable; retrying on next scheduled run"); }
    }

    private long idleMillis() {
        long limit = Math.max(0, maxIdleMs);
        var next = outbox.nextAttemptAt();
        return next == null ? limit : Math.max(0, Math.min(limit, java.time.Duration.between(java.time.Instant.now(), next).toMillis()));
    }
    // Published rows are only an audit trail; without this the table grows forever.
    @Scheduled(initialDelayString = "${wallet.events.cleanup-initial-delay-ms:600000}", fixedDelayString = "${wallet.events.cleanup-delay-ms:3600000}")
    public void cleanup() {
        var cutoff = java.time.Instant.now().minus(java.time.Duration.ofDays(Math.max(1, retentionDays)));
        try { for (int i = 0; i < 100 && outbox.removePublishedBefore(cutoff) == 500; i++) { } }
        catch (RuntimeException e) { log.warn("Wallet outbox cleanup unavailable; retrying on next scheduled run"); }
    }
}
