package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.dto.BookResponse;
import com.thang.nihongo_user.model.dto.LessonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "gateway-service",
        contextId = "staffClient", url = "http://localhost:8082")
public interface IStaffClient {
    @GetMapping("/api/staff/getBooksByLevel")
    List<BookResponse> getBooksByLevel(@RequestParam("levelId") Long levelId);

    @GetMapping("/api/staff/lessons/{id}")
    LessonResponse getLessonById(@PathVariable Long id);
}
