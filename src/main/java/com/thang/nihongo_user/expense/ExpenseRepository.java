package com.thang.nihongo_user.expense;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Optional<Expense> findByIdAndUserId(Long id, String userId);

    List<Expense> findByUserIdAndSpentOnBetweenOrderBySpentOnDescIdDesc(String userId, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.userId = :userId and e.spentOn between :from and :to")
    long total(@Param("userId") String userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select e.category, sum(e.amount) from Expense e where e.userId = :userId and e.spentOn between :from and :to group by e.category")
    List<Object[]> totalsByCategory(@Param("userId") String userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
