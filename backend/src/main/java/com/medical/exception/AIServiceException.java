package com.medical.exception;

/**
 * 🛡️ AI服务异常
 * 当AI诊断服务出现问题时抛出此异常
 */
public class AIServiceException extends BusinessException {
    
    public AIServiceException(String message) {
        super("AI_SERVICE_ERROR", message, org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    public AIServiceException(String message, Throwable cause) {
        super("AI_SERVICE_ERROR", message, cause, org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    public static BusinessException of(String message) {
        return new AIServiceException(message);
    }
    
    public static BusinessException of(String message, Throwable cause) {
        return new AIServiceException(message, cause);
    }
}