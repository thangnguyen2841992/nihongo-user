package com.thang.nihongo_user.repository;
import org.springframework.cloud.openfeign.FeignClient;import org.springframework.web.bind.annotation.*;import java.util.*;
@FeignClient(name="staff-service",contextId="classroomContent",url="${services.staff.url:}")
public interface ClassroomContentClient {
 record Question(Long id,Long lessonId,String lessonName,Long bookId,String bookName,Long levelId,String levelName,Long typeId,String typeName,String difficulty,String content,String answerA,String answerB,String answerC,String answerD,String correctAnswer){}
 record Exam(Long id,String title,String owner,List<Question> questions){}
 record Lesson(Long lessonId,String name){}
 @GetMapping("/api/staff/content/exams/{id}") Exam exam(@PathVariable Long id);
 @GetMapping("/api/staff/lessons/{id}") Lesson lesson(@PathVariable Long id);
}
