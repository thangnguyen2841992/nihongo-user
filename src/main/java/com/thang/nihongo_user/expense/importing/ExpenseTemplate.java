package com.thang.nihongo_user.expense.importing;

import com.thang.nihongo_user.expense.ExpenseCategory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;

/** The downloadable sample file "mau-chi-tieu.xlsx". */
public final class ExpenseTemplate {
    private ExpenseTemplate() {}

    public static byte[] build(LocalDate today) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle date = workbook.createCellStyle();
            date.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0"));
            CellStyle header = workbook.createCellStyle();
            Font bold = workbook.createFont(); bold.setBold(true); header.setFont(bold);

            Sheet sheet = workbook.createSheet("Chi tiêu");
            String[] titles = {"Ngày", "Số tiền", "Danh mục", "Ghi chú"};
            Row head = sheet.createRow(0);
            for (int i = 0; i < titles.length; i++) { Cell c = head.createCell(i); c.setCellValue(titles[i]); c.setCellStyle(header); }
            Object[][] samples = {{today, 45000, "Ăn uống", "Cơm trưa"}, {today, 30000, "Đi lại", "Xe buýt"}};
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                Cell d = row.createCell(0); d.setCellValue((LocalDate) samples[r][0]); d.setCellStyle(date);
                Cell m = row.createCell(1); m.setCellValue((Integer) samples[r][1]); m.setCellStyle(money);
                row.createCell(2).setCellValue((String) samples[r][2]);
                row.createCell(3).setCellValue((String) samples[r][3]);
            }
            sheet.setColumnWidth(0, 14 * 256); sheet.setColumnWidth(1, 14 * 256);
            sheet.setColumnWidth(2, 16 * 256); sheet.setColumnWidth(3, 40 * 256);

            Sheet help = workbook.createSheet("Danh mục");
            help.createRow(0).createCell(0).setCellValue("Danh mục hợp lệ (để trống hoặc ghi khác sẽ thành \"Khác\")");
            int r = 1;
            for (ExpenseCategory category : ExpenseCategory.values()) help.createRow(r++).createCell(0).setCellValue(category.label());
            help.setColumnWidth(0, 60 * 256);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
