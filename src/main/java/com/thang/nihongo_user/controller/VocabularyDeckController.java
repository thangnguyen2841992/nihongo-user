package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.service.VocabularyDeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/nihongo-user/courses")
public class VocabularyDeckController {
    private final VocabularyDeckService decks;

    @GetMapping("/{courseId}/vocabulary-deck")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public VocabularyDeckService.Deck get(@PathVariable Long courseId, @AuthenticationPrincipal Jwt jwt) {
        return decks.get(jwt.getSubject(), courseId);
    }
}
