package com.medical.service;

import com.medical.config.LangChain4jConfig;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.retriever.Retriever;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG诊断服务
 * 整合 LangChain4j + 向量检索 + Drools规则引擎
 */
@Slf4j
@Service
@SuppressWarnings("unused")
public class RAGDiagnosisService {
    
    private static final Logger log = LoggerFactory.getLogger(RAGDiagnosisService.class);
    
    private final LangChain4jConfig.MedicalDiagnosisAI medicalDiagnosisAI;
    private final MedicalVectorKnowledgeService vectorKnowledgeService;
    private final Retriever<TextSegment> retriever;
    private final KieSession kieSession;
    
    @Autowired
    public RAGDiagnosisService(
            @Autowired(required = false) LangChain4jConfig.MedicalDiagnosisAI medicalDiagnosisAI,
            @Autowired(required = false) MedicalVectorKnowledgeService vectorKnowledgeService,
            @Autowired(required = false) Retriever<TextSegment> retriever,
            @Autowired(required = false) KieSession kieSession) {
        this.medicalDiagnosisAI = medicalDiagnosisAI;
        this.vectorKnowledgeService = vectorKnowledgeService;
        this.retriever = retriever;
        this.kieSession = kieSession;
    }
    
    /**
     * 执行完整的RAG增强诊断流程
     * 1. 向量检索相关医疗知识
     * 2. LangChain4j链式调用AI诊断
     * 3. Drools规则引擎决策
     */
    public Map<String, Object> performRAGDiagnosis(DiagnosisRequest request) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            // ========== 第1步：向量检索相关知识 ==========
            log.info("开始RAG诊断流程 - 步骤1: 向量检索");
            List<MedicalVectorKnowledgeService.MedicalRecommendation> vectorResults = 
                vectorKnowledgeService.searchBySymptoms(request.getSymptoms());
            
            String retrievedKnowledge = vectorResults.stream()
                .map(r -> String.format("科室:%s, 设备:%s, 相似度:%.2f", 
                    r.getDepartment(), r.getEquipments(), r.getConfidence()))
                .collect(Collectors.joining("; "));
            
            log.info("检索到 {} 条相关医疗知识", vectorResults.size());
            
            // ========== 第2步：LangChain4j AI诊断（一次调用完成所有分析） ==========
            log.info("开始RAG诊断流程 - 步骤2: AI综合诊断分析（性能优化版）");
            
            // 构建综合提示词，一次调用完成所有分析
            String comprehensivePrompt = String.format(
                "患者信息: %s\n" +
                "症状描述: %s\n" +
                "生命体征: %s\n" +
                "病史: %s\n" +
                "参考知识: %s\n" +
                "年龄: %s\n\n" +
                "请一次性完成以下分析并按格式返回：\n" +
                "【症状分析】简要分析患者症状(50字内)\n" +
                "【分诊等级】1-5级(1级濒危 2级危急 3级急症 4级次急症 5级非急症)\n" +
                "【推荐科室】最合适的就诊科室\n" +
                "【处置建议】简要处置建议(50字内)",
                request.getPatientInfo(),
                request.getSymptoms(),
                request.getVitalSigns(),
                request.getMedicalHistory(),
                retrievedKnowledge,
                request.getPatientAge()
            );
            
            // 一次调用获取所有结果
            String comprehensiveResult = medicalDiagnosisAI.analyzeSymptomsWithRAG(
                request.getPatientInfo(),
                comprehensivePrompt,
                request.getVitalSigns(),
                request.getMedicalHistory()
            );
            
            // 解析综合结果
            String diagnosisAnalysis = extractSection(comprehensiveResult, "症状分析", "基于症状综合分析");
            String triageAssessment = extractSection(comprehensiveResult, "分诊等级", "3级（急症）");
            String departmentRecommendation = extractSection(comprehensiveResult, "推荐科室", "急诊科");
            String treatmentPlan = extractSection(comprehensiveResult, "处置建议", "建议进一步检查");
            
