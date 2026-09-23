package com.flashsale.controller;

import com.flashsale.common.Result;
import com.flashsale.dto.LoginRequest;
import com.flashsale.dto.LoginResponse;
import com.flashsale.dto.RegisterRequest;
import com.flashsale.entity.User;
import com.flashsale.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return Result.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername()
        ));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }
}
