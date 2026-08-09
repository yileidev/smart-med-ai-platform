package com.medical.controller;

import com.medical.dto.ApiResponse;
import com.medical.entity.DiagnosisRecord;
import com.medical.entity.Patient;
import com.medical.entity.TriageRecord;
import com.medical.entity.User;
import com.medical.entity.ResourceAllocation;
import com.medical.service.DiagnosisService;
import com.medical.service.TriageService;
import com.medical.service.UserService;
import com.medical.service.MultimodalDiagnosisService;
import com.medical.service.MedicalResourceSchedulingService;
import com.medical.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused") // REST API端点
@Tag(name = "医生工作台", description = "医生端功能接口")
@RestController
@RequestMapping("/doctor")
public class DoctorController {
    
    private static final Logger log = LoggerFactory.getLogger(DoctorController.class);
    
    private final TriageService triageService;
    private final DiagnosisService diagnosisService;
    private final UserService userService;
    private final MultimodalDiagnosisService multimodalDiagnosisService;
    private final MedicalResourceSchedulingService resourceSchedulingService;
    private final EncryptionUtil encryptionUtil;
    
    public DoctorController(TriageService triageService,
                           DiagnosisService diagnosisService,
                           UserService userService,
                           MultimodalDiagnosisService multimodalDiagnosisService,
                           MedicalResourceSchedulingService resourceSchedulingService,
                           EncryptionUtil encryptionUtil) {
        this.triageService = triageService;
        this.diagnosisService = diagnosisService;
        this.userService = userService;
        this.multimodalDiagnosisService = multimodalDiagnosisService;
        this.resourceSchedulingService = resourceSchedulingService;
        this.encryptionUtil = encryptionUtil;
    }
    
