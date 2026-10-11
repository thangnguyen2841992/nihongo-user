package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.AdminCommerceAudit;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
public interface AdminCommerceAuditRepository extends JpaRepository<AdminCommerceAudit,Long> {
 Page<AdminCommerceAudit> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(java.time.LocalDateTime from,java.time.LocalDateTime to,Pageable page);
 Page<AdminCommerceAudit> findAllByOrderByCreatedAtDescIdDesc(Pageable page);
}
