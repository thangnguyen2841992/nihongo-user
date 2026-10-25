package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.service.KokoroSpeechService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/api/nihongo-user/english-speech")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
@RequiredArgsConstructor
public class EnglishSpeechController {
    private final KokoroSpeechService speech;

    @GetMapping
    public ResponseEntity<byte[]> speech(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam String text,
                                         @RequestParam(defaultValue = "uk") String voice,
                                         @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        // The URL fully determines the audio, so the browser keeps it privately for a week without asking again.
        String etag = speech.etag(voice, text);
        var cache = CacheControl.maxAge(Duration.ofDays(7)).cachePrivate();
        if (ifNoneMatch != null && Arrays.stream(ifNoneMatch.split(",")).map(String::trim)
                .anyMatch(tag -> tag.equals(etag) || tag.equals("W/" + etag)))
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(cache).build();
        var audio = speech.speech(jwt.getSubject(), voice, text);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(audio.contentType()))
                .eTag(etag).cacheControl(cache).body(audio.audio());
    }
}
