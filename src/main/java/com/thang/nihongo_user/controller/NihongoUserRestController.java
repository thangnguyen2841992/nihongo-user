package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.UserSubscription;
import com.thang.nihongo_user.model.dto.CourseDTO;
import com.thang.nihongo_user.model.dto.MyCourseDTO;
import com.thang.nihongo_user.model.dto.UserDTO;
import com.thang.nihongo_user.repository.IUserClient;
import com.thang.nihongo_user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nihongo-user")
@RequiredArgsConstructor
public class NihongoUserRestController {

    private final IUserService userService;
    private final IUserClient userClient;

    // ================= COURSES =================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/courses")
    public ResponseEntity<List<CourseDTO>> getAllCourse() {
        return ResponseEntity.ok(userService.getAllCourse());
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @PostMapping("/courses")
    public ResponseEntity<CourseDTO> createCourse(
            @RequestBody Course course
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createNewCourse(course));
    }

    // ================= SUBSCRIPTION CORE =================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @PostMapping("/subscriptions")
    public ResponseEntity<?> subscribeCourse(
            @RequestParam Long courseId,
            @RequestParam Long packageId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        String email = jwt.getClaimAsString("email");

        UserDTO user = userClient.findUserByEmail(email);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy người dùng");
        }

        try {
            UserSubscription subscription =
                    userService.createSubscription(
                            user.getId(),
                            courseId,
                            packageId
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(subscription);

        } catch (RuntimeException ex) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }
    }

    // ================= MY COURSES (SUBSCRIPTION-BASED) =================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/my-courses")
    public ResponseEntity<List<Long>> getMyCourses(
            @AuthenticationPrincipal Jwt jwt
    ) {

        String email = jwt.getClaimAsString("email");

        UserDTO user = userClient.findUserByEmail(email);

        List<Long> courseIds =
                userService.findCourseIdsByUserId(user.getId());

        return ResponseEntity.ok(courseIds);
    }


    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/my-courses-dto")
    public ResponseEntity<List<MyCourseDTO>> getMyCoursesDTO(@AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getClaimAsString("email");
        UserDTO user = userClient.findUserByEmail(email);

        return ResponseEntity.ok(
                userService.findMyCourses(user.getId())
        );
    }

    // ================= CHECK ACCESS =================

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/courses/{courseId}/access")
    public ResponseEntity<Boolean> checkAccess(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        String email = jwt.getClaimAsString("email");

        UserDTO user = userClient.findUserByEmail(email);

        boolean hasAccess =
                userService.hasActiveSubscription(
                        user.getId(),
                        courseId
                );

        return ResponseEntity.ok(hasAccess);
    }
}