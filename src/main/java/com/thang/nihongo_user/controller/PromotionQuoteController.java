package com.thang.nihongo_user.controller;
import com.thang.nihongo_user.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
@RestController @RequestMapping("/api/nihongo-user/promotions") @RequiredArgsConstructor @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
public class PromotionQuoteController {
 private final AdminCommerceService commerce;
 @GetMapping("/quote") public PromotionService.Discount quote(@AuthenticationPrincipal Jwt jwt,@RequestParam Long courseId,@RequestParam Long packageId,@RequestParam String code){return commerce.quote(jwt.getSubject(),courseId,packageId,code);}
}