            log.info("AI综合诊断完成（单次调用），结果长度: {} 字", comprehensiveResult.length());
            
            // ========== 第3步：Drools规则引擎决策 ==========
            log.info("开始RAG诊断流程 - 步骤3: Drools规则决策");
            
            // 提取分诊等级
            int triageLevel = extractTriageLevel(triageAssessment);
            
            // 执行Drools规则（如果kieSession可用）
            int rulesFired = 0;
            int priorityScore = triageLevel <= 2 ? 100 : (triageLevel == 3 ? 50 : 20);
            int estimatedWaitTime = triageLevel == 1 ? 0 : (triageLevel == 2 ? 10 : (triageLevel == 3 ? 30 : 60));
            
            if (kieSession != null) {
                try {
                    rulesFired = kieSession.fireAllRules();
                    log.info("Drools规则引擎执行完成，触发 {} 条规则", rulesFired);
                } catch (Exception e) {
                    log.warn("Drools规则执行失败，使用默认值: {}", e.getMessage());
                }
            } else {
                log.warn("KieSession不可用，跳过Drools规则执行");
            }
            
            // ========== 汇总结果 ==========
            long processingTime = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("diagnosis", Map.of(
                "analysis", diagnosisAnalysis,
                "triage_assessment", triageAssessment,
                "department_recommendation", departmentRecommendation,
                "treatment_plan", treatmentPlan
            ));
            result.put("vector_retrieval", Map.of(
                "matched_knowledge", vectorResults,
                "count", vectorResults.size(),
                "summary", retrievedKnowledge
            ));
            result.put("drools_decision", Map.of(
                "priority_score", priorityScore,
                "estimated_wait_time", estimatedWaitTime,
                "triage_level", triageLevel,
                "rules_fired", rulesFired
            ));
            result.put("processing_time_ms", processingTime);
            result.put("architecture", "RAG (LangChain4j + Vector DB + Drools)");
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("RAG诊断流程完成，总耗时: {}ms", processingTime);
            
        } catch (Exception e) {
            log.error("RAG诊断流程失败", e);
            result.put("success", false);
            result.put("error", "诊断失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 从 AI 结果中提取分诊等级
     */
    private Integer extractTriageLevel(String assessment) {
        if (assessment.contains("1级") || assessment.contains("濒危")) return 1;
        if (assessment.contains("2级") || assessment.contains("危急")) return 2;
        if (assessment.contains("3级") || assessment.contains("急症")) return 3;
        if (assessment.contains("4级") || assessment.contains("次急症")) return 4;
        if (assessment.contains("5级") || assessment.contains("非急症")) return 5;
        return 3; // 默认3级
    }
    
    /**
     * 从AI综合结果中提取指定段落内容
     */
    private String extractSection(String content, String sectionName, String defaultValue) {
        try {
            String marker = "【" + sectionName + "】";
            int start = content.indexOf(marker);
            if (start == -1) {
                return defaultValue;
            }
            start += marker.length();
            
            // 查找下一个段落标记
            int end = content.indexOf("【", start);
            if (end == -1) {
                end = content.length();
            }
            
            return content.substring(start, end).trim();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * 诊断请求DTO
     */
    public static class DiagnosisRequest {
        private String patientInfo;
        private String symptoms;
        private String vitalSigns;
        private String medicalHistory;
        private String patientAge;
        
        // Getters and Setters
        public String getPatientInfo() { return patientInfo; }
        public void setPatientInfo(String patientInfo) { this.patientInfo = patientInfo; }
        
        public String getSymptoms() { return symptoms; }
        public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
        
        public String getVitalSigns() { return vitalSigns; }
        public void setVitalSigns(String vitalSigns) { this.vitalSigns = vitalSigns; }
        
        public String getMedicalHistory() { return medicalHistory; }
        public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
        
        public String getPatientAge() { return patientAge; }
        public void setPatientAge(String patientAge) { this.patientAge = patientAge; }
    }
}
