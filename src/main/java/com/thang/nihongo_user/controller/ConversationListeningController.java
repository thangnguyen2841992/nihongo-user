package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.service.ConversationListeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/nihongo-user/courses")
public class ConversationListeningController {
    private final ConversationListeningService listening;

    @GetMapping("/{courseId}/conversation-listening")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public List<ConversationListeningService.Recording> recordings(@PathVariable Long courseId, @AuthenticationPrincipal Jwt jwt) {
        return listening.recordings(jwt.getSubject(), courseId);
    }
}
