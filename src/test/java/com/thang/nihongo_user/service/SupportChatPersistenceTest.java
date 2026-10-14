package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.SupportConversation;
import com.thang.nihongo_user.model.SupportMessage;
import com.thang.nihongo_user.repository.SupportConversationRepository;
import com.thang.nihongo_user.repository.SupportMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:support_chat;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SupportChatPersistenceTest {
    @Autowired SupportConversationRepository conversations;
    @Autowired SupportMessageRepository messages;

    @Test
    void greetsOnlyAfterTheFirstUserMessageAndKeepsOneConversationPerUser() {
        SupportChatService service = new SupportChatService(conversations, messages, mock(SupportRealtimePublisher.class));

        var first = service.userMessage("user-1", "Thắng", "thang@example.com", "Mình cần hỗ trợ", false);
        var second = service.userMessage("user-1", "Thắng", "thang@example.com", "Mình gửi thêm thông tin", false);

        assertThat(first.messages()).hasSize(2);
        assertThat(first.messages()).extracting(SupportChatService.MessageView::senderType)
                .containsExactly("USER", "AUTO");
        assertThat(first.messages().get(1).content()).isEqualTo(SupportChatService.GREETING);
        assertThat(second.messages()).hasSize(3);
        assertThat(second.messages().stream().filter(message -> message.senderType().equals("AUTO"))).hasSize(1);
        assertThat(conversations.count()).isOne();
        assertThat(messages.findAll().stream().filter(message -> message.getSenderType() == SupportMessage.SenderType.AUTO)).hasSize(1);
        assertThat(second.conversation().status()).isEqualTo(SupportConversation.Status.WAITING_STAFF.name());
        assertThat(second.conversation().unreadForStaff()).isEqualTo(2);
        assertThat(service.staffSummary().unreadMessages()).isEqualTo(2);
    }

    @Test
    void staffReplyIsUnreadForTheOwnerAndResolvingUpdatesTheConversation() {
        SupportChatService service = new SupportChatService(conversations, messages, mock(SupportRealtimePublisher.class));
        Long id = service.userMessage("user-2", "An", "an@example.com", "Xin chào", false).conversation().id();

        var replied = service.staffMessage(id, "staff-1", "Hỗ trợ viên", "Nihongo đã nhận được yêu cầu của bạn.");
        var resolved = service.close(id);

        assertThat(replied.conversation().status()).isEqualTo("WAITING_USER");
        assertThat(replied.conversation().unreadForUser()).isOne();
        assertThat(service.staffSummary().unreadMessages()).isZero();
        assertThat(resolved.conversation().status()).isEqualTo("RESOLVED");
        assertThat(resolved.messages().get(resolved.messages().size() - 1).content()).isEqualTo(SupportChatService.CLOSED_MESSAGE);
    }

    @Test
    void aClosedConversationIsImmutableAndTheNextMessageStartsANewConversationExplicitly() {
        SupportChatService service = new SupportChatService(conversations, messages, mock(SupportRealtimePublisher.class));
        var first = service.userMessage("user-3", "Bình", "binh@example.com", "Phiên thứ nhất", false);
        service.close(first.conversation().id());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.userMessage("user-3", "Bình", "binh@example.com", "Không được nối vào phiên cũ", false))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        var next = service.userMessage("user-3", "Bình", "binh@example.com", "Phiên thứ hai", true);

        assertThat(next.conversation().id()).isNotEqualTo(first.conversation().id());
        assertThat(next.messages()).extracting(SupportChatService.MessageView::content)
                .containsExactly("Phiên thứ hai", SupportChatService.GREETING);
        assertThat(conversations.count()).isEqualTo(2);
    }
}
