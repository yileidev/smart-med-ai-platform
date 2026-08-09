package com.medical.controller;

import com.medical.dto.ApiResponse;
import com.medical.entity.Patient;
import com.medical.entity.EdgeDeviceData;
import com.medical.service.TriageService;
import com.medical.service.EdgeDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 急诊分诊控制器
 */
@SuppressWarnings("unused") // REST API端点
@RestController
@RequestMapping("/triage")
public class TriageController {
    
    @Autowired
    private TriageService triageService;
    
    @Autowired
    private EdgeDataService edgeDataService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 接收边缘端数据 (MQTT/HTTP接口)
     */
    @PostMapping("/edge-data")
    public ApiResponse<String> receiveEdgeData(@RequestBody EdgeDeviceData edgeData) {
        try {
            // 保存边缘设备数据
            edgeDataService.saveEdgeData(edgeData);
            
            // 异步处理分诊逻辑
            triageService.processTriageAsync(edgeData);
            
            // 实时推送到前端
            messagingTemplate.convertAndSend("/topic/new-patient", edgeData);
            
            return ApiResponse.success("边缘数据接收成功");
        } catch (Exception e) {
            return ApiResponse.error("数据处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取待分诊患者列表 (护士界面)
     */
    @GetMapping("/pending-patients")
    public ApiResponse<List<Patient>> getPendingPatients() {
        List<Patient> patients = triageService.getPendingPatients();
        return ApiResponse.success(patients);
    }
    
    /**
     * 护士确认分诊结果
     */
    @PostMapping("/confirm-triage")
    public ApiResponse<String> confirmTriage(@RequestBody Map<String, Object> request) {
        try {
            Object patientIdObj = request.get("patientId");
            Object triageLevelObj = request.get("triageLevel");
            
            if (patientIdObj == null) {
                return ApiResponse.error("分诊确认失败: 患者ID不能为空");
            }
            if (triageLevelObj == null) {
                return ApiResponse.error("分诊确认失败: 分诊等级不能为空");
            }
            
            Long patientId = Long.valueOf(patientIdObj.toString());
            Integer confirmedLevel = Integer.valueOf(triageLevelObj.toString());
            String nurseNotes = request.get("nurseNotes") != null ? request.get("nurseNotes").toString() : "";
            
            triageService.confirmTriage(patientId, confirmedLevel, nurseNotes);
            
            // 推送更新到前端
            messagingTemplate.convertAndSend("/topic/triage-confirmed", patientId);
            
            return ApiResponse.success("分诊确认成功");
        } catch (Exception e) {
            return ApiResponse.error("分诊确认失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取待诊断患者列表 (医生界面)
     */
    @GetMapping("/diagnosis-queue")
    public ApiResponse<List<Patient>> getDiagnosisQueue(@RequestParam(required = false) String department) {
        List<Patient> patients = triageService.getDiagnosisQueue(department);
        return ApiResponse.success(patients);
    }
    
    /**
     * 医生提交诊断
     */
    @PostMapping("/submit-diagnosis")
    public ApiResponse<String> submitDiagnosis(@RequestBody Map<String, Object> request) {
        try {
            Object patientIdObj = request.get("patientId");
            Object doctorIdObj = request.get("doctorId");
            
            if (patientIdObj == null) {
                return ApiResponse.error("诊断提交失败: 患者ID不能为空");
            }
            if (doctorIdObj == null) {
                return ApiResponse.error("诊断提交失败: 医生ID不能为空");
            }
            
            Long patientId = Long.valueOf(patientIdObj.toString());
            String diagnosis = request.get("diagnosis") != null ? request.get("diagnosis").toString() : "";
            String treatment = request.get("treatment") != null ? request.get("treatment").toString() : "";
            Long doctorId = Long.valueOf(doctorIdObj.toString());
            
            triageService.submitDiagnosis(patientId, diagnosis, treatment, doctorId);
            
            // 推送更新
            messagingTemplate.convertAndSend("/topic/diagnosis-completed", patientId);
            
            return ApiResponse.success("诊断提交成功");
        } catch (Exception e) {
            return ApiResponse.error("诊断提交失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取AI诊断建议
     */
    @PostMapping("/ai-diagnosis")
    public ApiResponse<Map<String, Object>> getAIDiagnosis(@RequestBody Map<String, Object> request) {
        try {
            Object patientIdObj = request.get("patientId");
            if (patientIdObj == null) {
                return ApiResponse.error("AI诊断失败: 患者ID不能为空");
            }
            Long patientId = Long.valueOf(patientIdObj.toString());
            Map<String, Object> aiResult = triageService.getAIDiagnosis(patientId);
            return ApiResponse.success(aiResult);
        } catch (Exception e) {
            return ApiResponse.error("AI诊断失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取资源调度建议
     */
    @GetMapping("/resource-allocation/{patientId}")
    public ApiResponse<Map<String, Object>> getResourceAllocation(@PathVariable Long patientId) {
        try {
            Map<String, Object> allocation = triageService.getResourceAllocation(patientId);
            return ApiResponse.success(allocation);
        } catch (Exception e) {
            return ApiResponse.error("资源调度失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取实时分诊统计
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getTriageStatistics() {
        Map<String, Object> stats = triageService.getTriageStatistics();
        return ApiResponse.success(stats);
    }
    
    /**
     * 获取患者详细信息
     */
    @GetMapping("/patient/{patientId}")
    public ApiResponse<Patient> getPatientDetail(@PathVariable Long patientId) {
        Patient patient = triageService.getPatientById(patientId);
        if (patient != null) {
            return ApiResponse.success(patient);
        } else {
            return ApiResponse.error("患者不存在");
        }
    }
    
    /**
     * 更新患者状态
     */
    @PostMapping("/update-status")
    public ApiResponse<String> updatePatientStatus(@RequestBody Map<String, Object> request) {
        try {
            Object patientIdObj = request.get("patientId");
            if (patientIdObj == null) {
                return ApiResponse.error("状态更新失败: 患者ID不能为空");
            }
            Long patientId = Long.valueOf(patientIdObj.toString());
            String status = request.get("status") != null ? request.get("status").toString() : "";
            
            triageService.updatePatientStatus(patientId, status);
            
            // 推送状态更新
            messagingTemplate.convertAndSend("/topic/status-updated", 
                Map.of("patientId", patientId, "status", status));
            
            return ApiResponse.success("状态更新成功");
        } catch (Exception e) {
            return ApiResponse.error("状态更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取边缘设备状态
     */
    @GetMapping("/edge-devices")
    public ApiResponse<List<Map<String, Object>>> getEdgeDeviceStatus() {
        List<Map<String, Object>> devices = edgeDataService.getEdgeDeviceStatus();
        return ApiResponse.success(devices);
    }
}
