package com.thang.nihongo_user.service;
import com.thang.nihongo_user.repository.ExamAttemptRepository;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.data.domain.PageRequest;import java.time.Duration;import java.time.LocalDateTime;
@Component @RequiredArgsConstructor
public class ExamDeadlineJob {
 private final ExamAttemptRepository attempts;private final ClassroomService classroom;
 /** Published when an attempt starts; the job re-reads the earliest deadline after that transaction commits. */
 public record Started() {}
 // Between deadlines the database is not polled; the cap also picks up attempts started on another instance.
 @org.springframework.beans.factory.annotation.Value("${classroom.deadline.max-idle-ms:30000}") private long maxIdleMs=30_000;
 @org.springframework.beans.factory.annotation.Value("${classroom.deadline.retry-ms:3000}") private long retryMs=3_000;
 private volatile long idleUntil;private volatile boolean woken;
 @org.springframework.transaction.event.TransactionalEventListener(fallbackExecution=true) public void started(Started event){woken=true;}
 @Scheduled(fixedDelayString="${classroom.deadline.tick-ms:1000}") public void finish(){
  if(!woken&&System.currentTimeMillis()<idleUntil)return;
  woken=false;
  LocalDateTime now=LocalDateTime.now();
  var due=attempts.overdue(now,PageRequest.of(0,200));
  for(Long id:due){try{classroom.finalizeDeadline(id);}catch(RuntimeException unavailable){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Exam deadline finalization pending: attemptId={}, failure={}",id,unavailable.getClass().getSimpleName());}}
  if(due.size()==200)return; // more overdue attempts: continue on the next tick
  // Failed finalizations stay overdue: retry them at the old cadence instead of every tick.
  if(!due.isEmpty()){idleUntil=System.currentTimeMillis()+Math.max(0,retryMs);return;}
  var next=attempts.nextDeadline();long limit=Math.max(0,maxIdleMs);
  idleUntil=System.currentTimeMillis()+(next==null?limit:Math.max(0,Math.min(limit,Duration.between(LocalDateTime.now(),next).toMillis())));
 }
}
