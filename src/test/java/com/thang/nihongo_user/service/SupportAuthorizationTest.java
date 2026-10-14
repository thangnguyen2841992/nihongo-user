package com.thang.nihongo_user.service;

import com.nihongo.security.CommonSecurityConfig;
import com.thang.nihongo_user.config.SecurityConfig;
import com.thang.nihongo_user.controller.StaffSupportController;
import com.thang.nihongo_user.controller.SupportChatController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {SupportChatController.class, StaffSupportController.class}, properties = {
        "spring.config.import=", "security.session-validation.enabled=false", "security.origin-check.enabled=false"
})
@Import({SecurityConfig.class, CommonSecurityConfig.class})
class SupportAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockitoBean SupportChatService support;
    @MockitoBean JwtDecoder decoder;

    @Test
    void userChatUsesTheAuthenticatedSubjectAndProfileClaims() throws Exception {
        mvc.perform(post("/api/nihongo-user/support/chat/messages")
                        .with(jwt().jwt(token -> token.subject("owner").claim("name", "Thắng Nguyễn").claim("email", "thang@example.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType("application/json").content("{\"content\":\"Tôi cần hỗ trợ\"}"))
                .andExpect(status().isOk());
        verify(support).userMessage("owner", "Thắng Nguyễn", "thang@example.com", "Tôi cần hỗ trợ", false);
    }

    @Test
    void staffCannotOpenAUserScopedChatAndUserCannotOpenTheStaffInbox() throws Exception {
        mvc.perform(get("/api/nihongo-user/support/chat")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/nihongo-user/staff/support/conversations")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(support);
    }

    @Test
    void staffAndAdminCanOpenTheSupportInbox() throws Exception {
        for (String role : new String[]{"ROLE_STAFF", "ROLE_ADMIN"}) {
            mvc.perform(get("/api/nihongo-user/staff/support/conversations")
                            .with(jwt().authorities(new SimpleGrantedAuthority(role))))
                    .andExpect(status().isOk());
        }
        verify(support, times(2)).staffConversations(null, 0, 30);
    }

    @Test
    void blankMessagesAreRejectedBeforeTheService() throws Exception {
        mvc.perform(post("/api/nihongo-user/support/chat/messages")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType("application/json").content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(support);
    }
}
