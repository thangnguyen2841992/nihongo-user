package com.thang.nihongo_user.controller;
import com.thang.nihongo_user.service.AdminCommerceService;
import com.thang.nihongo_user.model.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/nihongo-user/admin/commerce") @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AdminCommerceController {
 private final AdminCommerceService commerce;
 @GetMapping("/catalog") public List<AdminCommerceService.Catalog> catalog(){return commerce.catalog();}
 @PostMapping("/courses") public AdminCommerceService.Catalog create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AdminCommerceService.CourseInput input){return commerce.saveCourse(jwt.getSubject(),null,input);}
 @PutMapping("/courses/{id}") public AdminCommerceService.Catalog update(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody AdminCommerceService.CourseInput input){return commerce.saveCourse(jwt.getSubject(),id,input);}
 @PostMapping("/courses/{courseId}/packages") public AdminCommerceService.Catalog createPackage(@AuthenticationPrincipal Jwt jwt,@PathVariable Long courseId,@Valid @RequestBody AdminCommerceService.PackageInput input){return commerce.savePackage(jwt.getSubject(),courseId,null,input);}
 @PutMapping("/courses/{courseId}/packages/{id}") public AdminCommerceService.Catalog updatePackage(@AuthenticationPrincipal Jwt jwt,@PathVariable Long courseId,@PathVariable Long id,@Valid @RequestBody AdminCommerceService.PackageInput input){return commerce.savePackage(jwt.getSubject(),courseId,id,input);}
 @GetMapping("/promotions") public StudyController.PageResult<Promotion> promotions(@RequestParam(defaultValue="0") int page){return StudyController.PageResult.of(commerce.promotions(AdminCommerceService.page(page,20)));}
 @PostMapping("/promotions") public Promotion createPromotion(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AdminCommerceService.PromotionInput input){return commerce.savePromotion(jwt.getSubject(),null,input);}
 @PutMapping("/promotions/{id}") public Promotion updatePromotion(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@Valid @RequestBody AdminCommerceService.PromotionInput input){return commerce.savePromotion(jwt.getSubject(),id,input);}
 @GetMapping("/revenue") public AdminCommerceService.Revenue revenue(@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){var today=LocalDate.now();return commerce.revenue(from==null?today.withDayOfMonth(1):from,to==null?today:to);}
 @GetMapping("/audit") public StudyController.PageResult<AdminCommerceAudit> audit(@RequestParam(defaultValue="0") int page){return StudyController.PageResult.of(commerce.auditPage(AdminCommerceService.page(page,20)));}
 @GetMapping("/deposit-audit") public StudyController.PageResult<AdminCommerceService.ReviewAudit> depositAudit(@RequestParam(defaultValue="0") int page){return StudyController.PageResult.of(commerce.depositAudit(AdminCommerceService.page(page,20)));}
}
