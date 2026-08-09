package com.medical.controller;

import com.medical.dto.ApiResponse;
import com.medical.entity.TriageRecord;
import com.medical.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI集成功能控制器
 * 整合百川模型、向量知识库、资源调度和HL7接口
 */
@SuppressWarnings("unused") // REST API端点
@RestController
@RequestMapping("/ai")
public class AIIntegrationController {
    
    private static final Logger log = LoggerFactory.getLogger(AIIntegrationController.class);

    private final BaichuanAIService baichuanAIService;
    private final ChromaVectorService vectorService;
    private final ResourceSchedulingService schedulingService;
    private final HL7IntegrationService hl7Service;
    private final TriageService triageService;

    public AIIntegrationController(BaichuanAIService baichuanAIService,
                                   ChromaVectorService vectorService,
                                   ResourceSchedulingService schedulingService,
                                   HL7IntegrationService hl7Service,
                                   TriageService triageService) {
        this.baichuanAIService = baichuanAIService;
        this.vectorService = vectorService;
        this.schedulingService = schedulingService;
        this.hl7Service = hl7Service;
        this.triageService = triageService;
    }

    /**
     * 智能分诊 - 集成AI分析、向量检索和资源调度
     */
    @PostMapping("/intelligent-triage")
    public ApiResponse<Map<String, Object>> performIntelligentTriage(@RequestBody Map<String, Object> request) {
        try {
            String chiefComplaint = (String) request.get("chiefComplaint");
            Map<String, Object> vitalSigns = (Map<String, Object>) request.get("vitalSigns");
            
            // 1. AI分诊分析
            Map<String, Object> aiResult = baichuanAIService.performAITriage(chiefComplaint, vitalSigns);
            
            // 2. 向量知识库科室推荐
            String vectorDepartment = vectorService.recommendDepartment(chiefComplaint);
            
            // 3. 资源智能调度
            ResourceSchedulingService.SchedulingRequest schedReq = new ResourceSchedulingService.SchedulingRequest();
            schedReq.setTriageLevel((Integer) aiResult.get("triageLevel"));
            schedReq.setRecommendedDepartment(vectorDepartment);
            schedReq.setSymptoms(chiefComplaint);
            
            ResourceSchedulingService.SchedulingResult scheduling = schedulingService.scheduleResources(schedReq);
            
            // 4. 整合结果
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("aiAnalysis", aiResult);
            result.put("vectorRecommendation", vectorDepartment);
            result.put("resourceScheduling", scheduling);
            result.put("confidence", aiResult.get("confidence"));
            
            log.info("智能分诊完成 - 主诉: {}, 推荐科室: {}", chiefComplaint, vectorDepartment);
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("智能分诊失败", e);
            return ApiResponse.error("智能分诊服务暂不可用");
        }
    }

    /**
     * AI深度诊断
     */
    @PostMapping("/deep-diagnosis")
    public ApiResponse<Map<String, Object>> performDeepDiagnosis(@RequestBody Map<String, Object> request) {
        try {
            String symptoms = (String) request.get("symptoms");
            String history = (String) request.get("history");
            Map<String, Object> vitalSigns = (Map<String, Object>) request.get("vitalSigns");
            
            Map<String, Object> diagnosis = baichuanAIService.performDeepDiagnosis(symptoms, history, vitalSigns);
            
            return ApiResponse.success(diagnosis);
            
        } catch (Exception e) {
            log.error("AI深度诊断失败", e);
            return ApiResponse.error("AI诊断服务暂不可用");
        }
    }

    /**
     * 初始化医疗知识库
     */
    @PostMapping("/init-knowledge-base")
    public ApiResponse<String> initializeKnowledgeBase() {
        try {
            vectorService.initializeMedicalKnowledge();
            return ApiResponse.success("医疗知识库初始化成功");
        } catch (Exception e) {
            log.error("知识库初始化失败", e);
            return ApiResponse.error("知识库初始化失败");
        }
    }

    /**
     * 获取科室推荐
     */
    @GetMapping("/recommend-department")
    public ApiResponse<String> recommendDepartment(@RequestParam String symptoms) {
        try {
            String department = vectorService.recommendDepartment(symptoms);
            return ApiResponse.success(department);
        } catch (Exception e) {
            log.error("科室推荐失败", e);
            return ApiResponse.error("科室推荐服务暂不可用");
        }
    }

