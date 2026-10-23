package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.WalletOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
public interface WalletOutboxRepository extends JpaRepository<WalletOutbox, String> {
    long countByPublishedAtIsNull();
    // Bounded batches keep each delete transaction short on the shared database.
    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(value = "DELETE FROM wallet_outbox WHERE published_at IS NOT NULL AND published_at < :cutoff LIMIT 500", nativeQuery = true)
    int removePublishedBefore(@Param("cutoff") Instant cutoff);
    org.springframework.data.domain.Page<WalletOutbox> findByPublishedAtIsNullOrderByCreatedAtAscIdAsc(Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from WalletOutbox e where e.id=:id") java.util.Optional<WalletOutbox> lock(String id);
    // Claims move nextAttemptAt to the lease end, so this is also when an in-flight claim could be retried.
    @Query("select min(e.nextAttemptAt) from WalletOutbox e where e.publishedAt is null")
    Instant nextAttemptAt();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from WalletOutbox e where e.publishedAt is null and e.nextAttemptAt <= :now order by e.createdAt, e.id")
    List<WalletOutbox> lockDue(@Param("now") Instant now, Pageable page);

    @Modifying
    @Query("update WalletOutbox e set e.publishedAt = :publishedAt,e.claimedUntil=null where e.id = :id and e.attempts = :attempt and e.publishedAt is null")
    int markPublished(@Param("id") String id, @Param("attempt") int attempt,
                      @Param("publishedAt") Instant publishedAt);

    @Modifying
    @Query("update WalletOutbox e set e.nextAttemptAt = :nextAttemptAt,e.claimedUntil=null where e.id = :id and e.attempts = :attempt and e.publishedAt is null")
    int retryAt(@Param("id") String id, @Param("attempt") int attempt,
                @Param("nextAttemptAt") Instant nextAttemptAt);
}
