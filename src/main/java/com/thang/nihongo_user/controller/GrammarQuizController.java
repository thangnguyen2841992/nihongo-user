package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.service.GrammarQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/nihongo-user/courses/{courseId}/grammar-quiz")
@PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
public class GrammarQuizController {
    private final GrammarQuizService quizzes;

    /** Grammar questions of Minna no Nihongo II, lessons 26–50 (N4 courses only). */
    @GetMapping
    public GrammarQuizService.Bank bank(@PathVariable Long courseId, @AuthenticationPrincipal Jwt jwt) {
        return quizzes.bank(jwt.getSubject(), courseId);
    }

    /** Best score, number of attempts and last attempt time for each lesson the learner has taken. */
    @GetMapping("/summary")
    public GrammarQuizService.Summary summary(@PathVariable Long courseId, @AuthenticationPrincipal Jwt jwt) {
        return quizzes.summary(jwt.getSubject(), courseId);
    }

    /** Grades the chosen answers of one lesson against the question bank and stores the score. */
    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public GrammarQuizService.Result submit(@PathVariable Long courseId, @AuthenticationPrincipal Jwt jwt,
                                            @RequestBody GrammarQuizService.Submission submission) {
        return quizzes.submit(jwt.getSubject(), courseId, submission);
    }
}
