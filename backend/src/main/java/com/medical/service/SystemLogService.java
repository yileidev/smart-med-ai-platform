package com.medical.service;

import com.medical.entity.SystemLog;
import com.medical.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SystemLogService {
    
    private final SystemLogRepository logRepository;
    
    public Page<SystemLog> findLogs(SystemLog.LogLevel level,
                                  String action,
                                  String userName,
                                  LocalDateTime startTime,
                                  LocalDateTime endTime,
                                  Pageable pageable) {
        return logRepository.findByConditions(level, action, userName, startTime, endTime, pageable);
    }
    
    @Transactional
    public void log(Long userId, String userName, String action, String resourceType, 
                   String resourceId, String details, String ipAddress, String userAgent) {
        SystemLog log = new SystemLog();
        log.setUserId(userId);
        log.setUserName(userName);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setLevel(SystemLog.LogLevel.INFO);
        
        logRepository.save(log);
    }
    
    @Transactional
    public void logError(Long userId, String userName, String action, String details, 
                        String ipAddress, String userAgent) {
        SystemLog log = new SystemLog();
        log.setUserId(userId);
        log.setUserName(userName);
        log.setAction(action);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setLevel(SystemLog.LogLevel.ERROR);
        
        logRepository.save(log);
    }
    
    public long countByLevel(SystemLog.LogLevel level) {
        return logRepository.countByLevel(level);
    }
    
    public long countTodayLogs() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return logRepository.countByCreatedAtAfter(startOfDay);
    }
    
    @Transactional
    public void logUserAction(Long userId, String userName, String action, 
                             String resourceType, String resourceId, String details) {
        SystemLog log = new SystemLog();
        log.setUserId(userId);
        log.setUserName(userName);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetails(details);
        log.setLevel(SystemLog.LogLevel.INFO);
        
        logRepository.save(log);
    }
}