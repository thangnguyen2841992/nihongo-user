package com.thang.nihongo_user.expense;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of {@code /api/nihongo-user/expenses}. */
public final class ExpenseDtos {
    private ExpenseDtos() {}

    public record ExpenseRequest(
            @NotNull(message = "Vui lòng nhập số tiền") @Min(value = 1, message = "Số tiền phải lớn hơn 0")
            @Max(value = 10_000_000_000L, message = "Số tiền tối đa 10.000.000.000 ₫") Long amount,
            @NotNull(message = "Vui lòng chọn ngày") LocalDate spentOn,
            @NotNull(message = "Vui lòng chọn danh mục") ExpenseCategory category,
            @Size(max = 255, message = "Ghi chú tối đa 255 ký tự") String note) {}

    public record BudgetRequest(
            @NotNull(message = "Vui lòng nhập hạn mức") @Min(value = 1000, message = "Hạn mức tối thiểu 1.000 ₫")
            @Max(value = 1_000_000_000_000L, message = "Hạn mức quá lớn") Long limitAmount,
            @Min(value = 50, message = "Ngưỡng cảnh báo từ 50% đến 99%") @Max(value = 99, message = "Ngưỡng cảnh báo từ 50% đến 99%") Integer warnPercent) {}

    public record ExpenseView(Long id, long amount, LocalDate spentOn, ExpenseCategory category, String categoryLabel,
                              String note, Expense.Source source) {
        static ExpenseView of(Expense e) {
            return new ExpenseView(e.getId(), e.getAmount(), e.getSpentOn(), e.getCategory(), e.getCategory().label(), e.getNote(), e.getSource());
        }
    }

    public record CategoryTotal(ExpenseCategory category, String label, long total) {}

    /** status: NO_BUDGET, OK, WARNING or EXCEEDED. limitAmount and percent are null without a budget. */
    public record MonthSummary(String month, Long limitAmount, int warnPercent, boolean inheritedLimit, long total,
                               Integer percent, String status, List<CategoryTotal> byCategory, List<ExpenseView> expenses) {}

    public record ExpenseSaved(ExpenseView expense, MonthSummary summary) {}

    public record CategoryOption(ExpenseCategory code, String label) {}

    public record PreviewRow(int line, LocalDate spentOn, long amount, ExpenseCategory category, String categoryLabel,
                             String note, boolean duplicate) {}

    public record ImportError(int line, String message) {}

    public record ImportPreviewResponse(String previewId, List<PreviewRow> rows, List<ImportError> errors) {}

    public record ImportConfirmRequest(@NotBlank(message = "Thiếu mã import") String previewId,
                                       @NotNull(message = "Chưa chọn khoản chi nào") @Size(max = 1000) List<Integer> lines) {}

    public record ImportConfirmResponse(int imported, List<MonthSummary> summaries) {}
}
