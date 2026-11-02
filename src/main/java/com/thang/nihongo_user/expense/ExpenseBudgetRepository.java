package com.thang.nihongo_user.expense;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ExpenseBudgetRepository extends JpaRepository<ExpenseBudget, Long> {
    Optional<ExpenseBudget> findByUserIdAndMonth(String userId, String month);

    /** Serialises alert decisions for one user and month, so a threshold is never announced twice. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ExpenseBudget b where b.userId = :userId and b.month = :month")
    Optional<ExpenseBudget> lock(@Param("userId") String userId, @Param("month") String month);

    /** The most recent month before {@code month} that has a limit; a new month inherits it. */
    Optional<ExpenseBudget> findFirstByUserIdAndMonthLessThanOrderByMonthDesc(String userId, String month);
}
