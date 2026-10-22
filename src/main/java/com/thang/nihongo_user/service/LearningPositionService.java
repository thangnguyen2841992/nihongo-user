package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.UserSubscription;
import com.thang.nihongo_user.model.dto.LearningPositionRequest;
import com.thang.nihongo_user.model.dto.ContentLocationResponse;
import com.thang.nihongo_user.repository.ICourseRepository;
import com.thang.nihongo_user.repository.IStaffClient;
import com.thang.nihongo_user.repository.IUserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LearningPositionService {
    private final IUserSubscriptionRepository subscriptions;
    private final ICourseRepository courses;
    private final IStaffClient staff;

    // Content location is checked over HTTP before the short write transaction starts.
    private TransactionOperations transactions = TransactionOperations.withoutTransaction();
    @Autowired(required = false) void setTransactionManager(PlatformTransactionManager manager) { transactions = new TransactionTemplate(manager); }

    public void save(String userId, Long courseId, LearningPositionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vị trí học không hợp lệ");
        }
        if (!subscriptions.existsActive(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chưa có quyền học khóa này");
        }
        Long bookId = request.bookId();
        Long lessonId = request.lessonId();
        if ((bookId != null && bookId <= 0) || (lessonId != null && lessonId <= 0) || (lessonId != null && bookId == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vị trí học không hợp lệ");
        }
        if (bookId != null) {
            Long levelId = courses.findById(courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học"))
                    .getLevelId();
            ContentLocationResponse location = lessonId == null
                    ? staff.getBookLocation(bookId)
                    : staff.getLessonLocation(lessonId);
            if (!Objects.equals(levelId, location.levelId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giáo trình không thuộc khóa học");
            }
            if (!Objects.equals(bookId, location.bookId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài học không thuộc giáo trình");
            }
        }
        transactions.executeWithoutResult(status -> {
            UserSubscription subscription = subscriptions.findByUserIdAndCourseId(userId, courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học đã đăng ký"));
            if (bookId != null) {
                subscription.setLastBookId(bookId);
                subscription.setLastLessonId(lessonId);
            }
            LocalDateTime now = LocalDateTime.now();
            if (subscription.getStartedAt() == null) subscription.setStartedAt(now);
            subscription.setLastStudiedAt(now);
            subscriptions.save(subscription);
        });
    }
}
