package com.thang.nihongo_user.wallet.events;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nihongo.events.WalletEvent;
import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.repository.WalletOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class WalletEventRecorder {
    private final WalletOutboxRepository outbox;
    private final ObjectMapper mapper;
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String type, WalletDeposit deposit, UserWallet wallet) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        WalletEvent event = new WalletEvent(1, id, type, deposit.getId(), deposit.getUserId(),
            wallet.getNotificationEmail(), deposit.getAmount(), wallet.getBalance(), now.toString());
        WalletOutbox row = new WalletOutbox();
        row.setId(id); row.setUserId(event.userId()); row.setCreatedAt(now); row.setNextAttemptAt(now);
        try { row.setPayload(mapper.writeValueAsString(event)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot record wallet event", e); }
        outbox.save(row);
    }
}
