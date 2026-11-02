package com.thang.nihongo_user.expense.importing;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Reads the first sheet of an .xlsx file as text cells. Formulas are never evaluated; their cached value is used. */
public final class XlsxRows {
    private XlsxRows() {}

    public static List<List<String>> read(InputStream input, int maxRows) {
        // POI's ZipSecureFile defaults reject zip bombs (inflate ratio and entry size limits).
        try (Workbook workbook = new XSSFWorkbook(input)) {
            if (workbook.getNumberOfSheets() == 0) throw new ImportFileException("File Excel không có sheet nào");
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > maxRows) throw new ImportFileException("File có quá nhiều dòng");
            List<List<String>> rows = new ArrayList<>();
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                List<String> cells = new ArrayList<>();
                if (row != null) {
                    int last = Math.min(row.getLastCellNum(), 50);
                    for (int c = 0; c < last; c++) cells.add(text(row.getCell(c)));
                }
                rows.add(cells);
            }
            return rows;
        } catch (ImportFileException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ImportFileException("Không đọc được file Excel. Hãy lưu lại dưới dạng .xlsx hoặc dùng file mẫu.", e);
        }
    }

    private static String text(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
