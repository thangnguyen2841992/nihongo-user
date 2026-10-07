package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
 "spring.config.import=",
 "spring.datasource.url=jdbc:h2:mem:attempt_test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
 "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
 "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
 "spring.jpa.show-sql=false", "spring.cloud.discovery.enabled=false", "eureka.client.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ExerciseAttemptPersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExerciseAttemptPersistenceTest {
 @TestConfiguration
 @EntityScan(basePackageClasses = UserExerciseAttempt.class)
 @EnableJpaRepositories(basePackageClasses = IUserExerciseAttemptRepository.class)
 @Import({ExerciseAttemptService.class, ExerciseSubmissionService.class})
 static class Config {}
 @Autowired ExerciseSubmissionService service;
 @Autowired IUserExerciseAttemptRepository attempts;
 @Autowired WrongAnswerRepository wrong;
 @MockitoBean IStaffClient staff;
 @BeforeEach void clear() { wrong.deleteAll(); attempts.deleteAll(); reset(staff); }
 SubmitLessonResultRequest request() {
  var r = new SubmitLessonResultRequest(); r.setLessonId(9L); r.setSubmissionId(UUID.randomUUID().toString());
  r.setAnswers(Map.of(2L, "A", 3L, "B")); return r;
 }
 ExerciseGrade grade() { return new ExerciseGrade(3, 1, 2, Map.of(2L, "A", 3L, "C", 4L, "D"), Map.of()); }

 @Test void savesSeparateSkippedCountAndImmutableAnswersAndReplaysRetries() {
  var r = request(); when(staff.grade(9L, r.getAnswers())).thenReturn(grade());
  var first = service.submit("owner", r);
  assertEquals(1, first.wrongCount()); assertEquals(1, first.unansweredCount());
  var saved = attempts.findByUserIdAndSubmissionId("owner", r.getSubmissionId()).orElseThrow();
  assertEquals(1, saved.getUnansweredCount()); assertEquals(r.getAnswers(), saved.getSnapshot().chosenAnswers());
  assertEquals(grade().correctAnswers(), saved.getSnapshot().grade().correctAnswers());
  when(staff.grade(9L, r.getAnswers())).thenThrow(new IllegalStateException("Source unavailable after first submission"));
  assertEquals(first, service.submit("owner", r)); assertEquals(1, attempts.count());
  verify(staff, times(1)).grade(9L, r.getAnswers());
 }

 @Test void rejectsReusingAnIdWithChangedAnswersButAllowsAnotherUserAndNewAttempt() {
  var r = request(); when(staff.grade(eq(9L), any())).thenReturn(grade());
  service.submit("owner", r);
  service.submit("other-user", r);
  r.setAnswers(Map.of(2L, "B", 3L, "B"));
  assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.submit("owner", r)).getStatusCode().value());
  r.setSubmissionId(UUID.randomUUID().toString());
  service.submit("owner", r);
  assertEquals(3, attempts.count());
 }

 @Test void concurrentRetriesCommitExactlyOneAttempt() throws Exception {
  var r = request(); var barrier = new CyclicBarrier(2);
  when(staff.grade(9L, r.getAnswers())).thenAnswer(call -> { barrier.await(10, TimeUnit.SECONDS); return grade(); });
  var pool = Executors.newFixedThreadPool(2);
  try {
   var a = pool.submit(() -> service.submit("owner", r));
   var b = pool.submit(() -> service.submit("owner", r));
   assertEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
   assertEquals(1, attempts.count()); assertEquals(2, wrong.count());
  } finally { pool.shutdownNow(); }
 }

 @Test void oldAttemptsRetainUnknownSkippedCountAndNoSnapshot() {
  attempts.saveAndFlush(UserExerciseAttempt.builder().userId("owner").lessonId(9L).totalQuestion(3).correctCount(1).wrongCount(2).score(100.0/3).build());
  var old = attempts.findAll().get(0);
  assertNull(old.getUnansweredCount()); assertNull(old.getSnapshot());
 }
}
