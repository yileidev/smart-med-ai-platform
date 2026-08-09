package com.medical.controller;

import com.medical.dto.ApiResponse;
import com.medical.entity.EdgeDeviceData;
import com.medical.repository.EdgeDeviceDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 边缘设备数据查询控制器
 * 提供REST API接口供前端调用
 */
@SuppressWarnings("unused") // REST API端点，通过HTTP请求调用
@RestController
@RequestMapping("/edge-data")
public class EdgeDataController {
    
    private static final Logger log = LoggerFactory.getLogger(EdgeDataController.class);
    
    private final EdgeDeviceDataRepository edgeDataRepository;
    
    public EdgeDataController(EdgeDeviceDataRepository edgeDataRepository) {
        this.edgeDataRepository = edgeDataRepository;
    }
    
    /**
     * 根据ID获取边缘数据详情
     */
    @GetMapping("/{id}")
    public ApiResponse<?> getEdgeDataById(@PathVariable Long id) {
        try {
            EdgeDeviceData edgeData = edgeDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("边缘数据不存在，ID: " + id));
            
            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("id", edgeData.getId());
            result.put("deviceId", edgeData.getDeviceId());
            result.put("patientName", edgeData.getPatientName());
            result.put("patientAge", edgeData.getPatientAge());
            result.put("patientGender", edgeData.getPatientGender());
            result.put("patientIdCard", edgeData.getPatientIdCard());
            result.put("patientPhone", edgeData.getPatientPhone());
            result.put("voiceComplaint", edgeData.getVoiceComplaint());
            result.put("vitalSigns", edgeData.getVitalSigns());
            // 单独显示血压数据以便前端直接使用
            result.put("systolicBP", edgeData.getSystolicBP());
            result.put("diastolicBP", edgeData.getDiastolicBP());
            result.put("triageLevel", edgeData.getTriageLevel());
            result.put("triagePriority", edgeData.getTriagePriority());
            result.put("triageColor", edgeData.getTriageColor());
            result.put("waitTime", edgeData.getWaitTime());
            result.put("triageScore", edgeData.getTriageScore());
            result.put("aiDiagnosis", edgeData.getAiDiagnosis());
            result.put("aiConfidence", edgeData.getAiConfidence());
            result.put("processingStatus", edgeData.getProcessingStatus());
            result.put("createdAt", edgeData.getCreatedAt());
            result.put("updatedAt", edgeData.getUpdatedAt());
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("获取边缘数据失败", e);
            return ApiResponse.error("获取失败: " + e.getMessage());
        }
    }
}
