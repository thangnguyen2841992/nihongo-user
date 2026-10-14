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
@RequestMapping("/api/nihongo-user/staff/support")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@RequiredArgsConstructor
public class StaffSupportController {
    private final SupportChatService support;

    public record MessageInput(@NotBlank @Size(max = 2000) String content) {}

    @GetMapping("/conversations")
    public SupportChatService.PageView<SupportChatService.ConversationView> conversations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return support.staffConversations(status, page, size);
    }

    @GetMapping("/summary")
    public SupportChatService.StaffSummaryView summary() {
        return support.staffSummary();
    }

    @GetMapping("/conversations/{id}")
    public SupportChatService.ThreadView thread(@PathVariable Long id) {
        return support.staffThread(id);
    }

    @PostMapping("/conversations/{id}/messages")
    public SupportChatService.ThreadView message(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                  @Valid @RequestBody MessageInput input) {
        return support.staffMessage(id, jwt.getSubject(), name(jwt), input.content());
    }

    @PostMapping("/conversations/{id}/close")
    public SupportChatService.ThreadView close(@PathVariable Long id) {
        return support.close(id);
    }

    private static String name(Jwt jwt) {
        for (String claim : new String[]{"name", "preferred_username", "email"}) {
            Object value = jwt.getClaims().get(claim);
            if (value instanceof String text && !text.isBlank()) return text;
        }
        return "Nhân viên hỗ trợ";
    }
}
