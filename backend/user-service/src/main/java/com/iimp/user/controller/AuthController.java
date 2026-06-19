package com.iimp.user.controller;

import com.iimp.common.dto.ApiResponse;
import com.iimp.user.dto.AuthRequest;
import com.iimp.user.dto.AuthResponse;
import com.iimp.user.dto.RegisterRequest;
import com.iimp.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT Auth API")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request), "User registered successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ApiResponse.ok(userService.login(request), "Login successful");
    }
}
