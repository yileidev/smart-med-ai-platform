package com.medical.exception;

/**
 * 🛡️ MQTT消息异常
 * 当MQTT通信出现问题时抛出此异常
 */
public class MQTTException extends BusinessException {
    
    public MQTTException(String message) {
        super("MQTT_ERROR", message, org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    public MQTTException(String message, Throwable cause) {
        super("MQTT_ERROR", message, cause, org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    public static BusinessException of(String message) {
        return new MQTTException(message);
    }
    
    public static BusinessException of(String message, Throwable cause) {
        return new MQTTException(message, cause);
    }
}