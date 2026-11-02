package com.thang.nihongo_user.expense.importing;

import java.util.ArrayList;
import java.util.List;

/** Minimal RFC 4180 reader: quoted fields, doubled quotes, line breaks inside quotes, ',' or ';' delimiter. */
public final class CsvRows {
    private CsvRows() {}

    public static List<List<String>> read(String text, int maxRows) {
        if (text.startsWith("﻿")) text = text.substring(1);
        char delimiter = detectDelimiter(text);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else quoted = false;
                } else field.append(c);
            } else if (c == '"' && field.toString().isBlank()) {
                field.setLength(0);
                quoted = true;
            } else if (c == delimiter) {
                row.add(field.toString()); field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString()); field.setLength(0);
                rows.add(row); row = new ArrayList<>();
                if (rows.size() > maxRows) throw new ImportFileException("File có quá nhiều dòng");
            } else field.append(c);
        }
        if (quoted) throw new ImportFileException("File CSV bị lỗi: thiếu dấu ngoặc kép đóng");
        if (field.length() > 0 || !row.isEmpty()) { row.add(field.toString()); rows.add(row); }
        return rows;
    }

    /** Picks the delimiter that appears more often (outside quotes) on the first line; ties favour ','. */
    static char detectDelimiter(String text) {
        int commas = 0, semicolons = 0;
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (!quoted && (c == '\n' || c == '\r')) break;
            else if (!quoted && c == ',') commas++;
            else if (!quoted && c == ';') semicolons++;
        }
        return semicolons > commas ? ';' : ',';
    }
}
