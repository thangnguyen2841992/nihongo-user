package com.thang.nihongo_user.expense;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.format.DateTimeParseException;

/** Month handling in Vietnam time, where "this month" for the user is decided. */
public final class ExpenseMonths {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private ExpenseMonths() {}

    public static YearMonth current(Clock clock) { return YearMonth.now(clock.withZone(ZONE)); }

    public static LocalDate today(Clock clock) { return LocalDate.now(clock.withZone(ZONE)); }

    public static YearMonth parse(String month, Clock clock) {
        if (month == null || month.isBlank()) return current(clock);
        try {
            YearMonth value = YearMonth.parse(month.trim());
            if (value.getYear() < 2000 || value.getYear() > 2100) throw new DateTimeParseException("range", month, 0);
            return value;
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tháng không hợp lệ, dùng dạng yyyy-MM");
        }
    }
}
