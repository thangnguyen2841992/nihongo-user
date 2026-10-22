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
    @Scheduled(fixedDelayString = "${wallet.events.publish-delay-ms:1000}")
    public void run() {
        try { for (int i = 0; i < 20 && !Thread.currentThread().isInterrupted(); i++) if (!publisher.publishNext()) break; }
        catch (RuntimeException e) { log.warn("Wallet outbox unavailable; retrying on next scheduled run"); }
    }
    // Published rows are only an audit trail; without this the table grows forever.
    @Scheduled(initialDelayString = "${wallet.events.cleanup-initial-delay-ms:600000}", fixedDelayString = "${wallet.events.cleanup-delay-ms:3600000}")
    public void cleanup() {
        var cutoff = java.time.Instant.now().minus(java.time.Duration.ofDays(Math.max(1, retentionDays)));
        try { for (int i = 0; i < 100 && outbox.removePublishedBefore(cutoff) == 500; i++) { } }
        catch (RuntimeException e) { log.warn("Wallet outbox cleanup unavailable; retrying on next scheduled run"); }
    }
}
