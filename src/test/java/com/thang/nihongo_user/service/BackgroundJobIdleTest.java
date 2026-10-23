package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.CourseStatus;
import com.thang.nihongo_user.model.dto.CreateCourseRequest;
import com.thang.nihongo_user.repository.*;
import com.thang.nihongo_user.wallet.events.WalletEventRecorder;
import com.thang.nihongo_user.wallet.events.WalletOutboxJob;
import com.thang.nihongo_user.wallet.events.WalletOutboxPublisher;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BackgroundJobIdleTest {
    @Test void walletOutboxSleepsWhenEmptyAndWakesForANewEvent() {
        var publisher = mock(WalletOutboxPublisher.class);
        var outbox = mock(WalletOutboxRepository.class);
        when(publisher.publishNext()).thenReturn(false);
        when(outbox.nextAttemptAt()).thenReturn(null);
        var job = new WalletOutboxJob(publisher, outbox);
        job.run(); job.run(); job.run();
        verify(publisher, times(1)).publishNext();
        job.queued(new WalletEventRecorder.Queued());
        job.run();
        verify(publisher, times(2)).publishNext();
    }

    @Test void walletOutboxKeepsDrainingWhileEventsArePublished() {
        var publisher = mock(WalletOutboxPublisher.class);
        var outbox = mock(WalletOutboxRepository.class);
        when(publisher.publishNext()).thenReturn(true);
        var job = new WalletOutboxJob(publisher, outbox);
        job.run(); job.run();
        verify(publisher, times(40)).publishNext();
        verify(outbox, never()).nextAttemptAt();
    }

    @Test void examDeadlineJobSleepsUntilTheEarliestDeadline() {
        var attempts = mock(ExamAttemptRepository.class);
        var classroom = mock(ClassroomService.class);
        when(attempts.overdue(any(), any())).thenReturn(List.of());
        when(attempts.nextDeadline()).thenReturn(LocalDateTime.now().plusMinutes(10));
        var job = new ExamDeadlineJob(attempts, classroom);
        job.finish(); job.finish(); job.finish();
        verify(attempts, times(1)).overdue(any(), any());
        job.started(new ExamDeadlineJob.Started());
        job.finish();
        verify(attempts, times(2)).overdue(any(), any());
    }

    @Test void failedDeadlineFinalizationIsRetriedAtTheOldCadenceNotEveryTick() {
        var attempts = mock(ExamAttemptRepository.class);
        var classroom = mock(ClassroomService.class);
        when(attempts.overdue(any(), any())).thenReturn(List.of(5L));
        doThrow(new IllegalStateException("staff offline")).when(classroom).finalizeDeadline(5L);
        var job = new ExamDeadlineJob(attempts, classroom);
        job.finish(); job.finish();
        verify(classroom, times(1)).finalizeDeadline(5L);
        verify(attempts, never()).nextDeadline();
    }

    @Test void publicCourseListIsCachedAndRefreshedAfterACatalogChange() {
        var courses = mock(ICourseRepository.class);
        when(courses.findAll()).thenReturn(List.of(new CreateCourseRequest("N5", "Intro", 1L, CourseStatus.ACTIVE).toEntity()));
        var service = new UserServiceImpl(courses, mock(ICoursePackageRepository.class), mock(IUserSubscriptionRepository.class),
                mock(IStaffClient.class), mock(IUserExerciseAttemptRepository.class), mock(JapaneseAiService.class),
                mock(AdminCommerceAuditRepository.class));
        assertEquals(1, service.getAllCourse().size());
        service.getAllCourse();
        verify(courses, times(1)).findAll();
        service.catalogChanged(new CourseCatalogChanged());
        service.getAllCourse();
        verify(courses, times(2)).findAll();
    }
}
