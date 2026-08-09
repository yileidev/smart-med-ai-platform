package com.medical.controller;

import com.medical.dto.ApiResponse;
import com.medical.entity.Consultation;
import com.medical.entity.DiagnosisHistory;
import com.medical.entity.Notification;
import com.medical.entity.OperationLog;
import com.medical.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 综合功能Controller
 * 包含历史记录、通知、会诊、导出等功能
 */
@SuppressWarnings("unused") // REST API端点
@RestController
@RequestMapping("/api")
public class FeatureController {
    
    private static final Logger log = LoggerFactory.getLogger(FeatureController.class);

    private final HistoryService historyService;
    private final NotificationService notificationService;
    private final ConsultationService consultationService;
    private final ExportService exportService;
    private final TriageService triageService;

    public FeatureController(HistoryService historyService,
                             NotificationService notificationService,
                             ConsultationService consultationService,
                             ExportService exportService,
                             TriageService triageService) {
        this.historyService = historyService;
        this.notificationService = notificationService;
        this.consultationService = consultationService;
        this.exportService = exportService;
        this.triageService = triageService;
    }

    // ==================== 诊断历史 ====================

    @GetMapping("/history/diagnosis/patient/{patientId}")
    public ApiResponse<List<DiagnosisHistory>> getPatientDiagnosisHistory(@PathVariable Long patientId) {
        return ApiResponse.success(historyService.getPatientDiagnosisHistory(patientId));
    }

    @GetMapping("/history/diagnosis/doctor/{doctorId}")
    public ApiResponse<Page<DiagnosisHistory>> getDoctorDiagnosisHistory(
            @PathVariable Long doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(historyService.getDoctorDiagnosisHistory(doctorId, page, size));
    }

    @GetMapping("/history/diagnosis/range")
    public ApiResponse<Page<DiagnosisHistory>> getDiagnosisHistoryByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(
            historyService.getDiagnosisHistoryByTimeRange(startTime, endTime, page, size));
    }

    // ==================== 操作日志 ====================

    @GetMapping("/logs/user/{userId}")
    public ApiResponse<Page<OperationLog>> getUserLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(historyService.getUserOperationLogs(userId, page, size));
    }

    @GetMapping("/logs/range")
    public ApiResponse<Page<OperationLog>> getLogsByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
            historyService.getOperationLogsByTimeRange(startTime, endTime, page, size));
    }

    @GetMapping("/logs/type/{type}")
    public ApiResponse<Page<OperationLog>> getLogsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(historyService.getOperationLogsByType(type, page, size));
    }

    // ==================== 通知 ====================

    @GetMapping("/notifications/unread")
    public ApiResponse<List<Notification>> getUnreadNotifications(Authentication authentication) {
        String username = authentication.getName();
        return ApiResponse.success(notificationService.getUnreadNotifications(username));
    }

    @GetMapping("/notifications/count")
    public ApiResponse<Long> getUnreadCount(Authentication authentication) {
        String username = authentication.getName();
        return ApiResponse.success(notificationService.getUnreadCount(username));
    }

    @PostMapping("/notifications/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/notifications/read-all")
    public ApiResponse<Void> markAllAsRead(Authentication authentication) {
        String username = authentication.getName();
        notificationService.markAllAsRead(username);
        return ApiResponse.success(null);
    }

    // ==================== 会诊 ====================

    @PostMapping("/consultation")
    public ApiResponse<Consultation> createConsultation(@RequestBody Consultation consultation) {
        return ApiResponse.success(consultationService.createConsultation(consultation));
    }

    @PostMapping("/consultation/{id}/accept")
    public ApiResponse<Consultation> acceptConsultation(@PathVariable Long id) {
        return ApiResponse.success(consultationService.acceptConsultation(id));
    }

    @PostMapping("/consultation/{id}/complete")
    public ApiResponse<Consultation> completeConsultation(
            @PathVariable Long id,
            @RequestParam String opinion) {
        return ApiResponse.success(consultationService.completeConsultation(id, opinion));
    }

    @GetMapping("/consultation/doctor/{doctorId}")
    public ApiResponse<List<Consultation>> getDoctorConsultations(@PathVariable Long doctorId) {
        return ApiResponse.success(consultationService.getDoctorConsultations(doctorId));
    }

    @GetMapping("/consultation/pending")
    public ApiResponse<List<Consultation>> getPendingConsultations() {
        return ApiResponse.success(consultationService.getPendingConsultations());
    }

    // ==================== 导出 ====================

    @GetMapping("/export/diagnosis-history")
    public ResponseEntity<byte[]> exportDiagnosisHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            Page<DiagnosisHistory> page = historyService.getDiagnosisHistoryByTimeRange(
                startTime, endTime, 0, 10000);
            byte[] data = exportService.exportDiagnosisHistoryToExcel(page.getContent());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "diagnosis_history_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            log.error("导出诊断历史失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/operation-logs")
    public ResponseEntity<byte[]> exportOperationLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            Page<OperationLog> page = historyService.getOperationLogsByTimeRange(
                startTime, endTime, 0, 10000);
            byte[] data = exportService.exportOperationLogsToExcel(page.getContent());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "operation_logs_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            log.error("导出操作日志失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/triage-records")
    public ResponseEntity<byte[]> exportTriageRecords(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            // 获取时间范围内的分诊记录
            var records = triageService.getTriageRecordsByTimeRange(startTime, endTime);
            byte[] data = exportService.exportTriageRecordsToExcel(records);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "triage_records_" + System.currentTimeMillis() + ".xlsx");
            
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            log.error("导出分诊记录失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
