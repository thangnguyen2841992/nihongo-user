package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.ExamFeedback;import org.springframework.data.jpa.repository.*;import org.springframework.data.domain.*;
public interface ExamFeedbackRepository extends JpaRepository<ExamFeedback,Long> {
 Page<ExamFeedback> findByAttemptIdOrderByCreatedAtAscIdAsc(Long attemptId,Pageable page);
}
