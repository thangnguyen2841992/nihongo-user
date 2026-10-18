package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.model.dto.ExerciseGrade;
import com.thang.nihongo_user.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.config.import=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.generate_statistics=true", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"})
@ContextConfiguration(classes = StudyFeaturesPersistenceTest.Config.class)
class StudyFeaturesPersistenceTest {
    @TestConfiguration @EntityScan(basePackageClasses = StudyCard.class)
    @EnableJpaRepositories(basePackageClasses = StudyCardRepository.class)
    @Import({StudyService.class, StudyActivityService.class, StudyProgressService.class, WrongAnswerPracticeService.class})
    static class Config {}
    @Autowired StudyService study;
    @Autowired StudyProgressService progress;
    @Autowired WrongAnswerPracticeService practice;
    @Autowired StudyCardRepository cards;
    @Autowired WrongAnswerRepository wrong;
    @Autowired IUserExerciseAttemptRepository attempts;
    @Autowired StudyActivityRepository activities;
    @Autowired IUserSubscriptionRepository subscriptions;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @MockitoBean IStaffClient staff;

    StudyCard card() {
        return study.save("owner", new StudyService.CardInput("vocab:" + UUID.randomUUID(), "VOCAB", null, null, null, null, null, "日本", "Nhật Bản", null));
    }
    UserExerciseAttempt attempt(String user, long lesson, double score, LocalDateTime time) {
        var a = UserExerciseAttempt.builder().userId(user).lessonId(lesson).score(score).totalQuestion(10).submittedAt(time).build();
        a = attempts.saveAndFlush(a); a.setSubmittedAt(time); return attempts.saveAndFlush(a);
    }
    WrongAnswer wrong() {
        var w = new WrongAnswer(); w.setUserId("owner"); w.setLessonId(9L); w.setExerciseId(20L);
        w.setChosenAnswer("A"); w.setCorrectAnswer("B"); w.setUpdatedAt(LocalDateTime.now()); return wrong.saveAndFlush(w);
    }

    @Test void aggregatesLatestLessonWithTimestampTieAndScopesEveryQueryToOwner() {
        var now = LocalDateTime.now().minusMinutes(2);
        attempt("owner", 1L, 40, now); attempt("owner", 1L, 90, now);
        attempt("owner", 2L, 60, now); attempt("other", 3L, 10, now);
        var d = progress.dashboard("owner");
        assertEquals(3, d.attempts()); assertEquals(2, d.lessons()); assertEquals(1, d.weakLessons());
        assertEquals(190.0 / 3, d.averageScore(), 0.001);
        assertEquals(1, d.trend().size()); assertEquals(3, d.trend().get(0).attempts());
        var page = attempts.weakLessons("owner", PageRequest.of(0, 1));
        assertEquals(2L, page.getContent().get(0).getLessonId()); assertEquals(1, page.getTotalElements());
    }

    @Test void dashboardUsesNineQueriesAndDoesNotLoadSubscriptionEntitiesOrFutureTrend() {
        var today = LocalDate.now();
        attempt("owner", 1L, 80, today.atStartOfDay());
        attempt("owner", 2L, 60, today.atStartOfDay().minusSeconds(1));
        attempt("owner", 3L, 40, today.plusDays(1).atStartOfDay());
        for (int i = 0; i < 40; i++) {
            var sub = new UserSubscription(); sub.setUserId("owner"); sub.setCourseId((long) i); sub.setPackageId(1L);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setExpiredAt(today.atStartOfDay().plusDays(i == 0 ? 3 : -1));
            subscriptions.save(sub);
        }
        entityManager.flush(); entityManager.clear();
        var statistics = entityManager.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        statistics.clear();
        var result = progress.dashboard("owner");
        assertEquals(1, result.attemptsToday());
        assertEquals(2, result.trend().size());
        assertTrue(result.trend().stream().noneMatch(day -> day.day().isAfter(today)));
        assertEquals(1, result.reminders().size());
        assertEquals(9, statistics.getPrepareStatementCount(), "Dashboard used 11 statements before consolidating daily counts");
        assertEquals(0, statistics.getEntityLoadCount(), "Dashboard should use scalar projections, not hydrate past subscriptions");
    }

