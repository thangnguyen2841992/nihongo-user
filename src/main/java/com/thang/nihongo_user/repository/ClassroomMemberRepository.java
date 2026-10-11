package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.ClassroomMember;import org.springframework.data.jpa.repository.*;import org.springframework.data.domain.*;import jakarta.persistence.LockModeType;import java.util.*;
public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember,Long> {
 Page<ClassroomMember> findByClassroomIdOrderByJoinedAtAscIdAsc(Long classroomId,Pageable page);
 boolean existsByClassroomIdAndUserId(Long classroomId,String userId);
 long countByClassroomId(Long classroomId);
 List<ClassroomMember> findByClassroomIdAndUserIdIn(Long classroomId,List<String> userIds);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from ClassroomMember m where m.classroomId=:classroomId and m.userId=:userId") Optional<ClassroomMember> lock(Long classroomId,String userId);
}
