package com.thang.nihongo_user.expense;

import com.thang.nihongo_user.expense.ExpenseDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service @RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenses;
    private final ExpenseBudgetRepository budgets;
    private final BudgetAlertService alerts;
    Clock clock = Clock.systemUTC();

    @Transactional(readOnly = true)
    public MonthSummary summary(String userId, YearMonth month) {
        LocalDate from = month.atDay(1), to = month.atEndOfMonth();
        Optional<ExpenseBudget> own = budgets.findByUserIdAndMonth(userId, month.toString());
        Optional<ExpenseBudget> budget = own.isPresent() ? own
                : budgets.findFirstByUserIdAndMonthLessThanOrderByMonthDesc(userId, month.toString());
        long total = expenses.total(userId, from, to);
        Long limit = budget.map(ExpenseBudget::getLimitAmount).orElse(null);
        int warn = budget.map(ExpenseBudget::getWarnPercent).orElse(ExpenseBudget.DEFAULT_WARN_PERCENT);
        List<CategoryTotal> byCategory = new ArrayList<>();
        for (Object[] row : expenses.totalsByCategory(userId, from, to)) {
            ExpenseCategory category = (ExpenseCategory) row[0];
            byCategory.add(new CategoryTotal(category, category.label(), ((Number) row[1]).longValue()));
        }
        byCategory.sort(Comparator.comparingLong(CategoryTotal::total).reversed());
        List<ExpenseView> items = expenses.findByUserIdAndSpentOnBetweenOrderBySpentOnDescIdDesc(userId, from, to)
                .stream().map(ExpenseView::of).toList();
        return new MonthSummary(month.toString(), limit, warn, own.isEmpty() && limit != null, total,
                limit == null ? null : BudgetAlertRules.percent(limit, total),
                BudgetAlertRules.status(limit, warn, total), byCategory, items);
    }

    @Transactional
    public ExpenseSaved create(String userId, String email, ExpenseRequest request) {
        validateDate(request.spentOn());
        Expense expense = new Expense();
        expense.setUserId(userId);
        apply(expense, request);
        expenses.save(expense);
        YearMonth month = YearMonth.from(expense.getSpentOn());
        alerts.evaluate(userId, email, month);
        return new ExpenseSaved(ExpenseView.of(expense), summary(userId, month));
    }

    @Transactional
    public ExpenseSaved update(String userId, String email, Long id, ExpenseRequest request) {
        validateDate(request.spentOn());
        Expense expense = owned(userId, id);
        YearMonth before = YearMonth.from(expense.getSpentOn());
        apply(expense, request);
        expenses.save(expense);
        YearMonth after = YearMonth.from(expense.getSpentOn());
        // Lock months in a fixed order so two concurrent moves cannot deadlock.
        for (YearMonth month : new TreeSet<>(List.of(before, after))) alerts.evaluate(userId, email, month);
        return new ExpenseSaved(ExpenseView.of(expense), summary(userId, after));
    }

    @Transactional
    public MonthSummary delete(String userId, String email, Long id) {
        Expense expense = owned(userId, id);
        YearMonth month = YearMonth.from(expense.getSpentOn());
        expenses.delete(expense);
        expenses.flush();
        alerts.evaluate(userId, email, month);
        return summary(userId, month);
    }

    @Transactional
    public MonthSummary setBudget(String userId, String email, YearMonth month, BudgetRequest request) {
        ExpenseBudget budget = alerts.lockOrInherit(userId, month).orElseGet(() -> {
            ExpenseBudget created = new ExpenseBudget();
            created.setUserId(userId);
            created.setMonth(month.toString());
            return created;
        });
        budget.setLimitAmount(request.limitAmount());
        budget.setWarnPercent(request.warnPercent() == null ? ExpenseBudget.DEFAULT_WARN_PERCENT : request.warnPercent());
        budgets.saveAndFlush(budget);
        alerts.evaluate(userId, email, month);
        return summary(userId, month);
    }

    public List<CategoryOption> categories() {
        return Arrays.stream(ExpenseCategory.values()).map(c -> new CategoryOption(c, c.label())).toList();
    }

    private Expense owned(String userId, Long id) {
        // Another user's id answers exactly like a missing one.
        return expenses.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoản chi"));
    }

    private void validateDate(LocalDate date) {
        LocalDate latest = ExpenseMonths.today(clock).plusYears(1);
        if (date.getYear() < 2000 || date.isAfter(latest))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày chi không hợp lệ");
    }

    private static void apply(Expense expense, ExpenseRequest request) {
        expense.setAmount(request.amount());
        expense.setSpentOn(request.spentOn());
        expense.setCategory(request.category());
        String note = request.note() == null ? "" : request.note().replaceAll("\\s+", " ").trim();
        expense.setNote(note.isEmpty() ? null : note);
    }
}
