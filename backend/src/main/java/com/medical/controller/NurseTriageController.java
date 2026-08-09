package com.medical.controller;

import com.medical.dto.ApiResponse;
import com.medical.dto.NurseCorrectionRequest;
import com.medical.entity.NurseCorrectionRecord;
import com.medical.service.NurseTriageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 护士分诊确认控制器
 */
@SuppressWarnings("unused") // REST API端点
@RestController
@RequestMapping("/triage")
public class NurseTriageController {
    
    private static final Logger log = LoggerFactory.getLogger(NurseTriageController.class);
    
    private final NurseTriageService nurseTriageService;
    
    public NurseTriageController(NurseTriageService nurseTriageService) {
        this.nurseTriageService = nurseTriageService;
    }
    
    /**
     * 护士复核处理（统一接口，根据action决定流程）
     * action = "SEND_TO_EDGE": 发回边缘端重新分诊
     * action = "CONFIRM_TO_CLOUD": 确认无误，提交到云端大模型
     */
    @PostMapping("/nurse-review")
    public ApiResponse<Map<String, Object>> submitNurseReview(
            @RequestBody NurseCorrectionRequest request) {
        try {
            log.info("收到护士复核请求 - 操作: {}, 边缘数据ID: {}, 护士: {}", 
                request.getAction(), request.getEdgeDataId(), request.getNurseName());
            
            // 验证请求参数
            if (request.getEdgeDataId() == null) {
                return ApiResponse.error("边缘数据ID不能为空");
            }
            if (request.getAction() == null || request.getAction().trim().isEmpty()) {
                return ApiResponse.error("操作类型不能为空");
            }
            if (request.getCorrectedSensorData() == null || request.getCorrectedSensorData().isEmpty()) {
                return ApiResponse.error("生理参数不能为空");
            }
            if (request.getCorrectedChiefComplaint() == null || request.getCorrectedChiefComplaint().trim().isEmpty()) {
                return ApiResponse.error("语音主诉不能为空");
            }
            
            Map<String, Object> result;
            
            // 根据操作类型选择处理流程
            if ("SEND_TO_EDGE".equals(request.getAction())) {
                // 流程1：发回边缘端重新分诊
                result = nurseTriageService.sendCorrectionToEdge(request);
                
                if ((Boolean) result.get("success")) {
                    return ApiResponse.success("修正数据已发送到边缘端，等待重新分诊结果", result);
                } else {
                    return ApiResponse.error((String) result.get("message"));
                }
                
            } else if ("CONFIRM_TO_CLOUD".equals(request.getAction())) {
                // 流程2：确认无误，提交到云端大模型
                result = nurseTriageService.confirmAndSubmitToCloud(request);
                
                if ((Boolean) result.get("success")) {
                    return ApiResponse.success("已提交云端诊断大模型，正在处理中...", result);
                } else {
                    return ApiResponse.error((String) result.get("message"));
                }
                
            } else {
                return ApiResponse.error("无效的操作类型: " + request.getAction());
            }
            
        } catch (Exception e) {
            log.error("处理护士复核请求失败", e);
            return ApiResponse.error("处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取待复核患者列表
     */
    @GetMapping("/nurse-pending-list")
    public ApiResponse<?> getPendingPatientList() {
        try {
            List<Map<String, Object>> patientList = nurseTriageService.getPendingPatientList();
            return ApiResponse.success(patientList);
        } catch (Exception e) {
            log.error("获取待复核患者列表失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取护士工作台统计数据
     */
    @GetMapping("/nurse-statistics")
    public ApiResponse<?> getNurseStatistics() {
        try {
            Map<String, Object> statistics = nurseTriageService.getNurseStatistics();
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取边缘数据的修正历史
     */
    @GetMapping("/corrections/edge-data/{edgeDataId}")
    public ApiResponse<List<NurseCorrectionRecord>> getCorrectionsByEdgeData(
            @PathVariable Long edgeDataId) {
        try {
            List<NurseCorrectionRecord> corrections = 
                nurseTriageService.getCorrectionsByEdgeDataId(edgeDataId);
            return ApiResponse.success(corrections);
        } catch (Exception e) {
            log.error("获取修正记录失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取护士的修正历史
     */
    @GetMapping("/corrections/nurse/{nurseId}")
    public ApiResponse<List<NurseCorrectionRecord>> getCorrectionsByNurse(
            @PathVariable Long nurseId) {
        try {
            List<NurseCorrectionRecord> corrections = 
                nurseTriageService.getCorrectionsByNurseId(nurseId);
            return ApiResponse.success(corrections);
        } catch (Exception e) {
            log.error("获取修正记录失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }
}
