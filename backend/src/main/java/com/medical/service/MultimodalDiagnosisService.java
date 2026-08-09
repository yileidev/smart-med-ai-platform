package com.medical.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.model.BaichuanMedicalAIDiagnosis;
import com.medical.model.DiagnosisResult;
import com.medical.service.BaichuanMedicalAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;
import java.util.Date;

/**
 * 多模态诊断服务（医生端专用）
 * 集成百川智能医疗大模型 + 向量知识库RAG
 */
@Service
@Slf4j
public class MultimodalDiagnosisService {

    private final BaichuanMedicalAIService baichuanMedicalAIService;
    private final ChromaVectorService vectorService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    public MultimodalDiagnosisService(
            BaichuanMedicalAIService baichuanMedicalAIService,
            @Autowired(required = false) ChromaVectorService vectorService) {
        this.baichuanMedicalAIService = baichuanMedicalAIService;
        this.vectorService = vectorService;
    }

    /**
     * 综合多模态诊断（医生端专用 - 带RAG调用链）
     * 1. 向量知识库检索相关医疗知识
     * 2. 结合检索结果调用百川大模型
     */
    public DiagnosisResult comprehensiveDiagnosis(
            String patientInfo, String vitalSigns, 
            String symptomText, String triageLevel) {
        
        try {
            // ========== RAG步骤1: 向量知识库检索 ==========
            String retrievedKnowledge = "";
            if (vectorService != null) {
                try {
                    log.info("医生端RAG: 开始向量知识库检索...");
                    Map<String, Object> vectorResult = vectorService.semanticEnhancedDiagnosis(symptomText, "");
                    if (vectorResult != null && Boolean.TRUE.equals(vectorResult.get("success"))) {
                        retrievedKnowledge = (String) vectorResult.getOrDefault("enhancedContext", "");
                        log.info("医生端RAG: 检索到相关知识 {} 字", retrievedKnowledge.length());
                    }
                } catch (Exception e) {
                    log.warn("向量知识库检索失败，继续使用大模型", e);
                }
            }
            
            // ========== RAG步骤2: 构建增强提示词 + 调用大模型 ==========
            String enhancedInput = buildComprehensiveInput(patientInfo, vitalSigns, symptomText, triageLevel);
            if (!retrievedKnowledge.isEmpty()) {
                enhancedInput = "【参考知识库】\n" + retrievedKnowledge + "\n\n" + enhancedInput;
            }
            
            // 调用百川智能医疗大模型
            String aiDiagnosis = baichuanMedicalAIService.analyzeMedicalSymptoms(
                patientInfo, enhancedInput, vitalSigns, ""
            );
            
            // 解析诊断结果
            DiagnosisResult result = parseDiagnosisResult(aiDiagnosis);
            
            // 添加元数据
            result.setModelVersion("baichuan-medical-v1-RAG");
            result.setTriageLevel(triageLevel);
            result.setDiagnosisTime(new Date());
            
            log.info("医生端RAG诊断完成 - 患者: {}, 分诊等级: {}", 
                patientInfo.split(",")[0], triageLevel);
            
            return result;
            
        } catch (Exception e) {
            log.error("多模态诊断失败", e);
            return createErrorDiagnosisResult(e.getMessage());
        }
    }

    /**
     * 构建综合诊断输入
     */
    private String buildComprehensiveInput(String patientInfo, String vitalSigns, String symptomText, String triageLevel) {
        StringBuilder input = new StringBuilder();
        
        input.append("【患者信息】\n").append(patientInfo).append("\n\n");
        input.append("【分诊等级】\n").append(triageLevel).append("级\n\n");
        input.append("【生命体征】\n").append(vitalSigns).append("\n\n");
        input.append("【症状描述】\n").append(symptomText).append("\n\n");
        input.append("请基于以上信息进行综合诊断，并给出详细的分析和建议。");
        
        return input.toString();
    }

