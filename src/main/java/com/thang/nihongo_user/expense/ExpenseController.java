package com.thang.nihongo_user.expense;

import com.thang.nihongo_user.expense.ExpenseDtos.*;
import com.thang.nihongo_user.expense.importing.ExpenseTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.Clock;
import java.util.List;

/** Personal monthly spending. The owner is always the JWT subject; the alert email is always the JWT email claim. */
@RestController
@RequestMapping("/api/nihongo-user/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
public class ExpenseController {
    private final ExpenseService expenses;
    private final ExpenseImportService imports;
    private final Clock clock = Clock.systemUTC();

    @GetMapping
    public MonthSummary month(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String month) {
        return expenses.summary(jwt.getSubject(), ExpenseMonths.parse(month, clock));
    }

    @PostMapping
    public ExpenseSaved create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ExpenseRequest request) {
        return expenses.create(jwt.getSubject(), email(jwt), request);
    }

    @PutMapping("/{id}")
    public ExpenseSaved update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return expenses.update(jwt.getSubject(), email(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public MonthSummary delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return expenses.delete(jwt.getSubject(), email(jwt), id);
    }

    @PutMapping("/budget")
    public MonthSummary budget(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String month,
                               @Valid @RequestBody BudgetRequest request) {
        return expenses.setBudget(jwt.getSubject(), email(jwt), ExpenseMonths.parse(month, clock), request);
    }

    @GetMapping("/categories")
    public List<CategoryOption> categories() { return expenses.categories(); }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportPreviewResponse preview(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
        return imports.preview(jwt.getSubject(), file);
    }

    @PostMapping("/import/confirm")
    public ImportConfirmResponse confirm(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ImportConfirmRequest request) {
        return imports.confirm(jwt.getSubject(), email(jwt), request);
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("mau-chi-tieu.xlsx").build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(ExpenseTemplate.build(ExpenseMonths.today(clock)));
    }

    private static String email(Jwt jwt) { return jwt.getClaimAsString("email"); }
}
