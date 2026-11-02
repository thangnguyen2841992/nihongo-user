package com.thang.nihongo_user.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nihongo.events.BudgetAlertEvent;
import com.thang.nihongo_user.expense.ExpenseDtos.*;
import com.thang.nihongo_user.expense.events.ExpenseEventRecorder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:expense_test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false", "spring.cloud.discovery.enabled=false", "eureka.client.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ExpensePersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExpensePersistenceTest {
    @TestConfiguration
    @EntityScan(basePackageClasses = Expense.class)
    @EnableJpaRepositories(basePackageClasses = ExpenseRepository.class)
    @Import({ExpenseService.class, BudgetAlertService.class, ExpenseImportService.class, ExpenseEventRecorder.class})
    static class Config {
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
    }

    static final Clock NOV_2026 = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneOffset.UTC);
    @Autowired ExpenseService service;
    @Autowired BudgetAlertService alerts;
    @Autowired ExpenseImportService imports;
    @Autowired ExpenseRepository expenses;
    @Autowired ExpenseBudgetRepository budgets;
    @Autowired ExpenseOutboxRepository outbox;
    @Autowired ExpenseImportPreviewRepository previews;
    @Autowired ObjectMapper mapper;

    @BeforeEach void reset() {
        outbox.deleteAll(); expenses.deleteAll(); budgets.deleteAll(); previews.deleteAll();
        service.clock = NOV_2026; alerts.clock = NOV_2026; imports.clock = NOV_2026;
    }

    ExpenseRequest spend(long amount, String day) { return new ExpenseRequest(amount, LocalDate.parse(day), ExpenseCategory.AN_UONG, "  cơm   trưa "); }

    List<BudgetAlertEvent> sent() {
        return outbox.findAll().stream().map(row -> {
            try { return mapper.readValue(row.getPayload(), BudgetAlertEvent.class); } catch (Exception e) { throw new AssertionError(e); }
        }).sorted(Comparator.comparing((BudgetAlertEvent e) -> e.type().equals("WARNING") ? 0 : 1)).toList(); // the clock is fixed
    }

    @Test void warningThenExceededAreEachSentOnce() {
        service.setBudget("u1", "u1@example.com", YearMonth.of(2026, 11), new BudgetRequest(1_000_000L, null));
        var saved = service.create("u1", "u1@example.com", spend(700_000, "2026-11-02"));
        assertEquals("cơm trưa", saved.expense().note());
        assertEquals("OK", saved.summary().status());
        assertTrue(sent().isEmpty());

        service.create("u1", "u1@example.com", spend(150_000, "2026-11-03"));
        service.create("u1", "u1@example.com", spend(10_000, "2026-11-04"));
        assertEquals(List.of("WARNING"), sent().stream().map(BudgetAlertEvent::type).toList());
        assertEquals(85, sent().get(0).percent());
        assertEquals("u1@example.com", sent().get(0).email());

        var over = service.create("u1", "u1@example.com", spend(200_000, "2026-11-05"));
        assertEquals("EXCEEDED", over.summary().status());
        service.create("u1", "u1@example.com", spend(1, "2026-11-06"));
        assertEquals(2, sent().size());
        assertEquals("EXCEEDED", sent().get(1).type());
    }

    @Test void deletingBelowTheLimitReArmsTheAlert() {
        service.setBudget("u1", null, YearMonth.of(2026, 11), new BudgetRequest(100_000L, 90));
        var big = service.create("u1", null, spend(120_000, "2026-11-02"));
        assertEquals(1, sent().size());
        assertNull(sent().get(0).email());
        service.delete("u1", null, big.expense().id());
        service.create("u1", null, spend(100_000, "2026-11-02"));
        assertEquals(2, sent().size());
    }

    @Test void raisingOrLoweringTheLimitIsEvaluatedToo() {
        service.create("u1", null, spend(500_000, "2026-11-02"));
        var summary = service.setBudget("u1", null, YearMonth.of(2026, 11), new BudgetRequest(400_000L, 80));
        assertEquals("EXCEEDED", summary.status());
        assertEquals(1, sent().size());
        assertEquals("OK", service.setBudget("u1", null, YearMonth.of(2026, 11), new BudgetRequest(5_000_000L, 80)).status());
    }

    @Test void newMonthInheritsTheLatestLimitAndPastMonthsDoNotNotify() {
        service.setBudget("u1", null, YearMonth.of(2026, 9), new BudgetRequest(300_000L, 70));
        var october = service.summary("u1", YearMonth.of(2026, 10));
        assertEquals(300_000L, october.limitAmount());
        assertTrue(october.inheritedLimit());
        service.create("u1", null, spend(400_000, "2026-10-10"));
        assertTrue(sent().isEmpty(), "October is in the past on 15 Nov");
        assertEquals("EXCEEDED", service.summary("u1", YearMonth.of(2026, 10)).status());
        service.create("u1", null, spend(250_000, "2026-11-10"));
        assertEquals(List.of("WARNING"), sent().stream().map(BudgetAlertEvent::type).toList());
        assertEquals(70, budgets.findByUserIdAndMonth("u1", "2026-11").orElseThrow().getWarnPercent());
    }

    @Test void movingAnExpenseToAnotherMonthUpdatesBoth() {
        service.setBudget("u1", null, YearMonth.of(2026, 11), new BudgetRequest(100_000L, 80));
        var saved = service.create("u1", null, spend(100_000, "2026-11-02"));
        var moved = service.update("u1", null, saved.expense().id(), spend(100_000, "2026-12-02"));
        assertEquals("2026-12", moved.summary().month());
        assertEquals(0, service.summary("u1", YearMonth.of(2026, 11)).total());
    }

    @Test void usersCannotTouchEachOthersExpenses() {
        var mine = service.create("u1", null, spend(1000, "2026-11-02"));
        var e1 = assertThrows(ResponseStatusException.class, () -> service.update("u2", null, mine.expense().id(), spend(5, "2026-11-02")));
        assertEquals(404, e1.getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.delete("u2", null, mine.expense().id())).getStatusCode().value());
        assertEquals(0, service.summary("u2", YearMonth.of(2026, 11)).total());
        assertEquals(1000, service.summary("u1", YearMonth.of(2026, 11)).total());
    }

    @Test void importPreviewFlagsDuplicatesAndConfirmIsOneShot() {
        service.create("u1", null, new ExpenseRequest(45_000L, LocalDate.of(2026, 11, 2), ExpenseCategory.AN_UONG, "Cơm trưa"));
        service.setBudget("u1", "u1@example.com", YearMonth.of(2026, 11), new BudgetRequest(100_000L, 80));
        String csv = "Ngày,Số tiền,Danh mục,Ghi chú\n02/11/2026,45000,Ăn uống,cơm trưa\n03/11/2026,40000,Đi lại,Taxi\n04/11/2026,x,,\n05/10/2026,1000,,\n";
        var file = new MockMultipartFile("file", "chi-tieu.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        var preview = imports.preview("u1", file);
        assertEquals(3, preview.rows().size());
        assertTrue(preview.rows().get(0).duplicate());
        assertFalse(preview.rows().get(1).duplicate());
        assertEquals(1, preview.errors().size());

        var other = assertThrows(ResponseStatusException.class, () -> imports.confirm("u2", null, new ImportConfirmRequest(preview.previewId(), List.of(3))));
        assertEquals(404, other.getStatusCode().value());

        var done = imports.confirm("u1", "u1@example.com", new ImportConfirmRequest(preview.previewId(), List.of(3, 5)));
        assertEquals(2, done.imported());
        assertEquals(List.of("2026-10", "2026-11"), done.summaries().stream().map(MonthSummary::month).toList());
        assertEquals(85_000, done.summaries().get(1).total());
        assertEquals(List.of("WARNING"), sent().stream().map(BudgetAlertEvent::type).toList());
        var again = assertThrows(ResponseStatusException.class, () -> imports.confirm("u1", null, new ImportConfirmRequest(preview.previewId(), List.of(3))));
        assertEquals(409, again.getStatusCode().value());
    }

    @Test void expiredPreviewCannotBeConfirmed() {
        var file = new MockMultipartFile("file", "a.csv", "text/csv", "Ngày,Số tiền\n1/11/2026,1000\n".getBytes(StandardCharsets.UTF_8));
        var preview = imports.preview("u1", file);
        imports.clock = Clock.offset(NOV_2026, Duration.ofMinutes(16));
        assertEquals(410, assertThrows(ResponseStatusException.class,
            () -> imports.confirm("u1", null, new ImportConfirmRequest(preview.previewId(), List.of(2)))).getStatusCode().value());
    }

    @Test void rejectsUnsupportedOrNonUtf8Files() {
        var pdf = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> imports.preview("u1", pdf)).getStatusCode().value());
        var latin = new MockMultipartFile("file", "a.csv", "text/csv", new byte[]{'N', 'g', (byte) 0xE0, 'y', '\n'});
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> imports.preview("u1", latin)).getStatusCode().value());
    }
}
