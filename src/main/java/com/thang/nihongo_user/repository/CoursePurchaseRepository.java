package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.CoursePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CoursePurchaseRepository extends JpaRepository<CoursePurchase,Long> {
 org.springframework.data.domain.Page<CoursePurchase> findAllByOrderByCreatedAtDescIdDesc(org.springframework.data.domain.Pageable page);
 org.springframework.data.domain.Page<CoursePurchase> findByUserIdOrderByCreatedAtDescIdDesc(String userId,org.springframework.data.domain.Pageable page);
 interface Sales { Long getCourseId(); Boolean getRenewal(); long getPurchases(); java.math.BigDecimal getAmount(); }
 @org.springframework.data.jpa.repository.Query("select p.courseId as courseId, p.renewal as renewal, count(p) as purchases, sum(p.amount) as amount from CoursePurchase p where p.createdAt >= :from and p.createdAt < :to group by p.courseId,p.renewal order by p.courseId,p.renewal")
 java.util.List<Sales> sales(java.time.LocalDateTime from,java.time.LocalDateTime to);
 org.springframework.data.domain.Page<CoursePurchase> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(java.time.LocalDateTime from,java.time.LocalDateTime to,org.springframework.data.domain.Pageable page);
 Optional<CoursePurchase> findByUserIdAndRequestKey(String userId,String requestKey);
}
