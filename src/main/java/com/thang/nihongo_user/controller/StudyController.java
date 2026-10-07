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

    @GetMapping("/cards")
    public List<StudyCard> cards(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Long lessonId) {
        return lessonId == null ? study.all(jwt.getSubject())
                : cardsRepository.findByUserIdAndLessonIdOrderByCreatedAtDesc(jwt.getSubject(), lessonId);
    }
    public record Overview(long dueCards, long weakLessons) {}
    @GetMapping("/overview")
    public Overview overview(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        var latest = new java.util.HashMap<Long, com.thang.nihongo_user.model.UserExerciseAttempt>();
        for (var attempt : attempts.findByUserIdOrderBySubmittedAtDesc(userId))
            latest.putIfAbsent(attempt.getLessonId(), attempt);
        return new Overview(cardsRepository.countByUserIdAndDueAtLessThanEqual(userId, java.time.LocalDateTime.now()),
                latest.values().stream().filter(attempt -> attempt.getScore() != null && attempt.getScore() < 80).count());
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
