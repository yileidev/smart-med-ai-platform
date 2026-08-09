package com.medical.controller;

import com.medical.entity.TriageRecord;
import com.medical.service.TriageConfirmationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分诊确认控制器
 * 处理护士确认边缘AI分诊结果的相关功能
 */
@SuppressWarnings("unused") // REST API端点
@RestController
@RequestMapping("/triage-confirmation")
@Tag(name = "分诊确认", description = "护士确认边缘AI分诊结果")
public class TriageConfirmationController {

    private final TriageConfirmationService triageConfirmationService;
    
    public TriageConfirmationController(TriageConfirmationService triageConfirmationService) {
        this.triageConfirmationService = triageConfirmationService;
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待确认的分诊记录")
    @PreAuthorize("hasRole('NURSE') or hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPendingRecords(Pageable pageable) {
        try {
            Page<TriageRecord> pendingRecords = triageConfirmationService.getPendingConfirmationRecords(pageable);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "records", pendingRecords.getContent(),
                "totalElements", pendingRecords.getTotalElements(),
                "totalPages", pendingRecords.getTotalPages(),
                "currentPage", pendingRecords.getNumber(),
                "message", "获取待确认记录成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "获取待确认记录失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{recordId}/confirm")
    @Operation(summary = "确认分诊结果")
    @PreAuthorize("hasRole('NURSE') or hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> confirmTriage(
            @PathVariable Long recordId,
            @RequestBody TriageConfirmationRequest request) {
        try {
            Map<String, Object> result = triageConfirmationService.confirmTriage(recordId, request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "result", result,
                "message", "分诊确认成功，已提交到云端大模型诊断"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "分诊确认失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{recordId}/reject")
    @Operation(summary = "拒绝分诊结果，要求重新分诊")
    @PreAuthorize("hasRole('NURSE') or hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> rejectTriage(
            @PathVariable Long recordId,
            @RequestBody TriageRejectionRequest request) {
        try {
            triageConfirmationService.rejectTriage(recordId, request);


            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已提交重新分诊请求"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "拒绝分诊失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{recordId}/detail")
    @Operation(summary = "获取分诊记录详情")
    @PreAuthorize("hasRole('NURSE') or hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTriageDetail(@PathVariable Long recordId) {
        try {
            TriageRecord record = triageConfirmationService.getTriageRecordDetail(recordId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "record", record,
                "message", "获取详情成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "获取详情失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "获取分诊确认统计信息")
    @PreAuthorize("hasRole('NURSE') or hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTriageStats() {
        try {
            Map<String, Object> stats = triageConfirmationService.getTriageConfirmationStats();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "stats", stats,
                "message", "获取统计信息成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "获取统计信息失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/batch-confirm")
    @Operation(summary = "批量确认分诊")
    @PreAuthorize("hasRole('NURSE') or hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> batchConfirm(@RequestBody BatchConfirmRequest request) {
        try {
            List<TriageRecord> confirmedRecords = triageConfirmationService.batchConfirmTriage(request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "confirmedCount", confirmedRecords.size(),
                "records", confirmedRecords,
                "message", "批量确认成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "批量确认失败: " + e.getMessage()
            ));
        }
    }

    // 请求数据类
    public static class TriageConfirmationRequest {
        private Integer confirmedTriageLevel;
        private String nurseComments;
        private String updatedChiefComplaint;
        private PatientInfoUpdate patientInfo;
        private String updatedVitalSigns;

        // Getters and Setters
        public Integer getConfirmedTriageLevel() { return confirmedTriageLevel; }
        public void setConfirmedTriageLevel(Integer confirmedTriageLevel) { this.confirmedTriageLevel = confirmedTriageLevel; }

        public String getNurseComments() { return nurseComments; }
        public void setNurseComments(String nurseComments) { this.nurseComments = nurseComments; }
        
        public String getUpdatedChiefComplaint() { return updatedChiefComplaint; }
        public void setUpdatedChiefComplaint(String updatedChiefComplaint) { this.updatedChiefComplaint = updatedChiefComplaint; }

        public PatientInfoUpdate getPatientInfo() { return patientInfo; }
        public void setPatientInfo(PatientInfoUpdate patientInfo) { this.patientInfo = patientInfo; }

        public String getUpdatedVitalSigns() { return updatedVitalSigns; }
        public void setUpdatedVitalSigns(String updatedVitalSigns) { this.updatedVitalSigns = updatedVitalSigns; }
    }

    public static class PatientInfoUpdate {
        private String name;
        private Integer age;
        private String gender;
        private String phoneNumber;
        private String idCard;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getIdCard() { return idCard; }
        public void setIdCard(String idCard) { this.idCard = idCard; }
    }

    public static class TriageRejectionRequest {
        private String reason;
        private String updatedChiefComplaint;

        // Getters and Setters
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getUpdatedChiefComplaint() { return updatedChiefComplaint; }
        public void setUpdatedChiefComplaint(String updatedChiefComplaint) { this.updatedChiefComplaint = updatedChiefComplaint; }
    }

    public static class BatchConfirmRequest {
        private List<Long> recordIds;
        private String batchComments;

        // Getters and Setters
        public List<Long> getRecordIds() { return recordIds; }
        public void setRecordIds(List<Long> recordIds) { this.recordIds = recordIds; }

        public String getBatchComments() { return batchComments; }
        public void setBatchComments(String batchComments) { this.batchComments = batchComments; }
    }
}