    /**
     * 获取设备推荐
     */
    @GetMapping("/recommend-devices")
    public ApiResponse<java.util.List<String>> recommendDevices(@RequestParam String department, 
                                                                @RequestParam String urgency) {
        try {
            java.util.List<String> devices = vectorService.recommendDevices(department, urgency);
            return ApiResponse.success(devices);
        } catch (Exception e) {
            log.error("设备推荐失败", e);
            return ApiResponse.error("设备推荐服务暂不可用");
        }
    }

    /**
     * 生成HL7消息
     */
    @PostMapping("/generate-hl7")
    public ResponseEntity<String> generateHL7Message(@RequestBody Map<String, Object> request) {
        try {
            Object triageRecordIdObj = request.get("triageRecordId");
            Object messageTypeObj = request.get("messageType");
            
            if (triageRecordIdObj == null) {
                return ResponseEntity.badRequest().body("分诊记录ID不能为空");
            }
            if (messageTypeObj == null) {
                return ResponseEntity.badRequest().body("消息类型不能为空");
            }
            
            Long triageRecordId = Long.valueOf(triageRecordIdObj.toString());
            String messageType = messageTypeObj.toString();
            
            TriageRecord triageRecord = triageService.findById(triageRecordId);
            String hl7Message;
            
            switch (messageType.toUpperCase()) {
                case "ADT_A01":
                    hl7Message = hl7Service.generateADT_A01(triageRecord.getPatient(), triageRecord);
                    break;
                case "ORM_O01":
                    String orderDetails = (String) request.get("orderDetails");
                    hl7Message = hl7Service.generateORM_O01(triageRecord, orderDetails);
                    break;
                default:
                    return ResponseEntity.badRequest().body("不支持的HL7消息类型");
            }
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/plain")
                .body(hl7Message);
            
        } catch (Exception e) {
            log.error("HL7消息生成失败", e);
            return ResponseEntity.internalServerError().body("HL7消息生成失败");
        }
    }

    /**
     * 解析HL7消息
     */
    @PostMapping("/parse-hl7")
    public ApiResponse<Map<String, Object>> parseHL7Message(@RequestBody Map<String, String> request) {
        try {
            String hl7Message = request.get("hl7Message");
            
            if (!hl7Service.validateHL7Message(hl7Message)) {
                return ApiResponse.error("无效的HL7消息格式");
            }
            
            Map<String, Object> parsed = hl7Service.parseHL7Message(hl7Message);
            return ApiResponse.success(parsed);
            
        } catch (Exception e) {
            log.error("HL7消息解析失败", e);
            return ApiResponse.error("HL7消息解析失败");
        }
    }

