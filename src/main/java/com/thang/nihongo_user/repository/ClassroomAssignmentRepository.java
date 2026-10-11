package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.ClassroomAssignment;import org.springframework.data.jpa.repository.*;import org.springframework.data.domain.*;
public interface ClassroomAssignmentRepository extends JpaRepository<ClassroomAssignment,Long> {
 Page<ClassroomAssignment> findByClassroomIdOrderByCreatedAtDescIdDesc(Long classroomId,Pageable page);
 @Query("select a from ClassroomAssignment a where a.published=true and (exists(select m.id from ClassroomMember m where m.classroomId=a.classroomId and m.userId=:userId) or exists(select e.id from ExamAttempt e where e.assignmentId=a.id and e.userId=:userId)) order by a.dueAt desc,a.id desc") Page<ClassroomAssignment> mine(String userId,Pageable page);
}