    @Operation(summary = "获取医生工作台统计数据")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDoctorStats(Principal principal) {
        try {
            User doctor = userService.findByUsername(principal.getName());
            
            // 基础统计
            Map<String, Object> stats = new HashMap<>();
            
            try {
                stats.put("urgentPatients", triageService.countByTriageLevelAndStatus(1, TriageRecord.TriageStatus.WAITING));
            } catch (Exception e) {
                log.warn("统计紧急患者失败", e);
                stats.put("urgentPatients", 0L);
            }
            
            try {
                stats.put("pendingPatients", triageService.countByStatus(TriageRecord.TriageStatus.CONFIRMED));
            } catch (Exception e) {
                log.warn("统计待诊患者失败", e);
                stats.put("pendingPatients", 0L);
            }
            
            try {
                stats.put("completedToday", diagnosisService.countTodayCompleted());
            } catch (Exception e) {
                log.warn("统计今日完成失败", e);
                stats.put("completedToday", 0L);
            }
            
            try {
                stats.put("inProgressPatients", triageService.countByStatus(TriageRecord.TriageStatus.IN_PROGRESS));
            } catch (Exception e) {
                log.warn("统计诊疗中患者失败", e);
                stats.put("inProgressPatients", 0L);
            }
            
            // 分诊等级分布（饱饼图数据）
            Map<String, Object> triageLevelDistribution = new HashMap<>();
            try {
                triageLevelDistribution.put("Ⅰ级-急危", triageService.countByTriageLevel(1));
                triageLevelDistribution.put("Ⅱ级-急重", triageService.countByTriageLevel(2));
                triageLevelDistribution.put("Ⅲ级-急症", triageService.countByTriageLevel(3));
                triageLevelDistribution.put("Ⅳ级-亚急症", triageService.countByTriageLevel(4));
            } catch (Exception e) {
                log.warn("统计分诊等级分布失败", e);
                triageLevelDistribution.put("Ⅰ级-急危", 0L);
                triageLevelDistribution.put("Ⅱ级-急重", 0L);
                triageLevelDistribution.put("Ⅲ级-急症", 0L);
                triageLevelDistribution.put("Ⅳ级-亚急症", 0L);
            }
            stats.put("triageLevelDistribution", triageLevelDistribution);
            
            // 近七天诊断趋势（折线图数据）
            List<Map<String, Object>> weeklyTrend = new ArrayList<>();
            try {
                for (int i = 6; i >= 0; i--) {
                    Map<String, Object> dayData = new HashMap<>();
                    java.time.LocalDate date = java.time.LocalDate.now().minusDays(i);
                    dayData.put("date", date.toString());
                    dayData.put("count", triageService.countByDate(date));
                    weeklyTrend.add(dayData);
                }
            } catch (Exception e) {
                log.warn("统计近七天趋势失败", e);
            }
            stats.put("weeklyTrend", weeklyTrend);
            
            // 科室分布（柱状图数据）
            Map<String, Object> departmentDistribution = new HashMap<>();
            try {
                departmentDistribution.put("急诊科", triageService.countByDepartment("急诊科"));
                departmentDistribution.put("心内科", triageService.countByDepartment("心内科"));
                departmentDistribution.put("呼吸内科", triageService.countByDepartment("呼吸内科"));
                departmentDistribution.put("消化内科", triageService.countByDepartment("消化内科"));
                departmentDistribution.put("神经内科", triageService.countByDepartment("神经内科"));
                departmentDistribution.put("骨科", triageService.countByDepartment("骨科"));
            } catch (Exception e) {
                log.warn("统计科室分布失败", e);
            }
            stats.put("departmentDistribution", departmentDistribution);
            
            // AI诊断使用统计
            try {
                stats.put("aiDiagnosisUsed", triageService.countAIDiagnosisUsed());
                stats.put("totalDiagnosis", diagnosisService.countTodayCompleted());
            } catch (Exception e) {
                log.warn("统计AI诊断使用失败", e);
                stats.put("aiDiagnosisUsed", 0L);
                stats.put("totalDiagnosis", 0L);
            }
            
            return ResponseEntity.ok(Map.of("success", true, "data", stats));
        } catch (Exception e) {
            log.error("获取医生统计数据失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "获取统计数据失败: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "获取患者队列")
    @GetMapping("/patient-queue")
    public ResponseEntity<Map<String, Object>> getPatientQueue() {
        try {
            // 获取待诊断患者列表 - CONFIRMED 状态表示护士已确认,等待医生诊断
            List<TriageRecord> triageRecords = triageService.findByStatus(
                TriageRecord.TriageStatus.CONFIRMED,
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
            
            log.info("医生端获取患者队列,共 {} 个患者", triageRecords.size());
            
            // 转换为简化的Map格式，避免懒加载问题
            List<Map<String, Object>> patientList = triageRecords.stream()
                .map(record -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", record.getId());
                    item.put("patientId", record.getPatient() != null ? record.getPatient().getId() : null);
                    item.put("patientName", record.getPatient() != null ? record.getPatient().getPatientName() : "未知");
                    item.put("age", record.getPatient() != null ? record.getPatient().getAge() : 0);
                    item.put("gender", record.getPatient() != null ? record.getPatient().getGender() : null);
                    // 证件号解密并脱敏显示
                    String idCard = record.getPatient() != null ? record.getPatient().getIdCard() : null;
                    String maskedIdCard = decryptAndMaskIdCard(idCard);
                    item.put("idCard", maskedIdCard);
                    item.put("idNumber", maskedIdCard);
                    // 添加patient子对象，方便前端访问
                    if (record.getPatient() != null) {
                        Map<String, Object> patientInfo = new HashMap<>();
                        patientInfo.put("id", record.getPatient().getId());
                        patientInfo.put("patientName", record.getPatient().getPatientName());
                        patientInfo.put("age", record.getPatient().getAge());
                        patientInfo.put("gender", record.getPatient().getGender());
                        patientInfo.put("idCard", maskedIdCard);
                        patientInfo.put("idNumber", maskedIdCard);
                        item.put("patient", patientInfo);
                    }
                    item.put("chiefComplaint", record.getChiefComplaint());
                    item.put("triageLevel", record.getTriageLevel());
                    item.put("triageScore", record.getTriageScore());
                    item.put("assignedDepartment", record.getAssignedDepartment());
                    item.put("arrivalTime", record.getArrivalTime());
                    item.put("vitalSigns", record.getVitalSigns());
                    item.put("aiDiagnosis", record.getAiDiagnosis());
                    item.put("aiConfidence", record.getAiConfidence());
                    item.put("status", record.getStatus());
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", patientList
            ));
        } catch (Exception e) {
            log.error("获取患者队列失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "获取患者队列失败: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "获取AI诊断建议")
    @GetMapping(value = "/ai-diagnosis/{triageId}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getAIDiagnosis(@PathVariable Long triageId) {
        try {
            log.info("开始获取AI诊断 - triageId: {}", triageId);
            
            TriageRecord triageRecord = triageService.findById(triageId);
            log.info("找到分诊记录 - 患者: {}, 主诉: {}", 
                triageRecord.getPatient() != null ? triageRecord.getPatient().getPatientName() : "无",
                triageRecord.getChiefComplaint());
            
            // 调用多模态AI诊断服务 - 真实调用百川AI
            com.medical.model.DiagnosisResult diagnosisResult = multimodalDiagnosisService.performMultimodalDiagnosis(triageRecord);
            
            log.info("AI诊断完成 - 成功: {}, 主诊断: {}", 
                diagnosisResult.isSuccess(), diagnosisResult.getPrimaryDiagnosis());
            
            Map<String, Object> aiData = new HashMap<>();
            aiData.put("symptomAnalysis", diagnosisResult.getSymptomAnalysis());
            aiData.put("possibleDiagnoses", diagnosisResult.getDifferentialDiagnosis());
            aiData.put("recommendedExams", diagnosisResult.getRecommendedExams());
            aiData.put("confidence", diagnosisResult.getConfidence());
            aiData.put("primaryDiagnosis", diagnosisResult.getPrimaryDiagnosis());
            aiData.put("treatmentRecommendation", diagnosisResult.getTreatmentRecommendation());
            aiData.put("urgencyLevel", diagnosisResult.getUrgencyLevel());
            
            // 使用HashMap避免Map.of()的序列化问题
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", aiData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("AI诊断失败 - triageId: {}", triageId, e);
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("symptomAnalysis", "系统暂时无法分析，请稍后重试");
            errorData.put("possibleDiagnoses", List.of("待人工诊断"));
            errorData.put("recommendedExams", List.of("建议完善相关检查"));
            errorData.put("confidence", 0.0);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "AI诊断失败: " + e.getMessage());
            response.put("data", errorData);
            
            return ResponseEntity.ok(response);
        }
    }
    
    @Operation(summary = "提交诊断结果")
    @PostMapping("/diagnosis")
    public ResponseEntity<Map<String, Object>> submitDiagnosis(@RequestBody Map<String, Object> diagnosisData, Principal principal) {
        try {
            Long triageRecordId = ((Number) diagnosisData.get("triageRecordId")).longValue();
            String diagnosis = (String) diagnosisData.get("diagnosis");
            String treatment = (String) diagnosisData.get("treatment");
            
            TriageRecord triageRecord = triageService.findById(triageRecordId);
            User doctor = userService.findByUsername(principal.getName());  // 获取当前登录的医生
            
            DiagnosisRecord diagnosisRecord = new DiagnosisRecord();
            diagnosisRecord.setTriageRecord(triageRecord);
            diagnosisRecord.setDoctor(doctor);  // 设置医生
            diagnosisRecord.setDiagnosis(diagnosis);  // 设置诊断结果
            diagnosisRecord.setTreatmentPlan(treatment);
            diagnosisRecord.setDiagnosisTime(java.time.LocalDateTime.now());
            
            DiagnosisRecord saved = diagnosisService.saveDiagnosis(diagnosisRecord);
            
            // 更新分诊记录 - 设置分诊医生和状态
            triageRecord.setAssignedDoctor(doctor);
            triageRecord.setStatus(TriageRecord.TriageStatus.COMPLETED);
            triageRecord.setConfirmedTime(java.time.LocalDateTime.now());
            triageService.updateTriageRecord(triageRecord);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "诊断提交成功"
            ));
        } catch (Exception e) {
            log.error("提交诊断失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "提交诊断失败: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "获取待诊患者列表")
    @GetMapping("/pending-patients")
    public ResponseEntity<Map<String, Object>> getPendingPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<Patient> pendingPatients = triageService.getPendingPatients();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", pendingPatients,
            "total", pendingPatients.size()
        ));
    }
    
