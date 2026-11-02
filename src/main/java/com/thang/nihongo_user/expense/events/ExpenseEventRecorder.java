package com.thang.nihongo_user.expense.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nihongo.events.BudgetAlertEvent;
import com.thang.nihongo_user.expense.ExpenseOutbox;
import com.thang.nihongo_user.expense.ExpenseOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

/** Writes budget alerts to the outbox inside the caller's transaction, like {@code WalletEventRecorder}. */
@Service @RequiredArgsConstructor
public class ExpenseEventRecorder {
    private final ExpenseOutboxRepository outbox;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;

    /** Published inside the recording transaction; {@link ExpenseOutboxJob} wakes up after it commits. */
    public record Queued() {}

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(BudgetAlertEvent event) {
        ExpenseOutbox row = new ExpenseOutbox();
        Instant now = Instant.now();
        row.setId(event.eventId()); row.setUserId(event.userId()); row.setCreatedAt(now); row.setNextAttemptAt(now);
        try { row.setPayload(mapper.writeValueAsString(event)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot record budget alert", e); }
        outbox.save(row);
        events.publishEvent(new Queued());
    }
}
