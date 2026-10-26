package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.service.JapaneseReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Hiragana and Hán Việt readings for Japanese text shown to learners (furigana / Hán Việt toggles). */
@RestController
@RequestMapping("/api/nihongo-user/japanese/readings")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
@RequiredArgsConstructor
public class JapaneseReadingController {
    static final int MAX_TEXTS = 100;
    static final int MAX_TOTAL_CHARS = 30_000;
    private final JapaneseReadingService readings;

    public record ReadingRequest(List<String> texts) {}
    public record ReadingResponse(List<List<JapaneseReadingService.Segment>> results) {}

    @PostMapping
    public ResponseEntity<ReadingResponse> annotate(@RequestBody ReadingRequest request) {
        if (request == null || request.texts() == null || request.texts().size() > MAX_TEXTS
                || request.texts().stream().mapToLong(text -> text == null ? 0 : text.length()).sum() > MAX_TOTAL_CHARS)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(new ReadingResponse(request.texts().stream().map(readings::annotate).toList()));
    }
}