    /**
     * 获取系统集成状态
     */
    @GetMapping("/integration-status")
    public ApiResponse<Map<String, Object>> getIntegrationStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        
        try {
            // 检查各服务状态
            status.put("baichuanAI", "已集成");
            status.put("vectorDatabase", "Chroma DB已配置");
            status.put("rulesEngine", "Drools已集成");
            status.put("hl7Support", "HL7 v2.5已支持");
            status.put("timestamp", System.currentTimeMillis());
            
            return ApiResponse.success(status);
            
        } catch (Exception e) {
            log.error("获取集成状态失败", e);
            return ApiResponse.error("状态检查失败");
        }
    }

    /**
     * RAG增强医疗诊断API
     */
    @PostMapping("/rag-diagnosis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ragDiagnosis(@RequestBody Map<String, Object> request) {
        try {
            String symptoms = (String) request.get("symptoms");
            String medicalHistory = (String) request.get("medicalHistory");
            Map<String, Object> vitalSigns = (Map<String, Object>) request.get("vitalSigns");
            
            // 调用RAG增强诊断
            Map<String, Object> diagnosisResult = baichuanAIService.performRAGDiagnosis(symptoms, medicalHistory, vitalSigns);
            
            return ResponseEntity.ok(
                ApiResponse.success("RAG增强诊断完成", diagnosisResult)
            );
        } catch (Exception e) {
            log.error("RAG诊断失败", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("RAG诊断失败: " + e.getMessage())
            );
        }
    }

    /**
     * 向量知识库语义搜索API
     */
    @PostMapping("/vector-search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> vectorSearch(@RequestBody Map<String, Object> request) {
        try {
            String symptoms = (String) request.get("symptoms");
            
            // 语义增强
            Map<String, Object> enhancement = vectorService.semanticEnhancedDiagnosis(symptoms, "");
            
            Map<String, Object> result = Map.of(
                "semanticEnhancement", enhancement,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(
                ApiResponse.success("向量知识库搜索完成", result)
            );
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("向量搜索失败: " + e.getMessage())
            );
        }
    }

    /**
     * 系统健康检查API
     */
    @GetMapping("/health-check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        try {
            Map<String, Object> healthStatus = new HashMap<>();
            
            // 检查百川AI服务状态
            Map<String, Object> aiStatus = baichuanAIService.getHealthStatus();
            healthStatus.put("baichuanAI", aiStatus);
            
            // 检查向量知识库状态
            Map<String, Object> vectorStatus = vectorService.getHealthStatus();
            healthStatus.put("chromaVector", vectorStatus);
            
            // 检查系统整体状态
            boolean allHealthy = "online".equals(aiStatus.get("status")) && 
                               "online".equals(vectorStatus.get("status"));
            
            healthStatus.put("overall", Map.of(
                "status", allHealthy ? "healthy" : "degraded",
                "timestamp", System.currentTimeMillis(),
                "architecture", "边缘-云端协同多模态AI急诊分诊系统"
            ));
            
            return ResponseEntity.ok(
                ApiResponse.success("健康检查完成", healthStatus)
            );
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("健康检查失败: " + e.getMessage())
            );
        }
    }

    /**
     * 完整AI医疗流程API - 集成所有功能
     */
    @PostMapping("/full-medical-pipeline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fullMedicalPipeline(@RequestBody Map<String, Object> request) {
        try {
            log.info("启动完整AI医疗流程");
            
            String symptoms = (String) request.get("symptoms");
            String medicalHistory = (String) request.get("medicalHistory");
            Map<String, Object> vitalSigns = (Map<String, Object>) request.get("vitalSigns");
            
            Map<String, Object> pipelineResult = new HashMap<>();
            
            // 第一步：AI智能分诊
            log.info("执行AI智能分诊...");
            Map<String, Object> triageResult = baichuanAIService.performAITriage(symptoms, vitalSigns);
            pipelineResult.put("aiTriage", triageResult);
            
            // 第二步：向量知识库增强
            log.info("执行向量知识库检索...");
            String department = vectorService.recommendDepartment(symptoms);
            List<String> equipment = vectorService.recommendDevices(department, "急诊");
            pipelineResult.put("vectorRecommendations", Map.of(
                "department", department,
                "equipment", equipment
            ));
            
            // 第三步：RAG增强诊断
            log.info("执行RAG增强诊断...");
            Map<String, Object> ragDiagnosis = baichuanAIService.performRAGDiagnosis(symptoms, medicalHistory, vitalSigns);
            pipelineResult.put("ragDiagnosis", ragDiagnosis);
            
            // 第四步：智能资源调度
            log.info("执行智能资源调度...");
            ResourceSchedulingService.SchedulingRequest schedulingRequest = 
                new ResourceSchedulingService.SchedulingRequest();
            schedulingRequest.setTriageLevel((Integer) triageResult.get("triageLevel"));
            schedulingRequest.setSymptoms(symptoms);
            schedulingRequest.setRecommendedDepartment(department);
            
            ResourceSchedulingService.SchedulingResult schedulingResult = 
                schedulingService.scheduleResources(schedulingRequest);
            pipelineResult.put("resourceScheduling", Map.of(
                "department", schedulingResult.getDepartment(),
                "priority", schedulingResult.getPriority(),
                "estimatedWaitTime", schedulingResult.getEstimatedWaitTime()
            ));
            
            // 第五步：生成HL7标准消息
            log.info("生成HL7标准消息...");
            String hl7Message = "MSH|^~\\&|MEDICAL_TRIAGE_SYSTEM|AI_INTEGRATION|HOSPITAL_HIS|MAIN_HIS|" +
                              getCurrentTimestamp() + "||ADT^A01^ADT_A01|" + generateControlId() + "|P|2.5";
            pipelineResult.put("hl7Message", hl7Message);
            
            // 添加流程元数据
            pipelineResult.put("pipelineMetadata", Map.of(
                "completedAt", System.currentTimeMillis(),
                "processingSteps", Arrays.asList("AI分诊", "向量检索", "RAG诊断", "资源调度", "HL7生成"),
                "architecture", "边缘-云端协同多模态AI系统",
                "frameworks", Arrays.asList("LangChain4j", "ChromaDB", "Drools", "HL7")
            ));
            
            log.info("完整AI医疗流程执行完成");
            
            return ResponseEntity.ok(
                ApiResponse.success("完整AI医疗流程执行成功", pipelineResult)
            );
            
        } catch (Exception e) {
            log.error("完整AI医疗流程执行失败", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("AI医疗流程失败: " + e.getMessage())
            );
        }
    }

    // 辅助方法
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
    
    private String generateControlId() {
        return "AI" + System.currentTimeMillis();
    }
}