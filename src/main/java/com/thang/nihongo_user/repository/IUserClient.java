package com.thang.nihongo_user.repository;

import com.thang.nihongo_user.model.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "gateway-service", url = "http://localhost:8082")
public interface IUserClient {
    @GetMapping("/api/users/findUserByEmail")
    UserDTO findUserByEmail(@RequestParam("email") String email);
}
