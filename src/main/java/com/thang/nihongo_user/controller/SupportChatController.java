package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.service.SupportChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nihongo-user/support/chat")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
public class SupportChatController {
    private final SupportChatService support;

    public record MessageInput(@NotBlank @Size(max = 2000) String content, boolean newConversation) {}

    @GetMapping
    public SupportChatService.ThreadView thread(@AuthenticationPrincipal Jwt jwt) {
        return support.userThread(jwt.getSubject());
    }

    @PostMapping("/messages")
    public SupportChatService.ThreadView message(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MessageInput input) {
        return support.userMessage(jwt.getSubject(), identity(jwt, "name", "preferred_username"), identity(jwt, "email"),
                input.content(), input.newConversation());
    }

    @PostMapping("/read")
    public void read(@AuthenticationPrincipal Jwt jwt) {
        support.userRead(jwt.getSubject());
    }

    private static String identity(Jwt jwt, String... claims) {
        for (String claim : claims) {
            Object value = jwt.getClaims().get(claim);
            if (value instanceof String text && !text.isBlank()) return text;
        }
        return null;
    }
}
