package com.thang.nihongo_user.expense;

import com.nihongo.security.CommonSecurityConfig;
import com.thang.nihongo_user.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.YearMonth;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ExpenseController.class, properties = {
        "security.session-validation.enabled=false", "security.origin-check.enabled=false"
})
@Import({SecurityConfig.class, CommonSecurityConfig.class, ExpenseExceptionHandler.class})
class ExpenseControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ExpenseService expenses;
    @MockitoBean ExpenseImportService imports;
    @MockitoBean JwtDecoder decoder;

    static org.springframework.test.web.servlet.request.RequestPostProcessor owner() {
        return jwt().jwt(j -> j.subject("owner").claim("email", "owner@example.com")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/nihongo-user/expenses")).andExpect(status().isUnauthorized());
        verifyNoInteractions(expenses, imports);
    }

    @Test void ownerAndEmailComeFromTheToken() throws Exception {
        mvc.perform(get("/api/nihongo-user/expenses").param("month", "2026-11").param("userId", "someone-else").with(owner()))
            .andExpect(status().isOk());
        verify(expenses).summary("owner", YearMonth.of(2026, 11));
        mvc.perform(post("/api/nihongo-user/expenses").with(owner()).contentType("application/json")
                .content("{\"amount\":45000,\"spentOn\":\"2026-11-02\",\"category\":\"AN_UONG\",\"note\":\"x\",\"email\":\"evil@example.com\"}"))
            .andExpect(status().isOk());
        verify(expenses).create(eq("owner"), eq("owner@example.com"), any());
    }

    @Test void invalidInputIsA400WithAVietnameseMessage() throws Exception {
        mvc.perform(post("/api/nihongo-user/expenses").with(owner()).contentType("application/json")
                .content("{\"amount\":0,\"spentOn\":\"2026-11-02\",\"category\":\"AN_UONG\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Số tiền phải lớn hơn 0"));
        mvc.perform(post("/api/nihongo-user/expenses").with(owner()).contentType("application/json")
                .content("{\"amount\":1,\"spentOn\":\"2026-11-02\",\"category\":\"NOPE\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/nihongo-user/expenses").param("month", "11/2026").with(owner()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Tháng không hợp lệ, dùng dạng yyyy-MM"));
        mvc.perform(put("/api/nihongo-user/expenses/budget").with(owner()).contentType("application/json")
                .content("{\"limitAmount\":1000000,\"warnPercent\":100}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(expenses);
    }

    @Test void templateIsAnXlsxDownload() throws Exception {
        mvc.perform(get("/api/nihongo-user/expenses/import/template").with(owner()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("mau-chi-tieu.xlsx")));
    }
}
