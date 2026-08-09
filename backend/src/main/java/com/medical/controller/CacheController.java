package com.medical.controller;

import com.medical.util.RedisCacheUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理接口
 */
@SuppressWarnings("unused") // REST API端点
@Tag(name = "缓存管理", description = "缓存管理接口")
@RestController
@RequestMapping("/cache")
public class CacheController {
    
    private static final Logger log = LoggerFactory.getLogger(CacheController.class);

    private final CacheManager cacheManager;
    
    @Autowired(required = false)
    private RedisCacheUtil redisCacheUtil;

    public CacheController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Operation(summary = "获取缓存统计信息")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        if (redisCacheUtil != null) {
            stats.put("stats", redisCacheUtil.getCacheStats());
            stats.put("type", "Redis");
        } else {
            stats.put("stats", "使用内存缓存");
            stats.put("type", "ConcurrentMap");
        }
        
        stats.put("cacheNames", cacheManager.getCacheNames());
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "清除指定缓存")
    @DeleteMapping("/clear/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearCache(@PathVariable String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("清除缓存: {}", cacheName);
                return ResponseEntity.ok(Map.of("success", true, "message", "缓存清除成功"));
            }
            return ResponseEntity.ok(Map.of("success", false, "message", "缓存不存在"));
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "清除失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "清除所有缓存")
    @DeleteMapping("/clear-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearAllCache() {
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
            log.info("清除所有缓存");
            return ResponseEntity.ok(Map.of("success", true, "message", "所有缓存清除成功"));
        } catch (Exception e) {
            log.error("清除所有缓存失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "清除失败: " + e.getMessage()));
        }
    }
}
