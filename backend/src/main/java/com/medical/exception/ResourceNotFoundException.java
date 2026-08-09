package com.medical.exception;

/**
 * 🛡️ 资源未找到异常
 * 当请求的资源不存在时抛出此异常
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, org.springframework.http.HttpStatus.NOT_FOUND);
    }
    
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super("RESOURCE_NOT_FOUND", 
              String.format("%s not found with id: %s", resourceType, resourceId),
              org.springframework.http.HttpStatus.NOT_FOUND);
    }
    
    public static BusinessException of(String message) {
        return new ResourceNotFoundException(message);
    }
    
    public static BusinessException of(String resourceType, Object resourceId) {
        return new ResourceNotFoundException(resourceType, resourceId);
    }
}