package com.thang.nihongo_user.expense;

import java.text.Normalizer;
import java.util.Locale;

/** Fixed spending categories. Codes are stored in the database; labels are shown to the user. */
public enum ExpenseCategory {
    AN_UONG("Ăn uống"),
    DI_LAI("Đi lại"),
    NHA_O("Nhà ở"),
    MUA_SAM("Mua sắm"),
    GIAI_TRI("Giải trí"),
    HOC_TAP("Học tập"),
    SUC_KHOE("Sức khỏe"),
    KHAC("Khác");

    private final String label;

    ExpenseCategory(String label) { this.label = label; }

    public String label() { return label; }

    /** Matches a code ("AN_UONG") or a Vietnamese label with or without accents ("ăn uống", "an uong"); unknown → KHAC. */
    public static ExpenseCategory parse(String text) {
        String key = fold(text);
        if (key.isEmpty()) return KHAC;
        for (ExpenseCategory category : values()) {
            if (key.equals(fold(category.name().replace('_', ' '))) || key.equals(fold(category.label))) return category;
        }
        return KHAC;
    }

    /** Lower-case, accents removed (đ → d), runs of spaces/underscores collapsed. */
    public static String fold(String text) {
        if (text == null) return "";
        String plain = Normalizer.normalize(text.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return plain.toLowerCase(Locale.ROOT).replaceAll("[\\s_]+", " ").trim();
    }
}
