package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.UserSubscription;
import com.thang.nihongo_user.model.dto.ContentLocationResponse;
import com.thang.nihongo_user.model.dto.LearningPositionRequest;
import com.thang.nihongo_user.repository.ICourseRepository;
import com.thang.nihongo_user.repository.IStaffClient;
import com.thang.nihongo_user.repository.IUserSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LearningPositionServiceTest {
    private final IUserSubscriptionRepository subscriptions = mock(IUserSubscriptionRepository.class);
    private final ICourseRepository courses = mock(ICourseRepository.class);
    private final IStaffClient staff = mock(IStaffClient.class);
    private final LearningPositionService service = new LearningPositionService(subscriptions, courses, staff);

    @Test void startsThenRemembersOnlyALessonInTheCourseBook() {
        UserSubscription subscription = new UserSubscription();
        when(subscriptions.existsActive("owner", 1L)).thenReturn(true);
        when(subscriptions.findByUserIdAndCourseId("owner", 1L)).thenReturn(Optional.of(subscription));
        Course course = new Course(); course.setLevelId(5L);
        when(courses.findById(1L)).thenReturn(Optional.of(course));
        when(staff.getBookLocation(10L)).thenReturn(new ContentLocationResponse(10L, 5L, "Book"));
        when(staff.getLessonLocation(20L)).thenReturn(new ContentLocationResponse(10L, 5L, "Lesson"));

        service.save("owner", 1L, new LearningPositionRequest(null, null));
        assertNotNull(subscription.getStartedAt());
        service.save("owner", 1L, new LearningPositionRequest(10L, 20L));
        assertEquals(10L, subscription.getLastBookId());
        assertEquals(20L, subscription.getLastLessonId());
        service.save("owner", 1L, new LearningPositionRequest(10L, null));
        assertNull(subscription.getLastLessonId());
        verify(staff, times(1)).getBookLocation(10L);
        verify(staff, times(1)).getLessonLocation(20L);
    }

    @Test void refusesInactiveCourseAndUnrelatedLesson() {
        assertThrows(ResponseStatusException.class,
                () -> service.save("other", 1L, new LearningPositionRequest(null, null)));
        verify(subscriptions, never()).save(any());

        UserSubscription subscription = new UserSubscription();
        when(subscriptions.existsActive("owner", 1L)).thenReturn(true);
        when(subscriptions.findByUserIdAndCourseId("owner", 1L)).thenReturn(Optional.of(subscription));
        Course course = new Course(); course.setLevelId(5L);
        when(courses.findById(1L)).thenReturn(Optional.of(course));
        when(staff.getLessonLocation(20L)).thenReturn(new ContentLocationResponse(99L, 5L, "Wrong"));
        assertThrows(ResponseStatusException.class,
                () -> service.save("owner", 1L, new LearningPositionRequest(10L, 20L)));
        verify(subscriptions, never()).save(any());
        verify(staff, never()).getBookLocation(anyLong());
    }

    @Test void refusesBookFromAnotherCourseLevel() {
        when(subscriptions.existsActive("owner", 1L)).thenReturn(true);
        when(subscriptions.findByUserIdAndCourseId("owner", 1L)).thenReturn(Optional.of(new UserSubscription()));
        Course course = new Course(); course.setLevelId(5L);
        when(courses.findById(1L)).thenReturn(Optional.of(course));
        when(staff.getBookLocation(10L)).thenReturn(new ContentLocationResponse(10L, 4L, "Book"));

        assertThrows(ResponseStatusException.class,
                () -> service.save("owner", 1L, new LearningPositionRequest(10L, null)));
        verify(subscriptions, never()).save(any());
    }
}
