package com.thang.nihongo_user.repository;
import org.springframework.cloud.openfeign.FeignClient;import org.springframework.web.bind.annotation.*;import java.util.*;
@FeignClient(name="user-service",contextId="classroomUsers",url="${services.user.url:}")
public interface ClassroomUserClient {
 record Learner(String userId,String name,String email){}
 @GetMapping("/api/users/staff/learner-members") List<Learner> learners(@RequestParam("ids") List<String> ids);
}
