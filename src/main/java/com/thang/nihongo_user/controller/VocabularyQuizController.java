package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.service.VocabularyQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/nihongo-user/courses/{courseId}/vocabulary-quiz")
@PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
public class VocabularyQuizController {
    private final VocabularyQuizService quizzes;

    /** Best score, number of attempts and last attempt time for each lesson the learner has taken. */
    @GetMapping("/summary")
    public VocabularyQuizService.Summary summary(@PathVariable Long courseId, @AuthenticationPrincipal Jwt jwt) {
        return quizzes.summary(jwt.getSubject(), courseId);
    }

    /** Grades the chosen answers of one lesson's quiz against the deck and stores the score. */
    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public VocabularyQuizService.Result submit(@PathVariable Long courseId, @AuthenticationPrincipal Jwt jwt,
                                               @RequestBody VocabularyQuizService.Submission submission) {
        return quizzes.submit(jwt.getSubject(), courseId, submission);
    }
}
