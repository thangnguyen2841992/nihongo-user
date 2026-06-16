package com.thang.nihongo_user.controller;

import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.dto.CourseDTO;
import com.thang.nihongo_user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nihongo-user")
@RequiredArgsConstructor
public class NihongoUserRestController {
    private final IUserService userService;

    @PreAuthorize("hasAnyRole('ADMIN','STAFF', 'USER')")
    @GetMapping("/courses")
    public ResponseEntity<List<CourseDTO>> getAllCourse() {
        return ResponseEntity.ok(this.userService.getAllCourse());
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF', 'USER')")
    @PostMapping("/courses")
    public ResponseEntity<CourseDTO> createNewCourse(@RequestBody Course course) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.userService.createNewCourse(course));
    }


}
