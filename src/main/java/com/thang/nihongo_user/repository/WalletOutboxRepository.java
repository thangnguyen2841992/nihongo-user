package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.WalletOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
public interface WalletOutboxRepository extends JpaRepository<WalletOutbox, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from WalletOutbox e where e.publishedAt is null and e.nextAttemptAt <= :now order by e.createdAt, e.id")
    List<WalletOutbox> lockDue(@Param("now") Instant now, Pageable page);

    @Modifying
    @Query("update WalletOutbox e set e.publishedAt = :publishedAt where e.id = :id and e.attempts = :attempt and e.publishedAt is null")
    int markPublished(@Param("id") String id, @Param("attempt") int attempt,
                      @Param("publishedAt") Instant publishedAt);

    @Modifying
    @Query("update WalletOutbox e set e.nextAttemptAt = :nextAttemptAt where e.id = :id and e.attempts = :attempt and e.publishedAt is null")
    int retryAt(@Param("id") String id, @Param("attempt") int attempt,
                @Param("nextAttemptAt") Instant nextAttemptAt);
}
