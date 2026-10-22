package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.SupportConversation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SupportConversationRepository extends JpaRepository<SupportConversation, Long> {
    Optional<SupportConversation> findFirstByUserIdOrderByIdDesc(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SupportConversation c where c.activeKey=:userId")
    Optional<SupportConversation> lockActiveByUserId(@Param("userId") String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SupportConversation> findTopByUserIdOrderByIdDesc(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SupportConversation c where c.id=:id")
    Optional<SupportConversation> lockById(@Param("id") Long id);

    Page<SupportConversation> findByStatus(SupportConversation.Status status, Pageable pageable);

    @Query("select coalesce(sum(c.unreadForStaff), 0) from SupportConversation c where c.unreadForStaff > 0")
    Long sumUnreadForStaff();
}
