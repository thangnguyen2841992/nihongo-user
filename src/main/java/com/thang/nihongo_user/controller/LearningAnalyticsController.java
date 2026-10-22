package com.thang.nihongo_user.controller;
import com.thang.nihongo_user.repository.*;
import com.thang.nihongo_user.service.AdminCommerceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDateTime;
import com.thang.nihongo_user.model.LearnerSupportNote;
@RestController @RequestMapping("/api/nihongo-user/staff/analytics") @RequiredArgsConstructor @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class LearningAnalyticsController {
 private final IUserExerciseAttemptRepository attempts;
 private final WrongAnswerRepository wrong;
 private final IUserSubscriptionRepository subscriptions;
 private final LearnerSupportNoteRepository support;
 @GetMapping("/lessons") public StudyController.PageResult<IUserExerciseAttemptRepository.LessonQuality> lessons(@RequestParam(defaultValue="0") int page){return StudyController.PageResult.of(attempts.quality(LocalDateTime.now().minusDays(30),AdminCommerceService.page(page,20)));}
 @GetMapping("/questions") public StudyController.PageResult<WrongAnswerRepository.QuestionQuality> questions(@RequestParam(defaultValue="0") int page){return StudyController.PageResult.of(wrong.quality(LocalDateTime.now().minusDays(QUESTION_WINDOW_DAYS),AdminCommerceService.page(page,20)));}
 // Questions still answered wrong and last missed within this window; avoids grouping the whole table.
 static final int QUESTION_WINDOW_DAYS=90;
 @GetMapping("/inactive") public StudyController.PageResult<IUserSubscriptionRepository.InactiveLearner> inactive(@RequestParam(defaultValue="0") int page){var now=LocalDateTime.now();return StudyController.PageResult.of(subscriptions.inactive(now,now.minusDays(7),now.minusDays(7).toLocalDate(),AdminCommerceService.page(page,20)));}
 @GetMapping("/declining") public StudyController.PageResult<IUserExerciseAttemptRepository.DecliningLearner> declining(@RequestParam(defaultValue="0") int page){var now=LocalDateTime.now();return StudyController.PageResult.of(attempts.declining(now.minusDays(30),now.minusDays(7),AdminCommerceService.page(page,20)));}
 public record SupportInput(@jakarta.validation.constraints.NotBlank String status,@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min=5,max=2000) String note) {}
 @GetMapping("/learners/{userId}/notes") public StudyController.PageResult<LearnerSupportNote> notes(@PathVariable String userId,@RequestParam(defaultValue="0") int page){return StudyController.PageResult.of(support.findByUserIdOrderByCreatedAtDescIdDesc(userId,AdminCommerceService.page(page,20)));}
 @PostMapping("/learners/{userId}/notes") public LearnerSupportNote note(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt,@PathVariable String userId,@jakarta.validation.Valid @RequestBody SupportInput input){
  if(!java.util.List.of("OPEN","CONTACTED","RESOLVED").contains(input.status())||input.note().trim().length()<5)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Ghi chú hoặc trạng thái không hợp lệ");
  if(!attempts.existsByUserId(userId)&&subscriptions.findByUserId(userId).isEmpty())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Không tìm thấy học viên");
  var n=new LearnerSupportNote();n.setUserId(userId);n.setActor(jwt.getSubject());n.setStatus(input.status());n.setNote(input.note().trim());n.setCreatedAt(LocalDateTime.now());return support.save(n);
 }
}
