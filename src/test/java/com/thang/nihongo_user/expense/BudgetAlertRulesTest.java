package com.thang.nihongo_user.expense;

import com.thang.nihongo_user.expense.BudgetAlertRules.Alert;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BudgetAlertRulesTest {
    @Test void belowWarningSendsNothing() {
        var d = BudgetAlertRules.decide(1_000_000, 80, 799_999, false, false, true);
        assertNull(d.alert()); assertFalse(d.warned()); assertFalse(d.exceeded());
    }
    @Test void reachingWarningSendsWarningOnce() {
        var first = BudgetAlertRules.decide(1_000_000, 80, 800_000, false, false, true);
        assertEquals(Alert.WARNING, first.alert());
        var again = BudgetAlertRules.decide(1_000_000, 80, 900_000, first.warned(), first.exceeded(), true);
        assertNull(again.alert());
    }
    @Test void jumpingPastTheLimitSendsOnlyExceeded() {
        var d = BudgetAlertRules.decide(1_000_000, 80, 1_500_000, false, false, true);
        assertEquals(Alert.EXCEEDED, d.alert()); assertTrue(d.warned()); assertTrue(d.exceeded());
    }
    @Test void exactlyTheLimitCountsAsExceeded() {
        assertEquals(Alert.EXCEEDED, BudgetAlertRules.decide(1_000_000, 80, 1_000_000, true, false, true).alert());
    }
    @Test void droppingBelowReArmsTheThreshold() {
        var down = BudgetAlertRules.decide(1_000_000, 80, 850_000, true, true, true);
        assertNull(down.alert()); assertTrue(down.warned()); assertFalse(down.exceeded());
        assertEquals(Alert.EXCEEDED, BudgetAlertRules.decide(1_000_000, 80, 1_000_000, down.warned(), down.exceeded(), true).alert());
        var far = BudgetAlertRules.decide(1_000_000, 80, 10, true, true, true);
        assertFalse(far.warned()); assertFalse(far.exceeded());
    }
    @Test void pastMonthsUpdateFlagsWithoutSending() {
        var d = BudgetAlertRules.decide(1_000_000, 80, 2_000_000, false, false, false);
        assertNull(d.alert()); assertTrue(d.exceeded());
    }
    @Test void statusAndPercent() {
        assertEquals("NO_BUDGET", BudgetAlertRules.status(null, 80, 5));
        assertEquals("OK", BudgetAlertRules.status(1000L, 80, 799));
        assertEquals("WARNING", BudgetAlertRules.status(1000L, 80, 800));
        assertEquals("EXCEEDED", BudgetAlertRules.status(1000L, 80, 1000));
        assertEquals(79, BudgetAlertRules.percent(1000, 799));
        assertEquals(150, BudgetAlertRules.percent(1000, 1500));
    }
}
