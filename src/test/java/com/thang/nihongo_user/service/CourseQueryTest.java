package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.model.dto.LessonResponse;
import com.thang.nihongo_user.repository.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseQueryTest {
    ICourseRepository courses = mock(ICourseRepository.class);
    ICoursePackageRepository packages = mock(ICoursePackageRepository.class);
    IUserSubscriptionRepository subscriptions = mock(IUserSubscriptionRepository.class);
    IStaffClient staff = mock(IStaffClient.class);
    IUserExerciseAttemptRepository attempts = mock(IUserExerciseAttemptRepository.class);
    UserServiceImpl service = new UserServiceImpl(courses, packages, subscriptions, staff, attempts, mock(JapaneseAiService.class));

    @Test void loadsCourseAndPackageDetailsInBatches() {
        when(subscriptions.findByUserId("owner")).thenReturn(List.of(
                UserSubscription.builder().courseId(1L).packageId(11L).progress(30).startedAt(java.time.LocalDateTime.of(2026, 10, 2, 9, 0)).lastBookId(3L).lastLessonId(7L).build(),
                UserSubscription.builder().courseId(2L).packageId(12L).progress(80).build()));
        Course one = new Course(); one.setCourseId(1L); one.setCourseName("N5");
        Course two = new Course(); two.setCourseId(2L); two.setCourseName("N4");
        CoursePackage first = new CoursePackage(); first.setPackageId(11L); first.setPackageName("Monthly");
        CoursePackage second = new CoursePackage(); second.setPackageId(12L); second.setPackageName("Yearly");
        when(courses.findAllById(List.of(1L, 2L))).thenReturn(List.of(two, one));
        when(packages.findAllById(List.of(11L, 12L))).thenReturn(List.of(second, first));
        var result = service.findMyCourses("owner");
        assertEquals("N5", result.get(0).getCourseName());
        assertEquals("Monthly", result.get(0).getPackageName());
        assertEquals(3L, result.get(0).getLastBookId());
        assertEquals(7L, result.get(0).getLastLessonId());
        assertEquals(80, result.get(1).getProgress());
        verify(courses, never()).findById(any()); verify(packages, never()).findById(any());
    }

    @Test void resolvesALessonNameOncePerResultList() {
        when(attempts.findByUserIdOrderBySubmittedAtDesc("owner")).thenReturn(List.of(
                UserExerciseAttempt.builder().lessonId(1L).score(50D).build(),
                UserExerciseAttempt.builder().lessonId(1L).score(90D).build()));
        when(staff.getLessonById(1L)).thenReturn(new LessonResponse(1L, 1L, "Lesson", "", ""));
        var result = service.getMyResults("owner");
        assertEquals(2, result.size()); assertEquals("Lesson", result.get(0).getLessonName());
        assertEquals(90D, result.get(1).getScore());
        verify(staff, times(1)).getLessonById(1L);
    }
}
