package com.medical.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 简单缓存配置（当Redis不可用时使用）
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "none", matchIfMissing = true)
public class SimpleCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        log.warn("⚠️ Redis不可用，使用内存缓存（ConcurrentMap）作为降级方案");
        return new ConcurrentMapCacheManager(
            "triageStats", 
            "patientList", 
            "aiDiagnosis", 
            "medicalKnowledge"
        );
    }
}
