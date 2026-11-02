package com.thang.nihongo_user.expense;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.expense.ExpenseDtos.*;
import com.thang.nihongo_user.expense.importing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class ExpenseImportService {
    public static final long MAX_FILE_BYTES = 2L * 1024 * 1024;
    static final Duration PREVIEW_TTL = Duration.ofMinutes(15);

    private final ExpenseRepository expenses;
    private final ExpenseImportPreviewRepository previews;
    private final BudgetAlertService alerts;
    private final ExpenseService service;
    private final ObjectMapper mapper;
    Clock clock = Clock.systemUTC();

    /** What is stored between preview and confirm. Dates as text so any ObjectMapper can read them back. */
    record StoredRow(int line, String spentOn, long amount, ExpenseCategory category, String note) {}

    @Transactional
    public ImportPreviewResponse preview(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn file");
        if (file.getSize() > MAX_FILE_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File tối đa 2 MB");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        ExpenseImportParser.Result result;
        try (InputStream input = file.getInputStream()) {
            List<List<String>> table;
            int maxLines = ExpenseImportParser.MAX_ROWS * 2 + 50; // blank rows are allowed, but not unbounded
            if (name.endsWith(".xlsx")) table = XlsxRows.read(input, maxLines);
            else if (name.endsWith(".csv")) table = CsvRows.read(decode(input.readAllBytes()), maxLines);
            else throw new ImportFileException("Chỉ nhận file .xlsx hoặc .csv");
            result = ExpenseImportParser.parse(table, ExpenseMonths.today(clock));
        } catch (ImportFileException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không đọc được file");
        }

        Set<String> seen = existingKeys(userId, result.rows());
        List<PreviewRow> rows = new ArrayList<>();
        List<StoredRow> stored = new ArrayList<>();
        for (ExpenseImportParser.Row row : result.rows()) {
            String note = row.note().isEmpty() ? null : row.note();
            boolean duplicate = !seen.add(key(row.spentOn(), row.amount(), note));
            rows.add(new PreviewRow(row.line(), row.spentOn(), row.amount(), row.category(), row.category().label(), note, duplicate));
            stored.add(new StoredRow(row.line(), row.spentOn().toString(), row.amount(), row.category(), note));
        }
        List<ImportError> errors = result.errors().stream().map(e -> new ImportError(e.line(), e.message())).toList();

        ExpenseImportPreview preview = new ExpenseImportPreview();
        preview.setId(UUID.randomUUID().toString());
        preview.setUserId(userId);
        Instant now = clock.instant();
        preview.setCreatedAt(now);
        preview.setExpiresAt(now.plus(PREVIEW_TTL));
        try { preview.setPayload(mapper.writeValueAsString(stored)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Cannot store import preview", e); }
        previews.save(preview);
        return new ImportPreviewResponse(preview.getId(), rows, errors);
    }

    @Transactional
    public ImportConfirmResponse confirm(String userId, String email, ImportConfirmRequest request) {
        ExpenseImportPreview preview = previews.lock(request.previewId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lần import này, hãy tải file lên lại"));
        if (preview.getConfirmedAt() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Lần import này đã được lưu");
        if (preview.getExpiresAt().isBefore(clock.instant()))
            throw new ResponseStatusException(HttpStatus.GONE, "Bản xem trước đã hết hạn (15 phút), hãy tải file lên lại");
        Set<Integer> chosen = new HashSet<>(request.lines());
        List<StoredRow> rows;
        try { rows = mapper.readValue(preview.getPayload(), new TypeReference<List<StoredRow>>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Corrupt import preview", e); }
        List<Expense> saved = new ArrayList<>();
        for (StoredRow row : rows) {
            if (!chosen.contains(row.line())) continue;
            Expense expense = new Expense();
            expense.setUserId(userId);
            expense.setAmount(row.amount());
            expense.setSpentOn(LocalDate.parse(row.spentOn()));
            expense.setCategory(row.category());
            expense.setNote(row.note());
            expense.setSource(Expense.Source.IMPORT);
            expense.setImportBatchId(preview.getId());
            saved.add(expense);
        }
        if (saved.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn khoản chi nào");
        expenses.saveAll(saved);
        preview.setConfirmedAt(clock.instant());
        // Keep the row a while so a repeated confirm answers 409; the payload is no longer needed.
        preview.setExpiresAt(clock.instant().plus(Duration.ofDays(1)));
        preview.setPayload("[]");
        previews.save(preview);
        expenses.flush();
        SortedSet<YearMonth> months = new TreeSet<>();
        saved.forEach(e -> months.add(YearMonth.from(e.getSpentOn())));
        for (YearMonth month : months) alerts.evaluate(userId, email, month);
        return new ImportConfirmResponse(saved.size(), months.stream().map(m -> service.summary(userId, m)).toList());
    }

    @Scheduled(initialDelayString = "${expense.import.cleanup-initial-delay-ms:300000}", fixedDelayString = "${expense.import.cleanup-delay-ms:3600000}")
    public void cleanup() {
        try { previews.removeExpiredBefore(clock.instant()); }
        catch (RuntimeException e) { log.warn("Expense import preview cleanup unavailable; retrying on next run"); }
    }

    private Set<String> existingKeys(String userId, List<ExpenseImportParser.Row> rows) {
        Set<String> keys = new HashSet<>();
        if (rows.isEmpty()) return keys;
        LocalDate from = rows.stream().map(ExpenseImportParser.Row::spentOn).min(LocalDate::compareTo).orElseThrow();
        LocalDate to = rows.stream().map(ExpenseImportParser.Row::spentOn).max(LocalDate::compareTo).orElseThrow();
        for (Expense e : expenses.findByUserIdAndSpentOnBetweenOrderBySpentOnDescIdDesc(userId, from, to))
            keys.add(key(e.getSpentOn(), e.getAmount(), e.getNote()));
        return keys;
    }

    private static String key(LocalDate date, long amount, String note) {
        return date + "|" + amount + "|" + (note == null ? "" : ExpenseCategory.fold(note));
    }

    /** UTF-8 (with or without BOM); files saved by Excel on Windows as "CSV" are often Windows-1258/1252. */
    static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            throw new ImportFileException("File CSV cần lưu với mã hoá UTF-8 (Excel: Lưu thành → \"CSV UTF-8\"), hoặc dùng file .xlsx");
        }
    }
}
