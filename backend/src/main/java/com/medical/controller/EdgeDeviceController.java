package com.medical.controller;

import com.medical.dto.ApiResponse;
import com.medical.entity.EdgeDeviceData;
import com.medical.service.EdgeDataService;
import com.medical.util.EncryptionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 边缘设备管理控制器
 */
@Tag(name = "边缘设备管理", description = "边缘设备数据和状态管理")
@RestController
@RequestMapping("/edge")
public class EdgeDeviceController {

    private final EdgeDataService edgeDataService;
    private final EncryptionUtil encryptionUtil;
    
    public EdgeDeviceController(EdgeDataService edgeDataService, EncryptionUtil encryptionUtil) {
        this.edgeDataService = edgeDataService;
        this.encryptionUtil = encryptionUtil;
    }

    /**
     * 获取边缘设备状态列表
     */
    @Operation(summary = "获取边缘设备状态")
    @GetMapping("/devices/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDeviceStatus() {
        List<Map<String, Object>> devices = edgeDataService.getEdgeDeviceStatus();
        
        // 🔒 数据脱敏处理
        devices.forEach(device -> {
            if (device.containsKey("patientName") && device.get("patientName") != null) {
                device.put("patientName", encryptionUtil.maskName((String) device.get("patientName")));
            }
            if (device.containsKey("patientPhone") && device.get("patientPhone") != null) {
                device.put("patientPhone", encryptionUtil.maskPhone((String) device.get("patientPhone")));
            }
            if (device.containsKey("patientIdCard") && device.get("patientIdCard") != null) {
                device.put("patientIdCard", encryptionUtil.maskIdCard((String) device.get("patientIdCard")));
            }
        });
        
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    /**
     * 获取边缘数据统计
     */
    @Operation(summary = "获取边缘数据统计")
    @GetMapping("/data/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE', 'DOCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDataStatistics() {
        Map<String, Object> stats = edgeDataService.getEdgeDataStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取未处理的边缘数据
     */
    @Operation(summary = "获取未处理数据")
    @GetMapping("/data/unprocessed")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE')")
    public ResponseEntity<ApiResponse<Page<EdgeDeviceData>>> getUnprocessedData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<EdgeDeviceData> data = edgeDataService.getUnprocessedData(pageable);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 根据设备ID获取数据
     */
    @Operation(summary = "获取设备数据")
    @GetMapping("/data/device/{deviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE')")
    public ResponseEntity<ApiResponse<Page<EdgeDeviceData>>> getDataByDevice(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<EdgeDeviceData> data = edgeDataService.getDataByDevice(deviceId, pageable);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 获取数据质量报告
     */
    @Operation(summary = "获取数据质量报告")
    @GetMapping("/data/quality-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDataQualityReport() {
        Map<String, Object> report = edgeDataService.getDataQualityReport();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * 重新处理失败数据
     */
    @Operation(summary = "重新处理失败数据")
    @PostMapping("/data/reprocess-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> reprocessFailedData() {
        edgeDataService.reprocessFailedData();
        return ResponseEntity.ok(ApiResponse.success("失败数据已重新处理"));
    }

    /**
     * 清理过期数据
     */
    @Operation(summary = "清理过期数据")
    @DeleteMapping("/data/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> cleanupExpiredData(
            @RequestParam(defaultValue = "30") int daysToKeep) {
        
        edgeDataService.cleanExpiredData(daysToKeep);
        return ResponseEntity.ok(ApiResponse.success("过期数据清理完成"));
    }

    /**
     * 更新设备状态
     */
    @Operation(summary = "更新设备状态")
    @PostMapping("/devices/{deviceId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateDeviceStatus(
            @PathVariable String deviceId,
            @RequestParam String status,
            @RequestParam(required = false) String errorMessage) {
        
        edgeDataService.updateDeviceStatus(deviceId, status, errorMessage);
        return ResponseEntity.ok(ApiResponse.success("设备状态更新成功"));
    }

    /**
     * 手动标记数据已处理
     */
    @Operation(summary = "标记数据已处理")
    @PostMapping("/data/{dataId}/mark-processed")
    @PreAuthorize("hasAnyRole('ADMIN', 'NURSE')")
    public ResponseEntity<ApiResponse<String>> markDataProcessed(@PathVariable Long dataId) {
        edgeDataService.markAsProcessed(dataId);
        return ResponseEntity.ok(ApiResponse.success("数据已标记为已处理"));
    }
}