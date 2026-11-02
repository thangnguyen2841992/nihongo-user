package com.thang.nihongo_user.expense;

import com.thang.nihongo_user.expense.importing.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExpenseImportParserTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    @Test void readsSemicolonCsvWithBomQuotesAndVietnameseFormats() {
        String csv = "﻿Ngày;Số tiền;Danh mục;Ghi chú\r\n01/11/2026;150.000;Ăn uống;\"Phở; trà\"\r\n"
                + "2026-11-02;\"150,000 ₫\";Đi lại;Grab\n\n3/11/2026;abc;;\n31/02/2026;100;;\n5/11/2026;120000.00;lạ;\"x \"\"y\"\"\"\n";
        var result = ExpenseImportParser.parse(CsvRows.read(csv, 100), TODAY);
        assertEquals(3, result.rows().size());
        assertEquals(new ExpenseImportParser.Row(2, LocalDate.of(2026, 11, 1), 150_000, ExpenseCategory.AN_UONG, "Phở; trà"), result.rows().get(0));
        assertEquals(ExpenseCategory.DI_LAI, result.rows().get(1).category());
        assertEquals(120_000, result.rows().get(2).amount());
        assertEquals(ExpenseCategory.KHAC, result.rows().get(2).category());
        assertEquals("x \"y\"", result.rows().get(2).note());
        assertEquals(List.of(5, 6), result.errors().stream().map(ExpenseImportParser.LineError::line).toList());
    }

    @Test void acceptsEnglishHeaders() {
        var result = ExpenseImportParser.parse(CsvRows.read("Date,Amount,Category,Note\n2026-11-03,50000,hoc tap,book\n", 10), TODAY);
        assertEquals(ExpenseCategory.HOC_TAP, result.rows().get(0).category());
    }

    @Test void rejectsFilesWithoutRequiredColumnsOrTooManyRows() {
        assertThrows(ImportFileException.class, () -> ExpenseImportParser.parse(CsvRows.read("a,b\n1,2\n", 10), TODAY));
        assertThrows(ImportFileException.class, () -> ExpenseImportParser.parse(CsvRows.read("Ngày,Số tiền\n", 10), TODAY));
        StringBuilder big = new StringBuilder("Ngày,Số tiền\n");
        for (int i = 0; i <= ExpenseImportParser.MAX_ROWS; i++) big.append("1/11/2026,1000\n");
        assertThrows(ImportFileException.class, () -> ExpenseImportParser.parse(CsvRows.read(big.toString(), 5000), TODAY));
    }

    @Test void rejectsFarFutureDatesAndAmbiguousAmounts() {
        var result = ExpenseImportParser.parse(CsvRows.read("Ngày,Số tiền\n1/1/2030,5\n1/11/2026,\"1.000,000\"\n1/11/2026,0\n", 10), TODAY);
        assertTrue(result.rows().isEmpty());
        assertEquals(3, result.errors().size());
    }

    @Test void readsXlsxDatesAndNumbersIncludingTheTemplate() throws IOException {
        var rows = XlsxRows.read(new ByteArrayInputStream(ExpenseTemplate.build(TODAY)), 100);
        var result = ExpenseImportParser.parse(rows, TODAY);
        assertEquals(2, result.rows().size());
        assertEquals(TODAY, result.rows().get(0).spentOn());
        assertEquals(45_000, result.rows().get(0).amount());
        assertEquals(ExpenseCategory.AN_UONG, result.rows().get(0).category());

        try (var book = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = book.createSheet();
            var head = sheet.createRow(0); head.createCell(0).setCellValue("Ngày"); head.createCell(1).setCellValue("Số tiền");
            var row = sheet.createRow(1); row.createCell(0).setCellValue("02/11/2026"); row.createCell(1).setCellFormula("1000*2");
            book.setForceFormulaRecalculation(false);
            book.write(out);
            // The formula is never evaluated; without a cached result it is not 2000, so the row is rejected.
            var parsed = ExpenseImportParser.parse(XlsxRows.read(new ByteArrayInputStream(out.toByteArray()), 100), TODAY);
            assertEquals(1, parsed.errors().size());
        }
    }

    @Test void garbageIsNotAnExcelFile() {
        assertThrows(ImportFileException.class, () -> XlsxRows.read(new ByteArrayInputStream("not a zip".getBytes()), 100));
    }
}
