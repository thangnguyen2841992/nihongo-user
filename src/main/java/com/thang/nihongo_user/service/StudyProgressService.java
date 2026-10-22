package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.StudyPreference;
import com.thang.nihongo_user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class StudyProgressService {
    private final IUserExerciseAttemptRepository attempts;
    private final StudyCardRepository cards;
    private final StudyActivityRepository activities;
    private final StudyPreferenceRepository preferences;
    private final IUserSubscriptionRepository subscriptions;

    public record Goal(int dailyCards, int dailyAttempts, boolean remindersEnabled) {}
    public record Trend(LocalDate day, long attempts, double averageScore) {}
    public record Reminder(String key, String message, Long courseId, LocalDateTime expiresAt) {}
    public record Dashboard(long dueCards, long weakLessons, long attempts, long lessons, double averageScore,
            long cardsToday, long attemptsToday, long practiceToday, boolean goalCompleted,
            int streak, int longestStreak, LocalDate today, String timeZone, Goal goal,
            List<Trend> trend, List<Reminder> reminders) {}

    static final int STREAK_HISTORY_DAYS = 400;

    public Goal goal(String userId) {
        var p = preferences.findById(userId).orElseGet(StudyPreference::new);
        return new Goal(p.getDailyCards(), p.getDailyAttempts(), p.isRemindersEnabled());
    }

    @Transactional
    public Goal saveGoal(String userId, Goal goal) {
        if (goal == null || goal.dailyCards() < 0 || goal.dailyCards() > 200
                || goal.dailyAttempts() < 0 || goal.dailyAttempts() > 20
                || goal.dailyCards() + goal.dailyAttempts() == 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn 1–200 thẻ hoặc 1–20 bài mỗi ngày");
        var p = preferences.findById(userId).orElseGet(StudyPreference::new);
        p.setUserId(userId); p.setDailyCards(goal.dailyCards()); p.setDailyAttempts(goal.dailyAttempts());
        p.setRemindersEnabled(goal.remindersEnabled()); preferences.save(p);
        return goal;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(String userId) {
        LocalDateTime now = LocalDateTime.now(); LocalDate today = now.toLocalDate();
        Goal goal = goal(userId);
        var summary = attempts.summary(userId);
        long due = cards.countByUserIdAndDueAtLessThanEqual(userId, now);
        var daily = activities.dailyCounts(userId, today);
        long cardsToday = daily.getCards();
        long practice = daily.getPractice();
        // Streaks only need recent history; bounding it keeps the dashboard cost flat as accounts age.
        LocalDate since = today.minusDays(STREAK_HISTORY_DAYS);
        TreeSet<LocalDate> dates = new TreeSet<>(attempts.activeDates(userId, since.atStartOfDay())); dates.addAll(activities.activeDates(userId, since));
        int[] streaks = streaks(dates, today);
        List<Trend> trend = attempts.trendBetween(userId, today.minusDays(29).atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                .map(t -> new Trend(t.getDay(), t.getAttempts(), t.getAverageScore() == null ? 0 : t.getAverageScore())).toList();
        long attemptsToday = trend.stream().filter(t -> t.day().equals(today)).mapToLong(Trend::attempts).sum();
        List<Reminder> reminders = new ArrayList<>();
        if (goal.remindersEnabled()) {
            if (due > 0) reminders.add(new Reminder("due-cards", "Bạn có " + due + " thẻ đến hạn ôn tập", null, null));
            for (var s : subscriptions.expiring(userId, now, now.plusDays(7)))
                reminders.add(new Reminder("course:" + s.getCourseId(), "Khóa học sắp hết hạn trong 7 ngày tới", s.getCourseId(), s.getExpiredAt()));
        }
        return new Dashboard(due, attempts.countWeakLessons(userId), summary.getAttempts(), summary.getLessons(),
                summary.getAverageScore() == null ? 0 : summary.getAverageScore(), cardsToday, attemptsToday, practice,
                cardsToday >= goal.dailyCards() && attemptsToday >= goal.dailyAttempts(), streaks[0], streaks[1],
                today, ZoneId.systemDefault().getId(), goal, trend, reminders);
    }

    // An activity streak includes today or yesterday; an unfinished day does not erase yesterday's streak.
    static int[] streaks(SortedSet<LocalDate> dates, LocalDate today) {
        int longest = 0, run = 0; LocalDate previous = null;
        for (LocalDate date : dates) {
            if (date.isAfter(today)) continue;
            run = previous != null && previous.plusDays(1).equals(date) ? run + 1 : 1;
            longest = Math.max(longest, run); previous = date;
        }
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1); int current = 0;
        while (dates.contains(cursor)) { current++; cursor = cursor.minusDays(1); }
        return new int[] {current, longest};
    }
}
