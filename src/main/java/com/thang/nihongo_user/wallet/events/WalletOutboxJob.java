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
    @Scheduled(fixedDelayString = "${wallet.events.publish-delay-ms:1000}")
    public void run() {
        try { for (int i = 0; i < 20 && !Thread.currentThread().isInterrupted(); i++) if (!publisher.publishNext()) break; }
        catch (RuntimeException e) { log.warn("Wallet outbox unavailable; retrying on next scheduled run"); }
    }
}
