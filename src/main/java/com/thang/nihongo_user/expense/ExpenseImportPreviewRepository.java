package com.thang.nihongo_user.expense;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

public interface ExpenseImportPreviewRepository extends JpaRepository<ExpenseImportPreview, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ExpenseImportPreview p where p.id = :id and p.userId = :userId")
    Optional<ExpenseImportPreview> lock(@Param("id") String id, @Param("userId") String userId);

    /** Confirmed previews are kept a day so a repeated confirm still answers 409 instead of 404. */
    @Transactional @Modifying
    @Query("delete from ExpenseImportPreview p where p.expiresAt < :cutoff")
    int removeExpiredBefore(@Param("cutoff") Instant cutoff);
}
