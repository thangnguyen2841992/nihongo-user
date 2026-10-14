package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findTop100ByConversationIdOrderByIdDesc(Long conversationId);
    boolean existsByConversationIdAndDedupKey(Long conversationId, String dedupKey);
}