    @Operation(summary = "开始诊断患者")
    @PostMapping("/start-diagnosis/{triageId}")
    public ResponseEntity<Map<String, Object>> startDiagnosis(@PathVariable Long triageId, Principal principal) {
        try {
            User doctor = userService.findByUsername(principal.getName());
            TriageRecord triageRecord = triageService.findById(triageId);
            
            // 分配医生
            triageRecord.setAssignedDoctor(doctor);
            triageService.updateTriageRecord(triageRecord);
            
            // 调用医疗大模型进行诊断
            com.medical.model.DiagnosisResult diagnosisResult = multimodalDiagnosisService.performMultimodalDiagnosis(triageRecord);
            
            // 转换为Map以供前端使用
            Map<String, Object> aiAnalysis = convertDiagnosisResultToMap(diagnosisResult);
            
            // 执行资源调度
            ResourceAllocation resourceAllocation = resourceSchedulingService.scheduleResources(triageRecord, null);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "triageRecord", triageRecord,
                "patient", Map.of(
                    "name", triageRecord.getPatient() != null ? triageRecord.getPatient().getPatientName() : "未知",
                    "idCard", triageRecord.getPatient() != null ? triageRecord.getPatient().getIdNumber() : "待补充",
                    "age", triageRecord.getPatient() != null ? triageRecord.getPatient().getAge() : 0,
                    "gender", triageRecord.getPatient() != null ? triageRecord.getPatient().getGender() : "未知"
                ),
                "aiAnalysis", aiAnalysis,
                "resourceAllocation", resourceAllocation
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "开始诊断失败: " + e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "获取AI详细诊断分析")
    @GetMapping("/ai-analysis/{triageId}")
    public ResponseEntity<Map<String, Object>> getAIAnalysis(@PathVariable Long triageId) {
        try {
            TriageRecord triageRecord = triageService.findById(triageId);
            
            // 调用多模态AI诊断服务
            com.medical.model.DiagnosisResult diagnosisResult = multimodalDiagnosisService.performMultimodalDiagnosis(triageRecord);
            Map<String, Object> analysis = convertDiagnosisResultToMap(diagnosisResult);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "analysis", analysis,
                "message", "AI分析获取成功"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "AI分析获取失败: " + e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "保存诊断结果")
    @PostMapping("/save-diagnosis")
    public ResponseEntity<Map<String, Object>> saveDiagnosis(@RequestBody Map<String, Object> diagnosisData) {
        try {
            Long triageId = ((Number) diagnosisData.get("triageId")).longValue();
            String diagnosisContent = (String) diagnosisData.get("diagnosis");
            String treatment = (String) diagnosisData.get("treatment");
            
            TriageRecord triageRecord = triageService.findById(triageId);
            
            DiagnosisRecord diagnosisRecord = new DiagnosisRecord();
            diagnosisRecord.setTriageRecord(triageRecord);
            diagnosisRecord.setDiagnosis(diagnosisContent); // 使用diagnosis字段
            diagnosisRecord.setTreatmentPlan(treatment);
            diagnosisRecord.setDiagnosisTime(java.time.LocalDateTime.now());
            
            DiagnosisRecord saved = diagnosisService.saveDiagnosis(diagnosisRecord);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "diagnosis", saved,
                "message", "诊断结果保存成功"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "保存诊断失败: " + e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "申请会诊")
    @PostMapping("/request-consultation/{triageId}")
    public ResponseEntity<Map<String, Object>> requestConsultation(
            @PathVariable Long triageId,
            @RequestBody Map<String, Object> requestData) {
        
        try {
            Long triageRecordId = ((Number) requestData.get("triageRecordId")).longValue();
            String reason = (String) requestData.get("reason");
            String department = (String) requestData.getOrDefault("department", "心内科");
            
            triageService.requestConsultation(triageRecordId, reason, department);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "会诊申请成功"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "会诊申请失败: " + e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "获取我的患者列表")
    @GetMapping("/my-patients")
    public ResponseEntity<Map<String, Object>> getMyPatients(Principal principal) {
        try {
            List<Patient> myPatients = new ArrayList<>();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", myPatients,
                "total", myPatients.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "获取患者列表失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 将DiagnosisResult转换为Map
     */
    private Map<String, Object> convertDiagnosisResultToMap(com.medical.model.DiagnosisResult diagnosisResult) {
        Map<String, Object> map = new HashMap<>();
        
        map.put("success", diagnosisResult.isSuccess());
        map.put("primaryDiagnosis", diagnosisResult.getPrimaryDiagnosis());
        map.put("confidence", diagnosisResult.getConfidence());
        map.put("symptomAnalysis", diagnosisResult.getSymptomAnalysis());
        map.put("vitalSignsAnalysis", diagnosisResult.getVitalSignsAnalysis());
        map.put("medicalHistoryAnalysis", diagnosisResult.getMedicalHistoryAnalysis());
        map.put("differentialDiagnosis", diagnosisResult.getDifferentialDiagnosis());
        map.put("recommendedExams", diagnosisResult.getRecommendedExams());
        map.put("treatmentRecommendation", diagnosisResult.getTreatmentRecommendation());
        map.put("urgencyLevel", diagnosisResult.getUrgencyLevel());
        map.put("modelVersion", diagnosisResult.getModelVersion());
        map.put("triageLevel", diagnosisResult.getTriageLevel());
        map.put("diagnosisTime", diagnosisResult.getDiagnosisTime());
        map.put("processingTimeMs", diagnosisResult.getProcessingTimeMs());
        
        if (!diagnosisResult.isSuccess()) {
            map.put("errorMessage", diagnosisResult.getErrorMessage());
        }
        
        return map;
    }
    
    /**
     * 解密并脱敏身份证号
     */
    private String decryptAndMaskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return "未登记";
        }
        // 判断是否是加密数据（Base64格式，长度>30）
        boolean isEncrypted = idCard.length() > 30 && idCard.matches("^[A-Za-z0-9+/=]+$");
        if (isEncrypted) {
            try {
                String decrypted = encryptionUtil.decrypt(idCard);
                // 解密成功，进行脱敏
                return encryptionUtil.maskIdCard(decrypted);
            } catch (Exception e) {
                log.warn("身份证解密失败，显示占位符");
                return "证件号待确认";
            }
        }
        // 未加密数据，直接脱敏
        return encryptionUtil.maskIdCard(idCard);
    }
}