package com.thang.nihongo_user.expense;

import com.nihongo.events.BudgetAlertEvent;
import com.thang.nihongo_user.expense.events.ExpenseEventRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

/** Re-checks a month's thresholds after every change, in the same transaction as the change. */
@Service @RequiredArgsConstructor
public class BudgetAlertService {
    private final ExpenseBudgetRepository budgets;
    private final ExpenseRepository expenses;
    private final ExpenseEventRecorder recorder;
    Clock clock = Clock.systemUTC();

    @Transactional(propagation = Propagation.MANDATORY)
    public void evaluate(String userId, String email, YearMonth month) {
        Optional<ExpenseBudget> found = lockOrInherit(userId, month);
        if (found.isEmpty()) return;
        ExpenseBudget budget = found.get();
        long total = expenses.total(userId, month.atDay(1), month.atEndOfMonth());
        boolean notify = !month.isBefore(ExpenseMonths.current(clock));
        var decision = BudgetAlertRules.decide(budget.getLimitAmount(), budget.getWarnPercent(), total,
                budget.getWarnedAt() != null, budget.getExceededAt() != null, notify);
        Instant now = clock.instant();
        budget.setWarnedAt(decision.warned() ? (budget.getWarnedAt() == null ? now : budget.getWarnedAt()) : null);
        budget.setExceededAt(decision.exceeded() ? (budget.getExceededAt() == null ? now : budget.getExceededAt()) : null);
        budgets.save(budget);
        if (decision.alert() == null) return;
        recorder.record(new BudgetAlertEvent(1, UUID.randomUUID().toString(), decision.alert().name(), userId,
                validEmail(email), month.toString(), budget.getLimitAmount(), total,
                BudgetAlertRules.percent(budget.getLimitAmount(), total), budget.getWarnPercent(), now.toString()));
    }

    /** Locks this month's budget row; a month without one copies the latest earlier limit (flags start clear). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ExpenseBudget> lockOrInherit(String userId, YearMonth month) {
        Optional<ExpenseBudget> own = budgets.lock(userId, month.toString());
        if (own.isPresent()) return own;
        return budgets.findFirstByUserIdAndMonthLessThanOrderByMonthDesc(userId, month.toString()).map(previous -> {
            ExpenseBudget copy = new ExpenseBudget();
            copy.setUserId(userId);
            copy.setMonth(month.toString());
            copy.setLimitAmount(previous.getLimitAmount());
            copy.setWarnPercent(previous.getWarnPercent());
            return budgets.saveAndFlush(copy);
        });
    }

    private static String validEmail(String email) {
        return email != null && email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") ? email : null;
    }
}
