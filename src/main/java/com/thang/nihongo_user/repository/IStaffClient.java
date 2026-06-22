package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.dto.BookResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "staff-service", url = "http://localhost:8085")
public interface IStaffClient {
    @GetMapping("/api/staff/getBooksByLevel")
    List<BookResponse> getBooksByLevel(@RequestParam("levelId") Long levelId);
}
