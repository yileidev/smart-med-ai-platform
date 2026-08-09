package com.medical.service;

import com.medical.dto.LoginRequest;
import com.medical.dto.LoginResponse;
import com.medical.entity.User;
import com.medical.repository.UserRepository;
import com.medical.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            // 验证用户名密码
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            
            User user = (User) authentication.getPrincipal();
            
            // 验证角色是否匹配（如果前端指定了角色）
            if (loginRequest.getRole() != null && !loginRequest.getRole().isEmpty()) {
                if (!user.getRole().name().equals(loginRequest.getRole())) {
                    throw new BadCredentialsException("用户角色不匹配");
                }
            }
            
            // 检查用户状态
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                throw new BadCredentialsException("用户账号已被禁用");
            }
            
            // 生成JWT token
            String token = jwtUtil.generateToken(user.getUsername());
            
            // 更新最后登录时间
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            
            // 记录登录日志
            systemLogService.logUserAction(user.getId(), user.getUsername(), 
                "USER_LOGIN", "AUTH", user.getId().toString(), 
                "用户登录成功，角色：" + user.getRole().name());
            
            log.info("用户 {} (角色: {}) 登录成功", user.getUsername(), user.getRole());
            
            return new LoginResponse(token, user);
            
        } catch (AuthenticationException e) {
            log.warn("用户 {} 登录失败: {}", loginRequest.getUsername(), e.getMessage());
            throw new BadCredentialsException("用户名或密码错误");
        }
    }
    
    @Transactional
    public void logout(String username) {
        // 记录登出日志
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            systemLogService.logUserAction(user.getId(), user.getUsername(), 
                "USER_LOGOUT", "AUTH", user.getId().toString(), "用户登出");
        }
        
        log.info("用户 {} 登出", username);
    }
    
    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}