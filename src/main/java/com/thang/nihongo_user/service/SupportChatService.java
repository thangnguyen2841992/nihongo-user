package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.SupportConversation;
import com.thang.nihongo_user.model.SupportMessage;
import com.thang.nihongo_user.repository.SupportConversationRepository;
import com.thang.nihongo_user.repository.SupportMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SupportChatService {
    public static final String GREETING = "Chào bạn! Cảm ơn bạn đã liên hệ Nihongo. Bạn hãy mô tả vấn đề cần hỗ trợ, đội ngũ của chúng tôi sẽ phản hồi sớm nhất có thể.";
    public static final String CLOSED_MESSAGE = "Phiên hỗ trợ đã kết thúc. Nếu bạn cần hỗ trợ thêm, hãy bắt đầu một cuộc trò chuyện mới.";
    private static final String GREETING_KEY = "FIRST_USER_MESSAGE_GREETING";
    private static final String CLOSED_KEY = "CONVERSATION_CLOSED";
    private final SupportConversationRepository conversations;
    private final SupportMessageRepository messages;
    private final SupportRealtimePublisher realtime;

    public record MessageView(Long id, String senderType, String senderName, String content, LocalDateTime createdAt) {}
    public record ConversationView(Long id, String userId, String userName, String userEmail, String status,
                                   String lastMessagePreview, int unreadForUser, int unreadForStaff,
                                   LocalDateTime createdAt, LocalDateTime lastMessageAt, long version) {}
    public record ThreadView(ConversationView conversation, List<MessageView> messages) {}
    public record PageView<T>(List<T> content, long totalElements, int number, int totalPages) {}
    public record StaffSummaryView(long unreadMessages) {}

    @Transactional(readOnly = true)
    public ThreadView userThread(String userId) {
        return conversations.findFirstByUserIdOrderByIdDesc(required(userId)).map(this::thread).orElse(new ThreadView(null, List.of()));
    }

    @Transactional
    public ThreadView userMessage(String userId, String userName, String userEmail, String content, boolean newConversation) {
        String actorId = required(userId);
        String text = message(content);
        LocalDateTime now = LocalDateTime.now();
        SupportConversation conversation = conversations.lockActiveByUserId(actorId).orElseGet(() -> {
            boolean hasHistory = conversations.findFirstByUserIdOrderByIdDesc(actorId).isPresent();
            if (hasHistory && !newConversation) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phiên hỗ trợ đã kết thúc. Hãy bắt đầu cuộc trò chuyện mới");
            return create(actorId, userName, userEmail, now);
        });
        updateIdentity(conversation, userName, userEmail);
        SupportMessage sent = saveMessage(conversation.getId(), actorId, display(userName, "Học viên"), SupportMessage.SenderType.USER, text, null, now);
        if (!messages.existsByConversationIdAndDedupKey(conversation.getId(), GREETING_KEY)) {
            saveMessage(conversation.getId(), "SYSTEM", "Nihongo hỗ trợ", SupportMessage.SenderType.AUTO, GREETING, GREETING_KEY, now.plusNanos(1));
        }
        conversation.setStatus(SupportConversation.Status.WAITING_STAFF);
        conversation.setUnreadForStaff(conversation.getUnreadForStaff() + 1);
        conversation.setUnreadForUser(0);
        touch(conversation, text, now);
        conversations.save(conversation);
        realtime.changed(conversation.getId(), sent.getId(), "MESSAGE", actorId);
        return thread(conversation);
    }

    @Transactional
    public void userRead(String userId) {
        conversations.findTopByUserIdOrderByIdDesc(required(userId))
                .ifPresent(conversation -> conversation.setUnreadForUser(0));
    }

    @Transactional(readOnly = true)
    public PageView<ConversationView> staffConversations(String status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw bad("Trang hoặc kích thước không hợp lệ");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        Page<SupportConversation> result;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) result = conversations.findAll(pageable);
        else {
            try { result = conversations.findByStatus(SupportConversation.Status.valueOf(status.toUpperCase(Locale.ROOT)), pageable); }
            catch (IllegalArgumentException error) { throw bad("Trạng thái hội thoại không hợp lệ"); }
        }
        return new PageView<>(result.map(this::view).getContent(), result.getTotalElements(), result.getNumber(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public StaffSummaryView staffSummary() {
        Long unread = conversations.sumUnreadForStaff();
        return new StaffSummaryView(unread == null ? 0 : unread);
    }

    @Transactional
    public ThreadView staffThread(Long id) {
        SupportConversation conversation = locked(id);
        conversation.setUnreadForStaff(0);
        return thread(conversation);
    }

    @Transactional
    public ThreadView staffMessage(Long id, String staffId, String staffName, String content) {
        SupportConversation conversation = locked(id);
        if (conversation.getStatus() == SupportConversation.Status.RESOLVED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiên hỗ trợ đã kết thúc");
        String text = message(content);
        LocalDateTime now = LocalDateTime.now();
        SupportMessage sent = saveMessage(id, required(staffId), display(staffName, "Nhân viên hỗ trợ"), SupportMessage.SenderType.STAFF, text, null, now);
        conversation.setStatus(SupportConversation.Status.WAITING_USER);
        conversation.setUnreadForStaff(0);
        conversation.setUnreadForUser(conversation.getUnreadForUser() + 1);
        touch(conversation, text, now);
        conversations.save(conversation);
        realtime.changed(id, sent.getId(), "MESSAGE", conversation.getUserId());
        return thread(conversation);
    }

    @Transactional
    public ThreadView close(Long id) {
        SupportConversation conversation = locked(id);
        if (conversation.getStatus() == SupportConversation.Status.RESOLVED) return thread(conversation);
        LocalDateTime now = LocalDateTime.now();
        SupportMessage closed = saveMessage(id, "SYSTEM", "Nihongo hỗ trợ", SupportMessage.SenderType.AUTO,
                CLOSED_MESSAGE, CLOSED_KEY, now);
        conversation.setStatus(SupportConversation.Status.RESOLVED);
        conversation.setActiveKey(null);
        conversation.setUnreadForStaff(0);
        conversation.setUnreadForUser(conversation.getUnreadForUser() + 1);
        touch(conversation, CLOSED_MESSAGE, now);
        conversations.save(conversation);
        realtime.changed(id, closed.getId(), "STATUS", conversation.getUserId());
        return thread(conversation);
    }

    private SupportConversation create(String userId, String userName, String userEmail, LocalDateTime now) {
        SupportConversation value = new SupportConversation();
        value.setUserId(userId);
        value.setActiveKey(userId);
        value.setUserName(clean(userName, 120));
        value.setUserEmail(clean(userEmail, 255));
        value.setStatus(SupportConversation.Status.WAITING_STAFF);
        value.setLastMessagePreview("");
        value.setCreatedAt(now);
        value.setUpdatedAt(now);
        value.setLastMessageAt(now);
        return conversations.saveAndFlush(value);
    }

    private void updateIdentity(SupportConversation conversation, String userName, String userEmail) {
        String name = clean(userName, 120), email = clean(userEmail, 255);
        if (name != null) conversation.setUserName(name);
        if (email != null) conversation.setUserEmail(email);
    }

    private SupportMessage saveMessage(Long conversationId, String senderId, String senderName,
                                       SupportMessage.SenderType type, String content, String dedupKey, LocalDateTime at) {
        SupportMessage value = new SupportMessage();
        value.setConversationId(conversationId);
        value.setSenderId(senderId);
        value.setSenderName(senderName);
        value.setSenderType(type);
        value.setContent(content);
        value.setDedupKey(dedupKey);
        value.setCreatedAt(at);
        return messages.save(value);
    }

    private void touch(SupportConversation conversation, String content, LocalDateTime now) {
        conversation.setLastMessagePreview(content.substring(0, Math.min(content.length(), 255)));
        conversation.setUpdatedAt(now);
        conversation.setLastMessageAt(now);
    }

    private SupportConversation locked(Long id) {
        if (id == null) throw bad("Mã hội thoại không hợp lệ");
        return conversations.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hội thoại"));
    }

    private ThreadView thread(SupportConversation conversation) {
        List<MessageView> rows = new ArrayList<>(messages.findTop100ByConversationIdOrderByIdDesc(conversation.getId()).stream().map(this::view).toList());
        Collections.reverse(rows);
        return new ThreadView(view(conversation), rows);
    }

    private ConversationView view(SupportConversation value) {
        return new ConversationView(value.getId(), value.getUserId(), value.getUserName(), value.getUserEmail(), value.getStatus().name(),
                value.getLastMessagePreview(), value.getUnreadForUser(), value.getUnreadForStaff(), value.getCreatedAt(),
                value.getLastMessageAt(), value.getVersion());
    }

    private MessageView view(SupportMessage value) {
        return new MessageView(value.getId(), value.getSenderType().name(), value.getSenderName(), value.getContent(), value.getCreatedAt());
    }

    private static String message(String value) {
        if (value == null || value.trim().isEmpty()) throw bad("Tin nhắn không được để trống");
        String result = value.trim();
        if (result.length() > 2000) throw bad("Tin nhắn không được vượt quá 2000 ký tự");
        return result;
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ");
        return value;
    }

    private static String clean(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim();
        return result.substring(0, Math.min(result.length(), max));
    }

    private static String display(String value, String fallback) {
        String result = clean(value, 120);
        return result == null ? fallback : result;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
