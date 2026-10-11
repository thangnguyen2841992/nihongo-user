package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.LessonCompletion;import org.springframework.data.jpa.repository.*;import org.springframework.data.domain.*;import java.util.*;
public interface LessonCompletionRepository extends JpaRepository<LessonCompletion,Long> {
 Optional<LessonCompletion> findByUserIdAndLessonId(String userId,Long lessonId);
 Page<LessonCompletion> findByUserIdOrderByCompletedAtDescIdDesc(String userId,Pageable page);
 long countByUserId(String userId);
 @Modifying @Query("delete from LessonCompletion c where c.userId=:userId and c.lessonId=:lessonId") void remove(String userId,Long lessonId);
 @Modifying @Query(value="insert into lesson_completion (user_id,lesson_id,name,completed_at) values (:userId,:lessonId,:name,:at) on duplicate key update name=values(name),completed_at=values(completed_at)",nativeQuery=true) void mark(String userId,Long lessonId,String name,java.time.LocalDateTime at);
}
