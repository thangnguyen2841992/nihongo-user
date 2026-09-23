package com.thang.nihongo_user.wallet;

import com.nihongo.security.CommonSecurityConfig;
import com.thang.nihongo_user.config.SecurityConfig;
import com.thang.nihongo_user.controller.NihongoUserRestController;
import com.thang.nihongo_user.repository.*;
import com.thang.nihongo_user.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NihongoUserRestController.class)
@Import({SecurityConfig.class, CommonSecurityConfig.class})
class PublicCoursesTest {
    @Autowired MockMvc mvc;
    @MockitoBean IUserService service;
    @MockitoBean ICourseRepository courses;
    @MockitoBean IUserClient users;
    @MockitoBean IStaffClient staff;
    @MockitoBean JwtDecoder decoder;
    @Test void anonymousCanReadCatalog() throws Exception {
        when(service.getAllCourse()).thenReturn(List.of());
        mvc.perform(get("/api/nihongo-user/courses")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).getAllCourse();
    }
    @Test void anonymousCannotReadPrivateCoursesOrPurchaseOrCreate() throws Exception {
        mvc.perform(get("/api/nihongo-user/my-courses-dto")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/nihongo-user/subscriptions").param("courseId", "1").param("packageId", "1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/nihongo-user/courses").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/nihongo-user/getBooksByLevel/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
