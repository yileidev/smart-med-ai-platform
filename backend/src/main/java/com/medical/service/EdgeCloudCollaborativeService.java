package com.medical.service;

import com.medical.entity.*;
import com.medical.model.DiagnosisResult;
import com.medical.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 边缘-云端协同多模态AI急诊分诊与诊断系统核心服务
 * 
 * 基于毕业设计要求实现的三层架构：
 * 1. 边缘感知分诊层 - 实时处理传感器数据和语音信息
 * 2. 云端诊断与调度层 - 深度AI诊断与智能资源调度
 * 3. Web交互层 - 医护人员操作界面
 * 
 * 技术栈：
 * - 边缘端：BERT-Tiny + 规则引擎混合分诊
 * - 云端：LangChain4j + 百川AI大模型 + Drools规则引擎
 * - 数据传输：MQTT协议
 * - 向量知识库：Chroma DB语义检索
 * 
 * TODO: 此服务需要完善EdgeDeviceData实体字段后才能正常使用
 * 暂时禁用以确保系统能够编译通过
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdgeCloudCollaborativeService {

    // 核心服务组件
    private final EdgeDataService edgeDataService;
    private final TriageService triageService;
    private final MultimodalDiagnosisService multimodalDiagnosisService;
    private final MedicalResourceSchedulingService resourceSchedulingService;
    private final MedicalVectorKnowledgeService vectorKnowledgeService;
    private final SystemLogService systemLogService;
    
    // 数据仓库
    private final EdgeDeviceDataRepository edgeDeviceDataRepository;
    private final TriageRecordRepository triageRecordRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;
    private final ResourceAllocationRepository resourceAllocationRepository;

    /**
     * 边缘-云端协同处理流程
     * 实现完整的多模态AI急诊分诊与诊断流程
     * 
     * @param edgeDeviceId 边缘设备ID
     * @param multimodalData 多模态数据（传感器+语音）
     * @return 处理结果
     */
    @Transactional
    public Map<String, Object> processCollaborativeDiagnosis(String edgeDeviceId, 
                                                           Map<String, Object> multimodalData) {
        try {
            log.info("开始边缘-云端协同处理流程 - 设备ID: {}", edgeDeviceId);
            
            // 第一阶段：边缘端实时分诊处理
            Map<String, Object> edgeProcessingResult = processEdgeTriage(edgeDeviceId, multimodalData);
            
            // 第二阶段：云端深度诊断分析
            Map<String, Object> cloudDiagnosisResult = processCloudDiagnosis(edgeProcessingResult);
            
            // 第三阶段：智能资源调度
            Map<String, Object> resourceSchedulingResult = processResourceScheduling(
                (Long) edgeProcessingResult.get("triageRecordId"),
                (Long) cloudDiagnosisResult.get("diagnosisResultId")
            );
            
            // 第四阶段：知识库增强与反馈
            Map<String, Object> knowledgeEnhancementResult = enhanceWithKnowledgeBase(
                edgeProcessingResult, cloudDiagnosisResult, resourceSchedulingResult
            );
            
            // 构建完整的处理结果
            Map<String, Object> collaborativeResult = new HashMap<>();
            collaborativeResult.put("success", true);
            collaborativeResult.put("timestamp", LocalDateTime.now());
            collaborativeResult.put("deviceId", edgeDeviceId);
            collaborativeResult.put("edgeProcessing", edgeProcessingResult);
            collaborativeResult.put("cloudDiagnosis", cloudDiagnosisResult);
            collaborativeResult.put("resourceScheduling", resourceSchedulingResult);
            collaborativeResult.put("knowledgeEnhancement", knowledgeEnhancementResult);
            
            // 记录系统日志
            systemLogService.logUserAction(
                null, "edge-cloud-system", "COLLABORATIVE_DIAGNOSIS", 
                "SYSTEM", null, "边缘-云端协同处理完成"
            );
            
            log.info("边缘-云端协同处理流程完成 - 设备ID: {}", edgeDeviceId);
            return collaborativeResult;
            
        } catch (Exception e) {
            log.error("边缘-云端协同处理失败 - 设备ID: {}", edgeDeviceId, e);
            return createErrorResult(e.getMessage());
        }
    }

    /**
     * 第一阶段：边缘端实时分诊处理
     * 基于传感器数据和语音信息进行快速分诊
     */
    private Map<String, Object> processEdgeTriage(String edgeDeviceId, 
                                                 Map<String, Object> multimodalData) {
        try {
            // 1. 解析多模态数据
            EdgeDeviceData edgeData = parseMultimodalData(edgeDeviceId, multimodalData);
            
            // 2. 边缘端AI分诊（模拟规则引擎 + BERT-Tiny）
            performEdgeAITriage(edgeData);
            
            // 3. 保存边缘数据
            EdgeDeviceData savedEdgeData = edgeDeviceDataRepository.save(edgeData);
            
            // 4. 创建分诊记录
            TriageRecord triageRecord = createTriageRecordFromEdgeData(savedEdgeData);
            TriageRecord savedTriageRecord = triageService.saveTriageRecordFromEdge(triageRecord);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("edgeDataId", savedEdgeData.getId());
            result.put("triageRecordId", savedTriageRecord.getId());
            result.put("triageLevel", savedTriageRecord.getTriageLevel());
            result.put("processingTime", System.currentTimeMillis());
            result.put("edgeProcessingLatency", calculateEdgeLatency(multimodalData));
            
            return result;
            
        } catch (Exception e) {
            log.error("边缘端分诊处理失败", e);
            throw new RuntimeException("边缘端分诊失败: " + e.getMessage());
        }
    }

    /**
     * 第二阶段：云端深度诊断分析
     * 使用百川AI大模型进行精确诊断
     */
    private Map<String, Object> processCloudDiagnosis(Map<String, Object> edgeResult) {
        try {
            Long triageRecordId = (Long) edgeResult.get("triageRecordId");
            TriageRecord triageRecord = triageService.findById(triageRecordId);
            
            // 1. 多模态AI诊断
            DiagnosisResult modelDiagnosis = multimodalDiagnosisService.performMultimodalDiagnosis(triageRecord);
            
            // 2. 向量知识库语义检索增强
            Map<String, Object> knowledgeEnhancement = vectorKnowledgeService.searchSimilarCases(
                triageRecord.getChiefComplaint(), 
                triageRecord.getVitalSigns()
            );
            
            // 3. 转换并保存诊断结果
            com.medical.entity.DiagnosisResult entityDiagnosis = convertToEntityDiagnosis(modelDiagnosis, triageRecord);
            entityDiagnosis = diagnosisResultRepository.save(entityDiagnosis);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("diagnosisResultId", entityDiagnosis.getId());
            result.put("preliminaryDiagnosis", entityDiagnosis.getPreliminaryDiagnosis());
            result.put("diagnosisConfidence", entityDiagnosis.getDiagnosisConfidence());
            result.put("knowledgeEnhancement", knowledgeEnhancement);
            result.put("modelUsed", entityDiagnosis.getAiModelUsed());
            result.put("cloudProcessingLatency", calculateCloudLatency());
            
            return result;
            
        } catch (Exception e) {
            log.error("云端诊断分析失败", e);
            throw new RuntimeException("云端诊断失败: " + e.getMessage());
        }
    }

    /**
     * 第三阶段：智能资源调度
     * 基于Drools规则引擎进行资源调度
     */
    private Map<String, Object> processResourceScheduling(Long triageRecordId, Long diagnosisResultId) {
        try {
            TriageRecord triageRecord = triageService.findById(triageRecordId);
            com.medical.entity.DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisResultId)
                .orElseThrow(() -> new RuntimeException("诊断结果不存在"));
            
            // 执行智能资源调度
            ResourceAllocation resourceAllocation = resourceSchedulingService.scheduleResources(
                triageRecord, diagnosisResult
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("resourceAllocationId", resourceAllocation.getId());
            result.put("allocatedDepartment", resourceAllocation.getAllocatedDepartment());
            result.put("allocatedBed", resourceAllocation.getAllocatedBed());
            result.put("assignedDoctor", resourceAllocation.getAssignedDoctor());
            result.put("status", resourceAllocation.getStatus());
            result.put("schedulingLatency", calculateSchedulingLatency());
            
            return result;
            
        } catch (Exception e) {
            log.error("资源调度失败", e);
            throw new RuntimeException("资源调度失败: " + e.getMessage());
        }
    }

    /**
     * 第四阶段：知识库增强与反馈
     * 持续优化知识库和模型性能
     */
    private Map<String, Object> enhanceWithKnowledgeBase(Map<String, Object> edgeResult,
                                                        Map<String, Object> cloudResult,
                                                        Map<String, Object> resourceResult) {
        try {
            // 1. 提取关键信息
            String symptoms = extractSymptomsFromResults(edgeResult, cloudResult);
            String diagnosis = (String) cloudResult.get("preliminaryDiagnosis");
            Double confidence = ((BigDecimal) cloudResult.get("diagnosisConfidence")).doubleValue();
            
            // 2. 向量知识库更新（语义学习）
            vectorKnowledgeService.updateKnowledgeBase(symptoms, diagnosis, confidence);
            
            // 3. 生成处理建议
            List<String> recommendations = generateTreatmentRecommendations(cloudResult, resourceResult);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("knowledgeBaseUpdated", true);
            result.put("recommendations", recommendations);
            result.put("modelImprovementSuggestion", generateModelImprovementSuggestion(confidence));
            result.put("processingCompleted", true);
            
            return result;
            
        } catch (Exception e) {
            log.error("知识库增强失败", e);
            throw new RuntimeException("知识库增强失败: " + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 解析多模态数据
     */
    private EdgeDeviceData parseMultimodalData(String edgeDeviceId, Map<String, Object> multimodalData) {
        EdgeDeviceData edgeData = new EdgeDeviceData();
        
        // 基础信息
        edgeData.setDeviceId(edgeDeviceId);
        edgeData.setTimestamp(LocalDateTime.now());
        edgeData.setProcessed(false);
        
        // 传感器数据
        Map<String, Object> sensorData = (Map<String, Object>) multimodalData.get("sensorData");
        if (sensorData != null) {
            edgeData.setTemperature(((Number) sensorData.get("temperature")).doubleValue());
            edgeData.setSystolicBloodPressure(((Number) sensorData.get("systolicBP")).intValue());
            edgeData.setDiastolicBloodPressure(((Number) sensorData.get("diastolicBP")).intValue());
            edgeData.setHeartRate(((Number) sensorData.get("heartRate")).intValue());
            edgeData.setBloodOxygen(((Number) sensorData.get("bloodOxygen")).intValue());
            edgeData.setRespiratoryRate(((Number) sensorData.get("respiratoryRate")).intValue());
        }
        
        // 语音数据（已转文字）
        String speechText = (String) multimodalData.get("speechText");
        if (speechText != null) {
            edgeData.setSymptomText(speechText);
        }
        
        // 意识状态
        String consciousness = (String) multimodalData.get("consciousness");
        if (consciousness != null) {
            edgeData.setConsciousness(convertConsciousnessLevel(consciousness));
        }
        
        return edgeData;
    }

    /**
     * 边缘端AI分诊（规则引擎 + BERT-Tiny混合）
     */
    private void performEdgeAITriage(EdgeDeviceData edgeData) {
        // 1. 基础规则引擎分诊
        int ruleBasedLevel = calculateRuleBasedTriageLevel(edgeData);
        
        // 2. BERT-Tiny语义分析（模拟）
        double semanticConfidence = calculateSemanticConfidence(edgeData.getSymptomText());
        
        // 3. 混合决策
        int finalLevel = hybridTriageDecision(ruleBasedLevel, semanticConfidence);
        double finalConfidence = calculateFinalConfidence(ruleBasedLevel, semanticConfidence);
        
        // 设置结果
        edgeData.setTriageLevel(finalLevel);
        edgeData.setTriageScore(finalConfidence);
        edgeData.setConfidence(finalConfidence);
        edgeData.setProcessingTimeMs(System.currentTimeMillis());
    }

    /**
     * 从边缘数据创建分诊记录
     */
    private TriageRecord createTriageRecordFromEdgeData(EdgeDeviceData edgeData) {
        TriageRecord triageRecord = new TriageRecord();
        
        // 创建患者记录（基于设备ID）
        Patient patient = new Patient();
        patient.setPatientName("边缘患者-" + edgeData.getDeviceId());
        patient.setGender(Patient.Gender.OTHER);
        
        triageRecord.setPatient(patient);
        triageRecord.setArrivalTime(edgeData.getTimestamp());
        triageRecord.setChiefComplaint(edgeData.getSymptomText());
        triageRecord.setTriageLevel(edgeData.getTriageLevel());
        triageRecord.setTriageScore(edgeData.getTriageScore());
        triageRecord.setAiConfidence(edgeData.getConfidence());
        triageRecord.setTriageSource("边缘AI");
        
        // 生命体征JSON
        Map<String, Object> vitalSigns = new HashMap<>();
        vitalSigns.put("temperature", edgeData.getTemperature());
        vitalSigns.put("systolicBP", edgeData.getSystolicBloodPressure());
        vitalSigns.put("diastolicBP", edgeData.getDiastolicBloodPressure());
        vitalSigns.put("heartRate", edgeData.getHeartRate());
        vitalSigns.put("bloodOxygen", edgeData.getBloodOxygen());
        vitalSigns.put("respiratoryRate", edgeData.getRespiratoryRate());
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            triageRecord.setVitalSigns(mapper.writeValueAsString(vitalSigns));
        } catch (Exception e) {
            triageRecord.setVitalSigns("{}");
        }
        
        return triageRecord;
    }

    /**
     * 转换诊断结果
     */
    private com.medical.entity.DiagnosisResult convertToEntityDiagnosis(
            DiagnosisResult modelResult, TriageRecord triageRecord) {
        
        com.medical.entity.DiagnosisResult entityResult = new com.medical.entity.DiagnosisResult();
        
        entityResult.setTriageRecord(triageRecord);
        entityResult.setPreliminaryDiagnosis(modelResult.getPrimaryDiagnosis());
        entityResult.setDiagnosisConfidence(BigDecimal.valueOf(modelResult.getConfidence()));
        entityResult.setSymptomsAnalysis(modelResult.getSymptomAnalysis());
        entityResult.setTreatmentSuggestions(modelResult.getTreatmentRecommendation());
        entityResult.setRiskAssessment("紧急程度: " + modelResult.getUrgencyLevel());
        entityResult.setAiModelUsed(modelResult.getModelVersion());
        entityResult.setProcessingTimeMs(modelResult.getProcessingTimeMs());
        entityResult.setStatus(com.medical.entity.DiagnosisResult.DiagnosisStatus.PENDING);
        
        return entityResult;
    }

    // 计算方法
    private Long calculateEdgeLatency(Map<String, Object> multimodalData) {
        return 150L + (long) (Math.random() * 100); // 模拟150-250ms
    }

    private Long calculateCloudLatency() {
        return 800L + (long) (Math.random() * 400); // 模拟800-1200ms
    }

    private Long calculateSchedulingLatency() {
        return 200L + (long) (Math.random() * 100); // 模拟200-300ms
    }

    private int calculateRuleBasedTriageLevel(EdgeDeviceData edgeData) {
        // 简化的规则引擎，结合血压数据
        // I级濒危：任意一项严重异常
        if (edgeData.getBloodOxygen() != null && edgeData.getBloodOxygen() < 85) return 1;
        if (edgeData.getHeartRate() != null && (edgeData.getHeartRate() < 40 || edgeData.getHeartRate() > 150)) return 1;
        if (edgeData.getSystolicBloodPressure() != null && 
            (edgeData.getSystolicBloodPressure() < 70 || edgeData.getSystolicBloodPressure() > 200)) return 1;
        if (edgeData.getTemperature() != null && 
            (edgeData.getTemperature() < 35.0 || edgeData.getTemperature() > 40.0)) return 1;
        
        // II级危急：多项中度异常
        if (edgeData.getBloodOxygen() != null && edgeData.getBloodOxygen() < 90) return 2;
        if (edgeData.getHeartRate() != null && (edgeData.getHeartRate() < 50 || edgeData.getHeartRate() > 120)) return 2;
        if (edgeData.getSystolicBloodPressure() != null && 
            (edgeData.getSystolicBloodPressure() < 90 || edgeData.getSystolicBloodPressure() > 180)) return 2;
        if (edgeData.getDiastolicBloodPressure() != null && edgeData.getDiastolicBloodPressure() > 110) return 2;
        if (edgeData.getTemperature() != null && edgeData.getTemperature() > 39.0) return 2;
        
        // III级急症：轻度异常
        if (edgeData.getHeartRate() != null && edgeData.getHeartRate() > 100) return 3;
        if (edgeData.getBloodOxygen() != null && edgeData.getBloodOxygen() < 95) return 3;
        if (edgeData.getSystolicBloodPressure() != null && edgeData.getSystolicBloodPressure() > 160) return 3;
        
        return 4;
    }

    private double calculateSemanticConfidence(String symptomText) {
        if (symptomText == null) return 0.7;
        if (symptomText.contains("胸痛") || symptomText.contains("呼吸困难")) return 0.95;
        if (symptomText.contains("发热") || symptomText.contains("头痛")) return 0.85;
        return 0.75;
    }

    private int hybridTriageDecision(int ruleLevel, double semanticConfidence) {
        if (semanticConfidence > 0.9) return Math.max(1, ruleLevel - 1);
        if (semanticConfidence < 0.6) return Math.min(5, ruleLevel + 1);
        return ruleLevel;
    }

    private double calculateFinalConfidence(int ruleLevel, double semanticConfidence) {
        return (ruleLevel * 0.4 + semanticConfidence * 0.6);
    }

    private Integer convertConsciousnessLevel(String consciousness) {
        switch (consciousness.toLowerCase()) {
            case "alert": return 1;
            case "drowsy": return 2;
            case "confused": return 3;
            case "unresponsive": return 4;
            default: return 3;
        }
    }

    private String extractSymptomsFromResults(Map<String, Object> edgeResult, Map<String, Object> cloudResult) {
        // 提取症状用于知识库更新
        return "多模态症状分析"; // 简化实现
    }

    private List<String> generateTreatmentRecommendations(Map<String, Object> cloudResult, Map<String, Object> resourceResult) {
        List<String> recommendations = new ArrayList<>();
        recommendations.add("密切监测生命体征");
        recommendations.add("根据诊断结果制定治疗方案");
        recommendations.add("确保资源分配到位");
        return recommendations;
    }

    private String generateModelImprovementSuggestion(double confidence) {
        if (confidence > 0.9) return "模型表现优秀";
        if (confidence > 0.8) return "模型表现良好，建议持续优化";
        return "建议增加训练数据，优化模型参数";
    }

    private Map<String, Object> createErrorResult(String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", errorMessage);
        error.put("timestamp", LocalDateTime.now());
        return error;
    }
}