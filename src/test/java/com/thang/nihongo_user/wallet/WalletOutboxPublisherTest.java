package com.thang.nihongo_user.wallet;

import com.thang.nihongo_user.model.WalletOutbox;
import com.thang.nihongo_user.repository.WalletOutboxRepository;
import com.thang.nihongo_user.wallet.events.WalletOutboxPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wallet_outbox_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = WalletOutboxPublisherTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WalletOutboxPublisherTest {
    @TestConfiguration
    @EntityScan(basePackageClasses = WalletOutbox.class)
    @EnableJpaRepositories(basePackageClasses = WalletOutboxRepository.class)
    @Import(WalletOutboxPublisher.class)
    static class Config {
        @Bean KafkaTemplate<String, String> walletKafkaTemplate() { return mock(KafkaTemplate.class); }
    }

    @Autowired WalletOutboxRepository outbox;
    @Autowired WalletOutboxPublisher publisher;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach void resetState() {
        outbox.deleteAll();
        reset(kafka);
    }

    private WalletOutbox pending() {
        WalletOutbox row = new WalletOutbox();
        row.setId(UUID.randomUUID().toString());
        row.setUserId("user-1");
        row.setPayload("{\"eventId\":\"example\"}");
        row.setCreatedAt(Instant.now());
        row.setNextAttemptAt(Instant.now());
        return outbox.saveAndFlush(row);
    }

    @Test void releasesDatabaseLockBeforeSendingToKafka() {
        WalletOutbox row = pending();
        when(kafka.send(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertFalse(publisher.publishNext(), "The claimed event must not be due while Kafka is pending");
            return CompletableFuture.completedFuture(null);
        });

        assertTrue(publisher.publishNext());
        WalletOutbox published = outbox.findById(row.getId()).orElseThrow();
        assertNotNull(published.getPublishedAt());
        assertEquals(1, published.getAttempts());
        verify(kafka, times(1)).send(anyString(), anyString(), anyString());
    }

    @Test void retriesAfterKafkaFailureAndRejectsAnObsoleteClaim() {
        WalletOutbox row = pending();
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertTrue(publisher.publishNext());
        WalletOutbox waiting = outbox.findById(row.getId()).orElseThrow();
        assertNull(waiting.getPublishedAt());
        assertEquals(1, waiting.getAttempts());
        assertTrue(waiting.getNextAttemptAt().isAfter(Instant.now()));

        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            WalletOutbox due = outbox.findById(row.getId()).orElseThrow();
            due.setNextAttemptAt(Instant.now().minusSeconds(1));
            outbox.save(due);
        });
        assertTrue(publisher.publishNext());
        assertNotNull(outbox.findById(row.getId()).orElseThrow().getPublishedAt());

        new TransactionTemplate(transactions).executeWithoutResult(status ->
                assertEquals(0, outbox.retryAt(row.getId(), 1, Instant.now())));
    }
}
