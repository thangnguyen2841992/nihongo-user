package com.thang.nihongo_user.service;
import com.thang.nihongo_user.repository.ExamAttemptRepository;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.data.domain.PageRequest;import java.time.LocalDateTime;
@Component @RequiredArgsConstructor
public class ExamDeadlineJob {
 private final ExamAttemptRepository attempts;private final ClassroomService classroom;
 @Scheduled(fixedDelay=3000) public void finish(){for(Long id:attempts.overdue(LocalDateTime.now(),PageRequest.of(0,200))){try{classroom.finalizeDeadline(id);}catch(RuntimeException unavailable){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Exam deadline finalization pending: attemptId={}, failure={}",id,unavailable.getClass().getSimpleName());}}}
}
