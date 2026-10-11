package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.model.StudyCard;
import com.thang.nihongo_user.model.WrongAnswer;
import com.thang.nihongo_user.repository.WrongAnswerRepository;
import com.thang.nihongo_user.repository.StudyCardRepository;
import com.thang.nihongo_user.repository.IUserExerciseAttemptRepository;
import com.thang.nihongo_user.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/nihongo-user/study")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
@RequiredArgsConstructor
public class StudyController {
    private final StudyService study;
    private final WrongAnswerRepository wrongAnswers;
    private final StudyCardRepository cardsRepository;
    private final IUserExerciseAttemptRepository attempts;
    private final com.thang.nihongo_user.service.StudyProgressService progress;
    private final com.thang.nihongo_user.service.WrongAnswerPracticeService practice;

    public record PageResult<T>(List<T> content, long totalElements, int number, int totalPages) {
        static <T> PageResult<T> of(org.springframework.data.domain.Page<T> page) {
            return new PageResult<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getTotalPages());
        }
    }
    private org.springframework.data.domain.Pageable page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Trang hoặc kích thước không hợp lệ");
        return org.springframework.data.domain.PageRequest.of(page, size);
    }
    @GetMapping("/dashboard")
    public com.thang.nihongo_user.service.StudyProgressService.Dashboard dashboard(@AuthenticationPrincipal Jwt jwt) {
        return progress.dashboard(jwt.getSubject());
    }
    @PutMapping("/goal")
    public com.thang.nihongo_user.service.StudyProgressService.Goal goal(@AuthenticationPrincipal Jwt jwt,
            @RequestBody com.thang.nihongo_user.service.StudyProgressService.Goal goal) {
        return progress.saveGoal(jwt.getSubject(), goal);
    }
    @GetMapping("/cards/page")
    public PageResult<StudyCard> cardPage(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String kind) {
        var pageable = page(page, size);
        if (kind != null && !List.of("EXAMPLE", "GRAMMAR", "VOCAB").contains(kind))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Loại thẻ không hợp lệ");
        return PageResult.of(kind == null ? cardsRepository.findByUserIdOrderByCreatedAtDescIdDesc(jwt.getSubject(), pageable)
                : cardsRepository.findByUserIdAndKindOrderByCreatedAtDescIdDesc(jwt.getSubject(), kind, pageable));
    }
    @GetMapping("/cards/due/batch")
    public List<StudyCard> dueBatch(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "30") int size) {
        return cardsRepository.findByUserIdAndDueAtLessThanEqualOrderByDueAtAscIdAsc(jwt.getSubject(), java.time.LocalDateTime.now(), page(0, size));
    }
    public record RatedReview(com.thang.nihongo_user.service.StudyService.Rating rating) {}
    @PostMapping("/cards/{id}/rating")
    public StudyCard rate(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestBody RatedReview request) {
        return study.reviewRated(jwt.getSubject(), id, request.rating());
    }
    @GetMapping("/weak-lessons/page")
    public PageResult<IUserExerciseAttemptRepository.LatestLesson> weakLessons(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResult.of(attempts.weakLessons(jwt.getSubject(), page(page, size)));
    }
    @GetMapping("/wrong-answers/page")
    public PageResult<WrongAnswer> wrongPage(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResult.of(wrongAnswers.findByUserIdOrderByUpdatedAtDescIdDesc(jwt.getSubject(), page(page, size)));
    }
    public record PracticeAnswer(String answer) {}
    @PostMapping("/wrong-answers/{id}/practice")
    public com.thang.nihongo_user.service.WrongAnswerPracticeService.Result practice(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id, @RequestBody PracticeAnswer request) {
        return practice.practice(jwt.getSubject(), id, request.answer());
    }
    @DeleteMapping("/wrong-answers/{id}")
    public void master(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) { practice.master(jwt.getSubject(), id); }

    @GetMapping("/cards")
    public List<StudyCard> cards(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Long lessonId) {
        return lessonId == null ? study.all(jwt.getSubject())
                : cardsRepository.findByUserIdAndLessonIdOrderByCreatedAtDesc(jwt.getSubject(), lessonId);
    }
    public record Overview(long dueCards, long weakLessons) {}
    @GetMapping("/overview")
    public Overview overview(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return new Overview(cardsRepository.countByUserIdAndDueAtLessThanEqual(userId, java.time.LocalDateTime.now()),
                attempts.countWeakLessons(userId));
    }
    @GetMapping("/cards/due")
    public List<StudyCard> due(@AuthenticationPrincipal Jwt jwt) { return study.due(jwt.getSubject()); }
    @PutMapping("/cards")
    public StudyCard save(@AuthenticationPrincipal Jwt jwt, @RequestBody StudyService.CardInput input) {
        return study.save(jwt.getSubject(), input);
    }
    @PostMapping("/cards/{id}/review")
    public StudyCard review(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                            @RequestParam boolean remembered) { return study.review(jwt.getSubject(), id, remembered); }
    @DeleteMapping("/cards/{id}")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) { study.delete(jwt.getSubject(), id); }
    @GetMapping("/wrong-answers")
    public List<WrongAnswer> wrongAnswers(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Long lessonId) {
        return lessonId == null ? wrongAnswers.findByUserIdOrderByUpdatedAtDesc(jwt.getSubject())
                : wrongAnswers.findByUserIdAndLessonId(jwt.getSubject(), lessonId);
    }
}