    @Test void bothReviewEndpointsRejectDuplicateReviewAndKeepSameSchedule() {
        var c = card();
        var reviewed = study.review("owner", c.getId(), true);
        var due = reviewed.getDueAt();
        assertThrows(ResponseStatusException.class, () -> study.review("owner", c.getId(), true));
        assertThrows(ResponseStatusException.class, () -> study.reviewRated("owner", c.getId(), StudyService.Rating.EASY));
        assertEquals(due, cards.findById(c.getId()).orElseThrow().getDueAt());
        assertEquals(1, progress.dashboard("owner").cardsToday());
    }

    @Test void fourRatingsProduceDifferentIntervalsAndDuplicateReviewCannotMoveSchedule() {
        var hard = card(); var good = card(); var easy = card(); var again = card();
        for (var c : List.of(hard, good, easy)) c.setRepetitions(1);
        var now = LocalDateTime.now();
        assertTrue(study.reviewRated("owner", hard.getId(), StudyService.Rating.HARD).getDueAt().isBefore(now.plusDays(2)));
        assertTrue(study.reviewRated("owner", good.getId(), StudyService.Rating.GOOD).getDueAt().isAfter(now.plusDays(2)));
        assertTrue(study.reviewRated("owner", easy.getId(), StudyService.Rating.EASY).getDueAt().isAfter(now.plusDays(5)));
        var reviewed = study.reviewRated("owner", again.getId(), StudyService.Rating.AGAIN);
        assertEquals(0, reviewed.getRepetitions()); assertTrue(reviewed.getDueAt().isBefore(now.plusMinutes(11)));
        assertThrows(ResponseStatusException.class, () -> study.reviewRated("owner", again.getId(), StudyService.Rating.GOOD));
        assertThrows(ResponseStatusException.class, () -> study.reviewRated("other", hard.getId(), StudyService.Rating.EASY));
        assertEquals(4, progress.dashboard("owner").cardsToday()); assertEquals(0, progress.dashboard("other").cardsToday());
        again.setDueAt(now.minusMinutes(1)); cards.saveAndFlush(again);
        study.reviewRated("owner", again.getId(), StudyService.Rating.GOOD);
        assertEquals(4, progress.dashboard("owner").cardsToday());
    }

    @Test void practiceUsesCurrentAuthoritativeKeyAndNeverChangesOtherWrongAnswersOrLessonScores() {
        var w = wrong();
        when(staff.grade(eq(9L), any())).thenReturn(new ExerciseGrade(2, 1, 1, Map.of(20L, "C", 21L, "D"), Map.of()));
        var result = practice.practice("owner", w.getId(), "B");
        assertFalse(result.correct()); assertEquals("C", result.correctAnswer());
        assertEquals("C", wrong.findById(w.getId()).orElseThrow().getCorrectAnswer());
        assertThrows(ResponseStatusException.class, () -> practice.practice("other", w.getId(), "C"));
        assertTrue(practice.practice("owner", w.getId(), "C").correct());
        assertFalse(wrong.existsById(w.getId())); assertEquals(0, attempts.count());
        assertEquals(1, progress.dashboard("owner").practiceToday());
    }

