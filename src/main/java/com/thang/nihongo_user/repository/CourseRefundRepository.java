package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.CourseRefund;import org.springframework.data.jpa.repository.*;import org.springframework.data.domain.*;import jakarta.persistence.LockModeType;import java.util.*;import java.time.LocalDateTime;import java.math.BigDecimal;
public interface CourseRefundRepository extends JpaRepository<CourseRefund,Long> {
 Optional<CourseRefund> findByPurchaseId(Long purchaseId);
 Page<CourseRefund> findAllByOrderByCreatedAtDescIdDesc(Pageable page);
 Page<CourseRefund> findByStatusOrderByCreatedAtDescIdDesc(String status,Pageable page);
 Page<CourseRefund> findByUserIdOrderByCreatedAtDescIdDesc(String userId,Pageable page);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from CourseRefund r where r.id=:id") Optional<CourseRefund> lock(Long id);
 @Query("select coalesce(sum(r.amount),0) from CourseRefund r where r.status='APPROVED' and r.reviewedAt>=:from and r.reviewedAt<:to") BigDecimal total(LocalDateTime from,LocalDateTime to);
}
