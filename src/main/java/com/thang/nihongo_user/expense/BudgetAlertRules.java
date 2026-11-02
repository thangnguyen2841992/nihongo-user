package com.thang.nihongo_user.expense;

/**
 * Pure threshold logic for monthly budget alerts. Each threshold fires once; it re-arms only when the total
 * drops back below it. A single change that jumps straight past 100% fires only EXCEEDED.
 */
public final class BudgetAlertRules {
    public enum Alert { WARNING, EXCEEDED }

    /** New flag state plus the alert to send, or {@code alert == null} when nothing should be sent. */
    public record Decision(boolean warned, boolean exceeded, Alert alert) {}

    private BudgetAlertRules() {}

    public static Decision decide(long limit, int warnPercent, long total, boolean warned, boolean exceeded, boolean notify) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        boolean overWarn = reachedWarning(limit, warnPercent, total);
        boolean overLimit = total >= limit;
        if (!overLimit) exceeded = false;
        if (!overWarn) warned = false;
        Alert alert = null;
        if (overLimit && !exceeded) {
            exceeded = true;
            warned = true;
            alert = Alert.EXCEEDED;
        } else if (overWarn && !warned) {
            warned = true;
            alert = Alert.WARNING;
        }
        return new Decision(warned, exceeded, notify ? alert : null);
    }

    public static boolean reachedWarning(long limit, int warnPercent, long total) {
        // Compare without division so 79.99% never rounds up to 80%.
        return Math.multiplyExact(total, 100L) >= Math.multiplyExact(limit, (long) warnPercent);
    }

    /** Whole percent, rounded down, for display. */
    public static int percent(long limit, long total) {
        if (limit <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, Math.multiplyExact(total, 100L) / limit);
    }

    public static String status(Long limit, int warnPercent, long total) {
        if (limit == null || limit <= 0) return "NO_BUDGET";
        if (total >= limit) return "EXCEEDED";
        return reachedWarning(limit, warnPercent, total) ? "WARNING" : "OK";
    }
}
