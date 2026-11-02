package com.thang.nihongo_user.expense.importing;

import com.thang.nihongo_user.expense.ExpenseCategory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns spreadsheet rows (strings; Excel dates already as yyyy-MM-dd) into validated expense rows.
 * The first non-empty row is the header. Line numbers are 1-based rows of the original file.
 */
public final class ExpenseImportParser {
    public static final int MAX_ROWS = 1000;
    public static final long MAX_AMOUNT = 10_000_000_000L;
    public static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);

    public record Row(int line, LocalDate spentOn, long amount, ExpenseCategory category, String note) {}
    public record LineError(int line, String message) {}
    public record Result(List<Row> rows, List<LineError> errors) {}

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT));

    private ExpenseImportParser() {}

    public static Result parse(List<List<String>> table, LocalDate today) {
        int headerIndex = -1;
        for (int i = 0; i < table.size(); i++) if (!blank(table.get(i))) { headerIndex = i; break; }
        if (headerIndex < 0) throw new ImportFileException("File không có dữ liệu");
        Columns columns = Columns.of(table.get(headerIndex));

        List<Row> rows = new ArrayList<>();
        List<LineError> errors = new ArrayList<>();
        int dataRows = 0;
        LocalDate latest = today.plusYears(1);
        for (int i = headerIndex + 1; i < table.size(); i++) {
            List<String> cells = table.get(i);
            if (blank(cells)) continue;
            if (++dataRows > MAX_ROWS) throw new ImportFileException("File có quá " + MAX_ROWS + " dòng dữ liệu");
            int line = i + 1;
            String dateText = cell(cells, columns.date), amountText = cell(cells, columns.amount);
            LocalDate date = parseDate(dateText);
            if (date == null) { errors.add(new LineError(line, dateText.isBlank() ? "Thiếu ngày" : "Ngày không hợp lệ: " + dateText)); continue; }
            if (date.isBefore(MIN_DATE) || date.isAfter(latest)) { errors.add(new LineError(line, "Ngày nằm ngoài khoảng cho phép: " + dateText)); continue; }
            Long amount = parseAmount(amountText);
            if (amount == null) { errors.add(new LineError(line, amountText.isBlank() ? "Thiếu số tiền" : "Số tiền không hợp lệ: " + amountText)); continue; }
            if (amount < 1 || amount > MAX_AMOUNT) { errors.add(new LineError(line, "Số tiền phải từ 1 đến 10.000.000.000 ₫")); continue; }
            String note = cell(cells, columns.note).replaceAll("\\s+", " ").trim();
            if (note.length() > 255) note = note.substring(0, 255);
            rows.add(new Row(line, date, amount, ExpenseCategory.parse(cell(cells, columns.category)), note));
        }
        if (dataRows == 0) throw new ImportFileException("File chỉ có dòng tiêu đề, chưa có khoản chi nào");
        return new Result(rows, errors);
    }

    static LocalDate parseDate(String text) {
        String value = text.trim();
        if (value.length() > 10 && value.matches("\\d{4}-\\d{2}-\\d{2}[T ].*")) value = value.substring(0, 10);
        for (DateTimeFormatter format : DATE_FORMATS) {
            try { return LocalDate.parse(value, format); } catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    /** Accepts 150000, 150.000, 150,000, "150 000 ₫", 150000đ, 150000 VND, 150000.00. Only whole VND. */
    static Long parseAmount(String text) {
        String value = text.replaceAll("(?i)vn[dđ]|₫|đ|\\s| ", "");
        if (value.startsWith("+")) value = value.substring(1);
        value = value.replaceAll("[.,]0{1,2}$", "");
        if (value.matches("\\d{1,3}([.,]\\d{3})+")) {
            char separator = value.charAt(value.length() - 4);
            if (value.indexOf(separator == '.' ? ',' : '.') >= 0) return null; // mixed separators
            value = value.replace(String.valueOf(separator), "");
        }
        if (!value.matches("\\d{1,15}")) return null;
        return Long.parseLong(value);
    }

    private static boolean blank(List<String> cells) {
        return cells.stream().allMatch(c -> c == null || c.isBlank());
    }

    private static String cell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size() || cells.get(index) == null) return "";
        return cells.get(index).trim();
    }

    private record Columns(int date, int amount, int category, int note) {
        static Columns of(List<String> header) {
            int date = -1, amount = -1, category = -1, note = -1;
            for (int i = 0; i < header.size(); i++) {
                String h = ExpenseCategory.fold(header.get(i));
                if (date < 0 && (h.startsWith("ngay") || h.startsWith("date"))) date = i;
                else if (amount < 0 && (h.contains("so tien") || h.startsWith("tien") || h.startsWith("amount") || h.startsWith("chi phi"))) amount = i;
                else if (category < 0 && (h.startsWith("danh muc") || h.startsWith("category") || h.startsWith("loai"))) category = i;
                else if (note < 0 && (h.startsWith("ghi chu") || h.startsWith("note") || h.startsWith("mo ta") || h.startsWith("noi dung") || h.startsWith("description"))) note = i;
            }
            if (date < 0 || amount < 0)
                throw new ImportFileException("Không tìm thấy cột \"Ngày\" và \"Số tiền\" ở dòng tiêu đề. Hãy dùng file mẫu.");
            return new Columns(date, amount, category, note);
        }
    }
}
