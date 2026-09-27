package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.UserSubscription;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.ICourseRepository;
import com.thang.nihongo_user.repository.IStaffClient;
import com.thang.nihongo_user.repository.IUserClient;
import com.thang.nihongo_user.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/nihongo-user")
@RequiredArgsConstructor
public class NihongoUserRestController {

    private final IUserService userService;
    private final ICourseRepository courseRepository;
    private final com.thang.nihongo_user.service.CoursePurchaseService purchases;
    private final com.thang.nihongo_user.service.ExerciseAttemptService exerciseAttempts;
    private final com.thang.nihongo_user.repository.IUserSubscriptionRepository subscriptions;
    private final IStaffClient staffClient;

    // ================= COURSES =================

    @GetMapping("/courses")
    public ResponseEntity<List<CourseDTO>> getAllCourse() {
        return ResponseEntity.ok(userService.getAllCourse());
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @PostMapping("/courses")
    public ResponseEntity<CourseDTO> createCourse(@Valid @RequestBody CreateCourseRequest course) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createNewCourse(course.toEntity()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @PostMapping("/subscriptions")
    public ResponseEntity<UserSubscription> subscribeCourse(@RequestParam Long courseId, @RequestParam Long packageId,
            @RequestHeader("Idempotency-Key") String key, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchases.purchase(jwt.getSubject(),courseId,packageId,key,false));
    }
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @PostMapping("/subscriptions/renew")
    public ResponseEntity<UserSubscription> renewSubscription(@RequestParam Long courseId, @RequestParam Long packageId,
            @RequestHeader("Idempotency-Key") String key, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(purchases.purchase(jwt.getSubject(),courseId,packageId,key,true));
    }
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/access/levels/{levelId}")
    public boolean levelAccess(@PathVariable Long levelId, @AuthenticationPrincipal Jwt jwt) {
        return subscriptions.hasLevelAccess(jwt.getSubject(),levelId);
    }

    // ================= MY COURSES (SUBSCRIPTION-BASED) =================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/my-courses")
    public ResponseEntity<List<Long>> getMyCourses(@AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();

        List<Long> courseIds = userService.findCourseIdsByUserId(userId);

        return ResponseEntity.ok(courseIds);
    }


    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/my-courses-dto")
    public ResponseEntity<List<MyCourseDTO>> getMyCoursesDTO(@AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();

        return ResponseEntity.ok(userService.findMyCourses(userId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/getBooksByLevel/{courseId}")
    public ResponseEntity<?> getBooksByLevel(@PathVariable Long courseId, @AuthenticationPrincipal Jwt jwt) {

        var roles = jwt.getClaimAsStringList("roles");
        if ((roles == null || roles.stream().noneMatch(r -> r.equals("ADMIN") || r.equals("STAFF")))
                && !userService.hasActiveSubscription(jwt.getSubject(), courseId))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"Bạn chưa có quyền học khóa này");
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Course not found"));

        return ResponseEntity.ok(staffClient.getBooksByLevel(course.getLevelId()));
    }


    // ================= CHECK ACCESS =================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/courses/{courseId}/access")
    public ResponseEntity<Boolean> checkAccess(@PathVariable Long courseId, @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        boolean hasAccess = userService.hasActiveSubscription(userId, courseId);
        return ResponseEntity.ok(hasAccess);
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @PostMapping("/userExerciseAttempt")
    public ResponseEntity<ExerciseGrade> submitUserExerciseAttempt(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SubmitLessonResultRequest request) {
        return ResponseEntity.ok(exerciseAttempts.submit(jwt.getSubject(), request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/userExerciseAttempt")
    public ResponseEntity<List<LessonResultResponse>> getMyResults(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(this.userService.getMyResults(userId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/lesson-result/{lessonId}")
    public ResponseEntity<List<LessonResultResponse>> getLessonResults(@AuthenticationPrincipal Jwt jwt, @PathVariable Long lessonId) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(this.userService.getLessonResults(userId, lessonId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @PostMapping("/japanese")
    public Mono<JapaneseAiResponse> analyzeJapanese(@Valid @RequestBody JapaneseAiRequest request) {
        return userService.analyzeJapanese(request.getText());
    }

}
