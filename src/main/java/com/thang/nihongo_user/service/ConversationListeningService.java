package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationListeningService {
    private final ICourseRepository courses;
    private final IUserSubscriptionRepository subscriptions;
    private final ICoursePackageRepository packages;

    public record Recording(String id, String title, String videoId) {}

    @Transactional(readOnly = true)
    public List<Recording> recordings(String userId, Long courseId) {
        var course = courses.findById(courseId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học"));
        var subscription = subscriptions.findByUserIdAndCourseId(userId, courseId).orElseThrow(ConversationListeningService::forbidden);
        if (course.getActive() != CourseStatus.ACTIVE
                || subscription.getStatus() != SubscriptionStatus.ACTIVE
                || subscription.getExpiredAt() == null
                || !subscription.getExpiredAt().isAfter(LocalDateTime.now())) throw forbidden();
        var pack = packages.findById(subscription.getPackageId()).orElseThrow(ConversationListeningService::forbidden);
        if (pack.getCourse() == null || !courseId.equals(pack.getCourse().getCourseId())
                || !"VIP".equalsIgnoreCase(pack.getPackageName())) throw forbidden();
        // The existing recording belongs to N3. Other levels have no recordings yet.
        return Long.valueOf(3).equals(course.getLevelId())
                ? List.of(new Recording("n3-conversation-1", "Luyện nghe hội thoại N3", "4u1pFXayzLE"))
                : List.of();
    }

    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Luyện nghe hội thoại dành cho gói VIP còn hạn của khóa học này.");
    }
}
