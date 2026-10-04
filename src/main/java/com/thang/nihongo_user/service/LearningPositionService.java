package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.UserSubscription;
import com.thang.nihongo_user.model.dto.LearningPositionRequest;
import com.thang.nihongo_user.repository.ICourseRepository;
import com.thang.nihongo_user.repository.IStaffClient;
import com.thang.nihongo_user.repository.IUserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LearningPositionService {
    private final IUserSubscriptionRepository subscriptions;
    private final ICourseRepository courses;
    private final IStaffClient staff;

    @Transactional
    public void save(String userId, Long courseId, LearningPositionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vị trí học không hợp lệ");
        }
        if (!subscriptions.existsActive(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chưa có quyền học khóa này");
        }
        UserSubscription subscription = subscriptions.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học đã đăng ký"));
        Long bookId = request.bookId();
        Long lessonId = request.lessonId();
        if ((bookId != null && bookId <= 0) || (lessonId != null && lessonId <= 0) || (lessonId != null && bookId == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vị trí học không hợp lệ");
        }
        if (bookId != null) {
            Long levelId = courses.findById(courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học"))
                    .getLevelId();
            boolean bookBelongsToCourse = staff.getBooksByLevel(levelId).stream()
                    .anyMatch(book -> Objects.equals(bookId, book.getBookId()));
            if (!bookBelongsToCourse) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giáo trình không thuộc khóa học");
            }
            if (lessonId != null && !Objects.equals(staff.getLessonById(lessonId).getBookId(), bookId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài học không thuộc giáo trình");
            }
            subscription.setLastBookId(bookId);
            subscription.setLastLessonId(lessonId);
        }
        LocalDateTime now = LocalDateTime.now();
        if (subscription.getStartedAt() == null) subscription.setStartedAt(now);
        subscription.setLastStudiedAt(now);
        subscriptions.save(subscription);
    }
}
