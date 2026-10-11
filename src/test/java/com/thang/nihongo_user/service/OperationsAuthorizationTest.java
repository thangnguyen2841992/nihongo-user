package com.thang.nihongo_user.service;
import com.nihongo.security.CommonSecurityConfig;
import com.thang.nihongo_user.config.SecurityConfig;
import com.thang.nihongo_user.controller.*;
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
@WebMvcTest(value={AdminCommerceController.class,PromotionQuoteController.class,LearningAnalyticsController.class},properties="spring.config.import=")
@Import({SecurityConfig.class,CommonSecurityConfig.class})
class OperationsAuthorizationTest {
 @Autowired MockMvc mvc;@MockitoBean JwtDecoder decoder;@MockitoBean AdminCommerceService commerce;
 @MockitoBean IUserExerciseAttemptRepository attempts;@MockitoBean WrongAnswerRepository wrong;@MockitoBean IUserSubscriptionRepository subscriptions;@MockitoBean LearnerSupportNoteRepository notes;
 @Test void staffAndUsersCannotReadFinancialOrAuditData() throws Exception {
  for(String role:new String[]{"ROLE_STAFF","ROLE_USER"})for(String path:new String[]{"catalog","revenue","audit","promotions","deposit-audit"})mvc.perform(get("/api/nihongo-user/admin/commerce/"+path).with(jwt().authorities(new SimpleGrantedAuthority(role)))).andExpect(status().isForbidden());
  verifyNoInteractions(commerce);
 }
 @Test void usersCannotReadLearningAnalyticsAndAnonymousCannotQuote() throws Exception {
  mvc.perform(get("/api/nihongo-user/staff/analytics/lessons").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))).andExpect(status().isForbidden());
  mvc.perform(get("/api/nihongo-user/promotions/quote").param("courseId","1").param("packageId","2").param("code","PROMO")).andExpect(status().isUnauthorized());verifyNoInteractions(commerce,attempts);
 }
 @Test void authenticatedSubjectOwnsCouponQuote() throws Exception {
  mvc.perform(get("/api/nihongo-user/promotions/quote").param("courseId","1").param("packageId","2").param("code","PROMO").param("userId","other").with(jwt().jwt(j->j.subject("owner")).authorities(new SimpleGrantedAuthority("ROLE_USER")))).andExpect(status().isOk());verify(commerce).quote("owner",1L,2L,"PROMO");
 }
 @Test void invalidDiscountAndPackagePriceAreRejectedBeforeService() throws Exception {
  mvc.perform(post("/api/nihongo-user/admin/commerce/promotions").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).contentType("application/json").content("{\"code\":\"PROMO\",\"percent\":101,\"maxUses\":10,\"startsAt\":\"2026-10-01T00:00:00\",\"endsAt\":\"2026-11-01T00:00:00\",\"active\":true}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/nihongo-user/admin/commerce/courses/1/packages").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).contentType("application/json").content("{\"name\":\"Gói\",\"durationDays\":30,\"price\":100.5,\"active\":true}")).andExpect(status().isBadRequest());verifyNoInteractions(commerce);
 }
}