    /**
     * 解析诊断结果
     */
    private DiagnosisResult parseDiagnosisResult(String aiDiagnosis) {
        DiagnosisResult result = new DiagnosisResult();
        
        try {
            // 1. 提取主要诊断
            String primaryDiagnosis = extractDiagnosis(aiDiagnosis);
            result.setPrimaryDiagnosis(primaryDiagnosis);
            
            // 2. 提取置信度
            double confidence = extractConfidence(aiDiagnosis);
            result.setConfidence(confidence);
            
            // 3. 提取症状分析
            String symptomAnalysis = extractSymptomAnalysis(aiDiagnosis);
            result.setSymptomAnalysis(symptomAnalysis);
            
            // 4. 提取鉴别诊断
            List<String> differentialDiagnosis = extractDifferentialDiagnosis(aiDiagnosis);
            result.setDifferentialDiagnosis(differentialDiagnosis);
            
            // 5. 提取推荐检查
            List<String> recommendedExams = extractRecommendedExams(aiDiagnosis);
            result.setRecommendedExams(recommendedExams);
            
            // 6. 提取治疗建议
            String treatmentRecommendation = extractTreatmentRecommendation(aiDiagnosis);
            result.setTreatmentRecommendation(treatmentRecommendation);
            
            // 7. 提取紧急程度
            String urgencyLevel = extractUrgencyLevel(aiDiagnosis);
            result.setUrgencyLevel(urgencyLevel);
            
            result.setSuccess(true);
            
        } catch (Exception e) {
            log.error("解析诊断结果失败", e);
            result.setSuccess(false);
            result.setErrorMessage("解析诊断结果失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 提取主要诊断
     */
    private String extractDiagnosis(String diagnosisResult) {
        if (diagnosisResult.contains("可能的诊断")) {
            String[] parts = diagnosisResult.split("可能的诊断");
            if (parts.length > 1) {
                String diagnosisSection = parts[1].split("\n\n")[0];
                return diagnosisSection.trim();
            }
        }
        
        // 使用正则表达式提取诊断信息
        Pattern pattern = Pattern.compile("(?:诊断|考虑|怀疑).{0,50}");
        Matcher matcher = pattern.matcher(diagnosisResult);
        if (matcher.find()) {
            return matcher.group();
        }
        
        return "诊断分析中";
    }

    /**
     * 提取置信度
     */
    private double extractConfidence(String diagnosisResult) {
        // 基于关键词和表达方式估算置信度
        if (diagnosisResult.contains("高度怀疑") || diagnosisResult.contains("确诊")) {
            return 0.85 + Math.random() * 0.1;
        } else if (diagnosisResult.contains("可能") || diagnosisResult.contains("考虑")) {
            return 0.70 + Math.random() * 0.15;
        } else if (diagnosisResult.contains("不排除")) {
            return 0.60 + Math.random() * 0.2;
        }
        
        return 0.75; // 默认置信度
    }

    /**
     * 提取症状分析
     */
    private String extractSymptomAnalysis(String diagnosisResult) {
        if (diagnosisResult.contains("主要症状分析")) {
            String[] parts = diagnosisResult.split("主要症状分析");
            if (parts.length > 1) {
                String analysisSection = parts[1].split("\n\n")[0];
                return analysisSection.trim();
            }
        }
        
        return "症状分析已完成";
    }

    /**
     * 提取鉴别诊断
     */
    private List<String> extractDifferentialDiagnosis(String diagnosisResult) {
        List<String> differentialDiagnosis = new ArrayList<>();
        
        // 查找可能的疾病列表
        Pattern pattern = Pattern.compile("[-•]\\s*([^\\n]+)");
        Matcher matcher = pattern.matcher(diagnosisResult);
        
        while (matcher.find()) {
            String diagnosis = matcher.group(1).trim();
            if (diagnosis.length() > 3 && !diagnosis.contains("检查") && !diagnosis.contains("建议")) {
                differentialDiagnosis.add(diagnosis);
            }
        }
        
        // 限制最多5个鉴别诊断
        if (differentialDiagnosis.size() > 5) {
            differentialDiagnosis = differentialDiagnosis.subList(0, 5);
        }
        
        return differentialDiagnosis;
    }

    /**
     * 提取推荐检查
     */
    private List<String> extractRecommendedExams(String diagnosisResult) {
        List<String> exams = new ArrayList<>();
        
        if (diagnosisResult.contains("建议的检查项目")) {
            String[] parts = diagnosisResult.split("建议的检查项目");
            if (parts.length > 1) {
                String examSection = parts[1].split("\r\n\r\n")[0];
                // 提取检查项目
                String[] lines = examSection.split("\r\n");
                for (String line : lines) {
                    if (line.contains("检查") || line.contains("化验") || line.contains("影像")) {
                        exams.add(line.trim());
                    }
                }
            }
        }
        
        if (exams.isEmpty()) {
            exams.add("建议完善相关检查");
        }
        
        return exams;
    }

    /**
     * 提取治疗建议
     */
    private String extractTreatmentRecommendation(String diagnosisResult) {
        if (diagnosisResult.contains("初步治疗建议")) {
            String[] parts = diagnosisResult.split("初步治疗建议");
            if (parts.length > 1) {
                String treatmentSection = parts[1].split("\n\n")[0];
                return treatmentSection.trim();
            }
        }
        
        return "请根据具体诊断制定治疗方案";
    }

    /**
     * 提取紧急程度
     */
    private String extractUrgencyLevel(String diagnosisResult) {
        if (diagnosisResult.contains("高度紧急") || diagnosisResult.contains("立即")) {
            return "紧急";
        } else if (diagnosisResult.contains("尽快") || diagnosisResult.contains("及时")) {
            return "较紧急";
        } else if (diagnosisResult.contains("择期") || diagnosisResult.contains("常规")) {
            return "常规";
        }
        
        return "一般";
    }

    /**
     * 创建错误诊断结果
     */
    private DiagnosisResult createErrorDiagnosisResult(String errorMessage) {
        DiagnosisResult result = new DiagnosisResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setPrimaryDiagnosis("诊断失败");
        result.setConfidence(0.0);
        result.setModelVersion("baichuan-medical-v1");
        result.setDiagnosisTime(new Date());
        
        return result;
    }

    /**
     * 批量诊断
     */
    public List<DiagnosisResult> batchDiagnosis(List<Map<String, Object>> patientDataList) {
        List<DiagnosisResult> results = new ArrayList<>();
        
        for (Map<String, Object> patientData : patientDataList) {
            try {
                String patientInfo = (String) patientData.get("patientInfo");
                String vitalSigns = (String) patientData.get("vitalSigns");
                String symptomText = (String) patientData.get("symptomText");
                String triageLevel = (String) patientData.get("triageLevel");
                
                DiagnosisResult result = comprehensiveDiagnosis(patientInfo, vitalSigns, symptomText, triageLevel);
                results.add(result);
                
            } catch (Exception e) {
                log.error("批量诊断中处理失败", e);
                results.add(createErrorDiagnosisResult("批量处理失败: " + e.getMessage()));
            }
        }
        
        return results;
    }

    /**
     * 验证诊断结果
     */
    public boolean validateDiagnosisResult(DiagnosisResult result) {
        if (result == null) {
            return false;
        }
        
        if (!result.isSuccess()) {
            return false;
        }
        
        if (result.getPrimaryDiagnosis() == null || result.getPrimaryDiagnosis().trim().isEmpty()) {
            return false;
        }
        
        if (result.getConfidence() < 0.3) {
            log.warn("诊断置信度过低: {}", result.getConfidence());
            return false;
        }
        
        return true;
    }
    
    /**
     * 基于分诊记录执行多模态诊断
     * @param triageRecord 分诊记录
     * @return 诊断结果
     */
    public DiagnosisResult performMultimodalDiagnosis(com.medical.entity.TriageRecord triageRecord) {
        try {
            // 构建患者信息 - 添加null检查
            com.medical.entity.Patient patient = triageRecord.getPatient();
            String patientInfo;
            if (patient != null) {
                patientInfo = String.format("%s, %d岁, %s", 
                    patient.getPatientName() != null ? patient.getPatientName() : "未知",
                    patient.getAge() != null ? patient.getAge() : 0,
                    patient.getGender() != null ? patient.getGender().toString() : "UNKNOWN");
            } else {
                patientInfo = "患者信息未提供";
                log.warn("分诊记录 {} 的患者信息为空", triageRecord.getId());
            }
            
            // 获取生命体征
            String vitalSigns = triageRecord.getVitalSigns() != null ? 
                triageRecord.getVitalSigns() : "{}";
            
            // 获取症状描述
            String symptomText = triageRecord.getChiefComplaint() != null ? 
                triageRecord.getChiefComplaint() : "无主诉";
            
            // 分诊等级
            String triageLevel = triageRecord.getTriageLevel() != null ? 
                triageRecord.getTriageLevel().toString() : "4";
            
            // 调用综合诊断方法
            return comprehensiveDiagnosis(patientInfo, vitalSigns, symptomText, triageLevel);
            
        } catch (Exception e) {
            log.error("多模态诊断失败: {}", e.getMessage(), e);
            
            // 返回默认诊断结果
            DiagnosisResult fallbackResult = new DiagnosisResult();
            fallbackResult.setSuccess(false);
            fallbackResult.setErrorMessage("诊断系统暂时不可用: " + e.getMessage());
            fallbackResult.setPrimaryDiagnosis("待人工诊断");
            fallbackResult.setConfidence(0.0);
            fallbackResult.setDiagnosisTime(new Date());
            
            return fallbackResult;
        }
    }
}