package com.medical.config;

import com.medical.dto.ApiResponse;
import com.medical.exception.AIServiceException;
import com.medical.exception.BusinessException;
import com.medical.exception.MQTTException;
import com.medical.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 🛡️ 医疗急诊分诊系统 - 全局异常处理器
 * 统一处理系统中所有异常，提供标准化的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 🛡️ 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        
        log.warn("业务异常: {} - {} - {}", 
            request.getRequestURI(), e.getErrorCode(), e.getMessage());
        
        Map<String, Object> errorData = Map.of(
            "code", e.getErrorCode(),
            "message", e.getMessage(),
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(e.getHttpStatus())
            .body(new ApiResponse<>(false, e.getMessage(), errorData));
    }

    /**
     * 🛡️ 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        Map<String, String> validationErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        log.warn("参数验证失败: {} - {}", request.getRequestURI(), validationErrors);
        
        Map<String, Object> errorData = Map.of(
            "code", "VALIDATION_ERROR",
            "message", "参数验证失败",
            "details", validationErrors,
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ApiResponse<>(false, "参数验证失败", errorData));
    }

    /**
     * 🛡️ 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBindException(
            BindException e, HttpServletRequest request) {
        
        Map<String, String> bindErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            bindErrors.put(fieldName, errorMessage);
        });
        
        log.warn("数据绑定异常: {} - {}", request.getRequestURI(), bindErrors);
        
        Map<String, Object> errorData = Map.of(
            "code", "BIND_ERROR",
            "message", "数据绑定失败",
            "details", bindErrors,
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ApiResponse<>(false, "数据绑定失败", errorData));
    }

    /**
     * 🛡️ 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        
        log.warn("约束违反异常: {} - {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> errorData = Map.of(
            "code", "CONSTRAINT_VIOLATION",
            "message", "数据约束违反",
            "details", e.getMessage(),
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ApiResponse<>(false, "数据约束违反", errorData));
    }

    /**
     * 🛡️ 处理类型转换异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        
        log.warn("类型转换异常: {} - {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> errorData = Map.of(
            "code", "TYPE_MISMATCH",
            "message", "参数类型不匹配",
            "details", String.format("参数 %s 的类型应为 %s", 
                e.getName(), e.getRequiredType().getSimpleName()),
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ApiResponse<>(false, "参数类型不匹配", errorData));
    }

    /**
     * 🛡️ 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        
        log.warn("认证异常: {} - {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> errorData = Map.of(
            "code", "AUTHENTICATION_FAILED",
            "message", "认证失败",
            "details", "用户名或密码错误",
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse<>(false, "认证失败", errorData));
    }

    /**
     * 🛡️ 处理凭据错误异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBadCredentialsException(
            BadCredentialsException e, HttpServletRequest request) {
        
        log.warn("凭据错误: {} - {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> errorData = Map.of(
            "code", "BAD_CREDENTIALS",
            "message", "用户名或密码错误",
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse<>(false, "用户名或密码错误", errorData));
    }

    /**
     * 🛡️ 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        
        log.warn("访问被拒绝: {} - {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> errorData = Map.of(
            "code", "ACCESS_DENIED",
            "message", "访问被拒绝",
            "details", "您没有权限访问此资源",
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ApiResponse<>(false, "访问被拒绝", errorData));
    }

    /**
     * 🛡️ 处理数据库异常
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleSQLException(
            SQLException e, HttpServletRequest request) {
        
        log.error("数据库异常: {} - {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> errorData = Map.of(
            "code", "DATABASE_ERROR",
            "message", "数据库操作失败",
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(false, "系统繁忙，请稍后重试", errorData));
    }

    /**
     * 🛡️ 处理AI服务异常
     */
    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAIServiceException(
            AIServiceException e, HttpServletRequest request) {
        
        log.error("AI服务异常: {} - {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> errorData = Map.of(
            "code", "AI_SERVICE_ERROR",
            "message", "AI诊断服务暂时不可用",
            "details", e.getMessage(),
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ApiResponse<>(false, "AI诊断服务暂时不可用，请稍后重试", errorData));
    }

    /**
     * 🛡️ 处理MQTT异常
     */
    @ExceptionHandler(MQTTException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMQTTException(
            MQTTException e, HttpServletRequest request) {
        
        log.error("MQTT异常: {} - {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> errorData = Map.of(
            "code", "MQTT_ERROR",
            "message", "消息队列服务异常",
            "details", e.getMessage(),
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ApiResponse<>(false, "消息队列服务异常", errorData));
    }

    /**
     * 🛡️ 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleResourceNotFoundException(
            ResourceNotFoundException e, HttpServletRequest request) {
        
        log.warn("资源未找到: {} - {}", request.getRequestURI(), e.getMessage());
        
        Map<String, Object> errorData = Map.of(
            "code", "RESOURCE_NOT_FOUND",
            "message", e.getMessage(),
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ApiResponse<>(false, e.getMessage(), errorData));
    }

    /**
     * 🛡️ 处理通用运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRuntimeException(
            RuntimeException e, HttpServletRequest request) {
        
        log.error("运行时异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        
        Map<String, Object> errorData = Map.of(
            "code", "RUNTIME_ERROR",
            "message", "系统运行异常",
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(false, "系统异常，请稍后重试", errorData));
    }

    /**
     * 🛡️ 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGenericException(
            Exception e, HttpServletRequest request) {
        
        log.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        
        Map<String, Object> errorData = Map.of(
            "code", "INTERNAL_SERVER_ERROR",
            "message", "系统内部错误",
            "timestamp", LocalDateTime.now(),
            "path", request.getRequestURI(),
            "traceId", generateTraceId()
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(false, "系统繁忙，请稍后重试", errorData));
    }

    /**
     * 生成跟踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}