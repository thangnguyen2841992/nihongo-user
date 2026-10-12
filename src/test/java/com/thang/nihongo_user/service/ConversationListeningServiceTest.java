package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConversationListeningServiceTest {
    private final ICourseRepository courses = mock(ICourseRepository.class);
    private final IUserSubscriptionRepository subscriptions = mock(IUserSubscriptionRepository.class);
    private final ICoursePackageRepository packages = mock(ICoursePackageRepository.class);
    private final ConversationListeningService service = new ConversationListeningService(courses, subscriptions, packages);
    private Course course;
    private UserSubscription subscription;
    private CoursePackage pack;

    @BeforeEach void setup() {
        course = Course.builder().courseId(42L).levelId(3L).active(CourseStatus.ACTIVE).build();
        subscription = new UserSubscription();
        subscription.setPackageId(9L);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiredAt(LocalDateTime.now().plusDays(1));
        pack = CoursePackage.builder().packageId(9L).course(course).packageName("VIP").build();
        when(courses.findById(42L)).thenReturn(Optional.of(course));
        when(subscriptions.findByUserIdAndCourseId("owner", 42L)).thenReturn(Optional.of(subscription));
        when(packages.findById(9L)).thenReturn(Optional.of(pack));
    }

    @Test void n3VipGetsExistingRecordingRegardlessOfCourseId() {
        assertEquals("4u1pFXayzLE", service.recordings("owner", 42L).get(0).videoId());
    }
    @Test void otherLevelDoesNotGetN3Recording() {
        course.setLevelId(4L);
        assertTrue(service.recordings("owner", 42L).isEmpty());
    }
    @Test void basicAndStandardCannotAccess() {
        for (String name : new String[]{"BASIC", "STANDARD"}) {
            pack.setPackageName(name);
            assertForbidden();
        }
    }
    @Test void expiredOrCancelledSubscriptionCannotAccess() {
        subscription.setExpiredAt(LocalDateTime.now().minusSeconds(1));
        assertForbidden();
        subscription.setExpiredAt(LocalDateTime.now().plusDays(1));
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        assertForbidden();
    }
    @Test void inactiveCourseCannotAccess() {
        course.setActive(null);
        assertForbidden();
    }
    @Test void anotherUsersVipDoesNotGrantAccess() {
        when(subscriptions.findByUserIdAndCourseId("other", 42L)).thenReturn(Optional.empty());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.recordings("other", 42L)).getStatusCode().value());
    }
    @Test void packageOfAnotherCourseCannotGrantAccess() {
        pack.setCourse(Course.builder().courseId(99L).build());
        assertForbidden();
    }
    private void assertForbidden() {
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.recordings("owner", 42L)).getStatusCode().value());
    }
}
