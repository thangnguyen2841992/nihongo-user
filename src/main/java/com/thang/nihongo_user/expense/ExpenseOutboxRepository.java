package com.thang.nihongo_user.expense;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface ExpenseOutboxRepository extends JpaRepository<ExpenseOutbox, String> {
    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query(value = "DELETE FROM expense_outbox WHERE published_at IS NOT NULL AND published_at < :cutoff LIMIT 500", nativeQuery = true)
    int removePublishedBefore(@Param("cutoff") Instant cutoff);

    @Query("select min(e.nextAttemptAt) from ExpenseOutbox e where e.publishedAt is null")
    Instant nextAttemptAt();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ExpenseOutbox e where e.publishedAt is null and e.nextAttemptAt <= :now order by e.createdAt, e.id")
    List<ExpenseOutbox> lockDue(@Param("now") Instant now, Pageable page);

    @Modifying
    @Query("update ExpenseOutbox e set e.publishedAt = :publishedAt, e.claimedUntil = null where e.id = :id and e.attempts = :attempt and e.publishedAt is null")
    int markPublished(@Param("id") String id, @Param("attempt") int attempt, @Param("publishedAt") Instant publishedAt);

    @Modifying
    @Query("update ExpenseOutbox e set e.nextAttemptAt = :nextAttemptAt, e.claimedUntil = null where e.id = :id and e.attempts = :attempt and e.publishedAt is null")
    int retryAt(@Param("id") String id, @Param("attempt") int attempt, @Param("nextAttemptAt") Instant nextAttemptAt);
}
