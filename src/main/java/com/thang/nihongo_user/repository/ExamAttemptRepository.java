package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.ExamAttempt;import org.springframework.data.jpa.repository.*;import org.springframework.data.domain.*;import jakarta.persistence.LockModeType;import java.time.LocalDateTime;import java.util.*;
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt,Long> {
 Optional<ExamAttempt> findByAssignmentIdAndUserId(Long assignmentId,String userId);
 Page<ExamAttempt> findByAssignmentIdOrderByStartedAtDescIdDesc(Long assignmentId,Pageable page);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from ExamAttempt e where e.id=:id") Optional<ExamAttempt> lock(Long id);
 @Query("select e.id from ExamAttempt e where e.submittedAt is null and e.endsAt<=:now order by e.endsAt,e.id") List<Long> overdue(LocalDateTime now,Pageable page);
}
