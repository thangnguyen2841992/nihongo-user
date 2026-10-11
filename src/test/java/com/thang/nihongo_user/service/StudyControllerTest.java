package com.thang.nihongo_user.service;

import com.nihongo.security.CommonSecurityConfig;
import com.thang.nihongo_user.config.SecurityConfig;
import com.thang.nihongo_user.controller.StudyController;
import com.thang.nihongo_user.repository.*;
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

@WebMvcTest(value = StudyController.class, properties = "spring.config.import=")
@Import({SecurityConfig.class, CommonSecurityConfig.class})
class StudyControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean StudyService study;
    @MockitoBean StudyProgressService progress;
    @MockitoBean WrongAnswerPracticeService practice;
    @MockitoBean StudyCardRepository cards;
    @MockitoBean WrongAnswerRepository wrong;
    @MockitoBean IUserExerciseAttemptRepository attempts;
    @MockitoBean JwtDecoder decoder;

    @Test void anonymousCannotReadAnyNewEndpoint() throws Exception {
        for (String path : new String[]{"dashboard", "cards/page", "cards/due/batch", "weak-lessons/page", "wrong-answers/page"})
            mvc.perform(get("/api/nihongo-user/study/" + path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(progress, cards, wrong, attempts);
    }
    @Test void usesAuthenticatedSubjectAndNeverAnOwnerFromParameters() throws Exception {
        mvc.perform(get("/api/nihongo-user/study/dashboard").param("userId", "other")
                .with(jwt().jwt(j -> j.subject("owner")).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
        verify(progress).dashboard("owner");
        mvc.perform(post("/api/nihongo-user/study/wrong-answers/7/practice").param("userId", "other")
                .with(jwt().jwt(j -> j.subject("owner")).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType("application/json").content("{\"answer\":\"C\"}"))
                .andExpect(status().isOk());
        verify(practice).practice("owner", 7L, "C");
    }
    @Test void rejectsUnboundedPagesAndUnknownRatingsBeforeCallingServices() throws Exception {
        for (String query : new String[]{"?size=101", "?size=0", "?page=-1", "?kind=INVALID"})
            mvc.perform(get("/api/nihongo-user/study/cards/page" + query)
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                    .andExpect(status().isBadRequest());
        mvc.perform(post("/api/nihongo-user/study/cards/1/rating")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType("application/json").content("{\"rating\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cards, study);
    }
}
