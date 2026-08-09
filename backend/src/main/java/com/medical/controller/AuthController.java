package com.medical.controller;

import com.medical.dto.ApiResponse;
import com.medical.dto.LoginRequest;
import com.medical.dto.LoginResponse;
import com.medical.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;

@Tag(name = "认证管理", description = "用户登录认证相关接口")
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @Operation(summary = "用户登录", description = "支持医生、护士、管理员多角色登录")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }
    
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(Principal principal) {
        if (principal != null) {
            authService.logout(principal.getName());
        }
        return ResponseEntity.ok(ApiResponse.success("登出成功", "登出成功"));
    }
    
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("未登录"));
        }
        
        var user = authService.getCurrentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("获取成功", user));
    }
    
    @Operation(summary = "检查token有效性")
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<String>> checkToken(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token有效", "Token有效"));
    }
}