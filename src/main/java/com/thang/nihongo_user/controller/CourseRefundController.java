package com.thang.nihongo_user.controller;
import com.thang.nihongo_user.service.*;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import jakarta.validation.Valid;
@RestController @RequiredArgsConstructor
public class CourseRefundController {
 private final CourseRefundService refunds;
 @GetMapping("/api/nihongo-user/admin/refunds/purchases") @PreAuthorize("hasRole('ADMIN')") public Object purchases(@RequestParam(required=false) String userId,@RequestParam(defaultValue="0") int page){return ClassroomController.PageView.from(refunds.purchases(userId,page));}
 @GetMapping("/api/nihongo-user/admin/refunds") @PreAuthorize("hasRole('ADMIN')") public Object list(@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page){return ClassroomController.PageView.from(refunds.list(status,page));}
 @PostMapping("/api/nihongo-user/admin/refunds") @PreAuthorize("hasRole('ADMIN')") public Object request(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CourseRefundService.Request input){return refunds.request(jwt.getSubject(),input);}
 @PostMapping("/api/nihongo-user/admin/refunds/{id}/review") @PreAuthorize("hasRole('ADMIN')") public Object review(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody CourseRefundService.Review input){return refunds.review(jwt.getSubject(),id,input);}
 @GetMapping("/api/nihongo-user/study/refunds") public Object mine(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page){return ClassroomController.PageView.from(refunds.mine(jwt.getSubject(),page));}
}
