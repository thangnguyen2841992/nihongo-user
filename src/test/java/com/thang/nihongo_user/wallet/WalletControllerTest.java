package com.thang.nihongo_user.wallet;

import com.nihongo.security.CommonSecurityConfig;
import com.thang.nihongo_user.config.SecurityConfig;
import com.thang.nihongo_user.controller.WalletController;
import com.thang.nihongo_user.service.wallet.IWalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(WalletController.class)
@Import({SecurityConfig.class, CommonSecurityConfig.class})
class WalletControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean IWalletService wallets;
    @MockitoBean JwtDecoder decoder;
    @Test void anonymousCannotReadWallet() throws Exception {
        mvc.perform(get("/api/nihongo-user/wallets")).andExpect(status().isUnauthorized());
        verifyNoInteractions(wallets);
    }
    @Test void queryUserIdCannotOverrideAuthenticatedOwner() throws Exception {
        mvc.perform(get("/api/nihongo-user/wallets").param("userId", "someone-else")
            .with(jwt().jwt(j -> j.subject("owner").claim("email", "owner@example.com")).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk());
        verify(wallets).getWallet("owner", "owner@example.com");
    }
    @Test void userAndStaffCannotReviewOrListOtherUsers() throws Exception {
        for (String role : new String[]{"ROLE_USER", "ROLE_STAFF"}) {
            mvc.perform(get("/api/nihongo-user/wallets/admin/deposits")
                .with(jwt().authorities(new SimpleGrantedAuthority(role)))).andExpect(status().isForbidden());
            mvc.perform(post("/api/nihongo-user/wallets/admin/deposits/1/review")
                .with(jwt().authorities(new SimpleGrantedAuthority(role))).contentType("application/json")
                .content("{\"approve\":true,\"bankReference\":\"BANK-123\"}"))
                .andExpect(status().isForbidden());
        }
        verifyNoInteractions(wallets);
    }
    @Test void adminCanReview() throws Exception {
        mvc.perform(post("/api/nihongo-user/wallets/admin/deposits/1/review")
            .with(jwt().jwt(j -> j.subject("admin")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType("application/json").content("{\"approve\":true,\"bankReference\":\"BANK-123\"}"))
            .andExpect(status().isOk());
        verify(wallets).review(eq(1L), eq("admin"), any());
    }
    @Test void invalidAmountAndMissingKeyAreRejected() throws Exception {
        mvc.perform(post("/api/nihongo-user/wallets/deposit")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
            .contentType("application/json").content("{\"amount\":999}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(wallets);
    }
    @Test void historyIsScopedToOwner() throws Exception {
        mvc.perform(get("/api/nihongo-user/wallets/deposits").param("userId", "other")
            .with(jwt().jwt(j -> j.subject("owner").claim("email", "owner@example.com")).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk());
        verify(wallets).history("owner");
    }
}