    @Test void emptyDashboardAndGoalsAndReminderPreferencePersist() {
        assertEquals(0, progress.dashboard("owner").averageScore());
        assertThrows(ResponseStatusException.class, () -> progress.saveGoal("owner", new StudyProgressService.Goal(0, 0, true)));
        assertThrows(ResponseStatusException.class, () -> progress.saveGoal("owner", new StudyProgressService.Goal(201, 0, true)));
        progress.saveGoal("owner", new StudyProgressService.Goal(1, 0, true));
        var c = card();
        // This test checks reminder persistence, not the boundary of the current clock tick.
        c.setDueAt(LocalDateTime.now().minusMinutes(1));
        cards.saveAndFlush(c);
        var sub = new UserSubscription(); sub.setUserId("owner"); sub.setCourseId(7L); sub.setPackageId(1L);
        sub.setStatus(SubscriptionStatus.ACTIVE); sub.setExpiredAt(LocalDateTime.now().plusDays(3)); subscriptions.saveAndFlush(sub);
        assertEquals(2, progress.dashboard("owner").reminders().size());
        study.reviewRated("owner", c.getId(), StudyService.Rating.GOOD);
        assertTrue(progress.dashboard("owner").goalCompleted()); assertEquals(1, progress.dashboard("owner").streak());
        progress.saveGoal("owner", new StudyProgressService.Goal(1, 0, false));
        assertTrue(progress.dashboard("owner").reminders().isEmpty());
        assertTrue(progress.dashboard("other").reminders().isEmpty());
    }

    @Test void streakHandlesYesterdayGapsDuplicatesAndFutureDates() {
        var today = LocalDate.of(2026, 10, 10);
        var dates = new TreeSet<>(List.of(today.minusDays(7), today.minusDays(6), today.minusDays(5), today.minusDays(2), today.minusDays(1), today.plusDays(1)));
        assertArrayEquals(new int[]{2, 3}, StudyProgressService.streaks(dates, today));
        dates.add(today); assertArrayEquals(new int[]{3, 3}, StudyProgressService.streaks(dates, today));
        assertArrayEquals(new int[]{0, 1}, StudyProgressService.streaks(new TreeSet<>(List.of(today.minusDays(3))), today));
    }

    @Test void paginationKeepsOwnerAndKindFiltersAndDeterministicOrder() {
        card(); var latest = card();
        assertEquals(latest.getId(), cards.findByUserIdAndKindOrderByCreatedAtDescIdDesc("owner", "VOCAB", PageRequest.of(0, 1)).getContent().get(0).getId());
        assertEquals(2, cards.findByUserIdOrderByCreatedAtDescIdDesc("owner", PageRequest.of(0, 1)).getTotalElements());
        assertTrue(cards.findByUserIdOrderByCreatedAtDescIdDesc("other", PageRequest.of(0, 1)).isEmpty());
    }

    @Test void analyticsGroupsLessonScoresAndOnlyFlagsLearnersWithRecentDeclines() {
        var now = LocalDateTime.now();
        attempt("declining", 1L, 90, now.minusDays(15)); attempt("declining", 2L, 60, now.minusDays(2));
        attempt("improving", 1L, 60, now.minusDays(15)); attempt("improving", 2L, 90, now.minusDays(2));
        attempt("new", 1L, 20, now.minusDays(2));
        var rows = attempts.declining(now.minusDays(30), now.minusDays(7), PageRequest.of(0,20));
        assertEquals(1, rows.getTotalElements()); assertEquals("declining", rows.getContent().get(0).getUserId());
        assertEquals(60, rows.getContent().get(0).getRecentScore());
        assertEquals(2, attempts.quality(now.minusDays(30), PageRequest.of(0,20)).getTotalElements());
    }

    @Test void inactiveCohortExcludesRecentStudyReviewAndNewEnrollments() {
        var now = LocalDateTime.now();
        for (String user : List.of("inactive", "reviewing", "new")) {
            var s = new UserSubscription(); s.setUserId(user); s.setCourseId(1L); s.setPackageId(1L); s.setStatus(SubscriptionStatus.ACTIVE); s.setExpiredAt(now.plusDays(30));
            s = subscriptions.saveAndFlush(s); s.setCreatedAt(user.equals("new") ? now.minusDays(1) : now.minusDays(10)); subscriptions.saveAndFlush(s);
        }
        var activity = new StudyActivity(); activity.setUserId("reviewing"); activity.setActivityDate(now.toLocalDate()); activity.setSourceKey("card:1"); activities.saveAndFlush(activity);
        var result = subscriptions.inactive(now,now.minusDays(7),now.minusDays(7).toLocalDate(),PageRequest.of(0,20));
        assertEquals(1,result.getTotalElements()); assertEquals("inactive",result.getContent().get(0).getUserId());
    }
}
