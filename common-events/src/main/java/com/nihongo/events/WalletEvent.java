package com.nihongo.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** JSON contract shared by the wallet producer and notification consumers. */
public record WalletEvent(int version, String eventId, String type, Long depositId,
        String userId, String email, BigDecimal amount, BigDecimal balance,
        String occurredAt, String reviewNote) {
    public WalletEvent {
        if (version != 1 || !Set.of("CREATED", "APPROVED", "REJECTED").contains(type == null ? "" : type))
            throw new IllegalArgumentException("Unsupported wallet event");
        UUID.fromString(eventId);
        Instant.parse(occurredAt);
        if (depositId == null || depositId <= 0 || userId == null || userId.isBlank()
                || email == null || email.isBlank() || amount == null || amount.signum() <= 0
                || balance == null || balance.signum() < 0)
            throw new IllegalArgumentException("Incomplete wallet event");
    }

    // Existing v1 payloads without reviewNote remain readable.
    public WalletEvent(int version, String eventId, String type, Long depositId,
            String userId, String email, BigDecimal amount, BigDecimal balance, String occurredAt) {
        this(version, eventId, type, depositId, userId, email, amount, balance, occurredAt, null);
    }
}
