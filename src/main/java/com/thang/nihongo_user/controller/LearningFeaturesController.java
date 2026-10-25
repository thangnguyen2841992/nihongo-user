package com.thang.nihongo_user.controller;
import com.thang.nihongo_user.service.LearningFeaturesService;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;
@RestController @RequestMapping("/api/nihongo-user/study") @RequiredArgsConstructor
public class LearningFeaturesController {
 private final LearningFeaturesService features;public record Completion(boolean completed){}
 @GetMapping("/lessons/{id}/completion") public Completion completed(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){return new Completion(features.completed(jwt.getSubject(),id));}
 @PutMapping("/lessons/{id}/completion") public void complete(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody Completion input){features.complete(jwt.getSubject(),id,input.completed());}
 @GetMapping("/completions/among") public java.util.List<Long> completedAmong(@AuthenticationPrincipal Jwt jwt,@RequestParam java.util.List<Long> lessonIds){return features.completedAmong(jwt.getSubject(),lessonIds);}
 @GetMapping("/completions") public Object completed(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page){return features.completions(jwt.getSubject(),page);}
 @GetMapping("/recommendations") public Object recommendations(@AuthenticationPrincipal Jwt jwt){return features.recommendations(jwt.getSubject());}
}
