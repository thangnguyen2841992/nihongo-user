package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.UserCourse;
import com.thang.nihongo_user.model.dto.CourseDTO;
import com.thang.nihongo_user.model.dto.UserDTO;
import com.thang.nihongo_user.repository.IUserClient;
import com.thang.nihongo_user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;


import java.util.List;

@RestController
@RequestMapping("/api/nihongo-user")
@RequiredArgsConstructor
public class NihongoUserRestController {
    private final IUserService userService;

    private final IUserClient userClient;

    @PreAuthorize("hasAnyRole('ADMIN','STAFF', 'USER')")
    @GetMapping("/courses")
    public ResponseEntity<List<CourseDTO>> getAllCourse() {
        return ResponseEntity.ok(this.userService.getAllCourse());
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @GetMapping("/my-courses")
    public ResponseEntity<List<Long>> getMyCourseIds(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = jwt.getClaimAsString("email");

        UserDTO userDTO =
                userClient.findUserByEmail(email);

        List<Long> courseIds =
                userService.findCourseIdsByUserId(
                        userDTO.getId()
                );

        return ResponseEntity.ok(courseIds);
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF', 'USER')")
    @PostMapping("/courses")
    public ResponseEntity<CourseDTO> createNewCourse(@RequestBody Course course) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.userService.createNewCourse(course));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF', 'USER')")
    @PostMapping("/userCourses")
    public ResponseEntity<?> createNewUserCourse(@RequestBody UserCourse course, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        UserDTO userDTO = this.userClient.findUserByEmail(email);
        if (userDTO == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy người dùng");
        }
        boolean isExist = this.userService.existsByCourseIdAndUserId(course.getCourseId(), userDTO.getId());
        if (isExist) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Đã tồn tại rồi");
        }
        course.setUserId(userDTO.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.userService.createNewUserCourse(course));
    }


}
