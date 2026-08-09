package com.medical.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 百川智能大模型调用服务
 * 基于LangChain4j框架实现
 */
@Slf4j
@Service
public class BaichuanAIService {

    private ChatLanguageModel chatModel;
    private ConversationalChain diagnosticChain;
    private final ChromaVectorService vectorService;
    private final ObjectMapper objectMapper;

    // 百川模型配置
    @Value("${medical.ai.model.api-key:}")
    private String apiKey;
    
    @Value("${medical.ai.model.base-url:https://api.baichuan-ai.com/v1}")
    private String baseUrl;

    public BaichuanAIService(ChromaVectorService vectorService, ObjectMapper objectMapper) {
        this.vectorService = vectorService;
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    private void initializeChatModel() {
        // 直接使用配置的API Key初始化百川模型
        log.info("初始化百川AI服务，使用真实API Key");
        this.chatModel = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName("Baichuan2-Turbo-192k")  // 使用长上下文版本
            .temperature(0.2)  // 医疗场景推荐参数
            .maxTokens(512)
            .build();

        // 创建对话链，用于诊断推理
        this.diagnosticChain = ConversationalChain.builder()
            .chatLanguageModel(chatModel)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .build();
        
        log.info("百川AI服务初始化完成");
    }

    /**
     * AI智能分诊 - 基于症状和生命体征
     */
    @Cacheable(value = "aiDiagnosis", key = "'triage:' + #chiefComplaint.hashCode()")
    public Map<String, Object> performAITriage(String chiefComplaint, Map<String, Object> vitalSigns) {
        try {
            // 1. 构建诊断提示词
            String prompt = buildTriagePrompt(chiefComplaint, vitalSigns);
            
            // 2. 调用百川模型进行分析
            String aiResponse = diagnosticChain.execute(prompt);
            
            // 3. 解析AI响应
            Map<String, Object> triageResult = parseTriageResponse(aiResponse);
            
            // 4. 结合向量知识库推荐科室
            String recommendedDepartment = vectorService.recommendDepartment(chiefComplaint);
            triageResult.put("recommendedDepartment", recommendedDepartment);
            
            log.info("AI分诊完成 - 患者主诉: {}, 推荐科室: {}", chiefComplaint, recommendedDepartment);
            return triageResult;
            
        } catch (Exception e) {
            log.error("AI分诊失败", e);
            return getDefaultTriageResult();
        }
    }

    /**
     * AI深度诊断分析
     */
    public Map<String, Object> performDeepDiagnosis(String symptoms, String history, Map<String, Object> vitalSigns) {
        try {
            String prompt = buildDiagnosisPrompt(symptoms, history, vitalSigns);
            String aiResponse = diagnosticChain.execute(prompt);
            
            Map<String, Object> diagnosisResult = parseDiagnosisResponse(aiResponse);
            
            log.info("AI深度诊断完成");
            return diagnosisResult;
            
        } catch (Exception e) {
            log.error("AI深度诊断失败", e);
            return getDefaultDiagnosisResult();
        }
    }

    /**
     * 构建分诊提示词
     */
    private String buildTriagePrompt(String chiefComplaint, Map<String, Object> vitalSigns) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("\u4f60\u662f\u4e00\u4e2a\u4e13\u4e1a\u7684\u6025\u8bca\u5206\u8bca AI\u52a9\u624b\u3002\u8bf7\u6839\u636e\u4ee5\u4e0b\u60a3\u8005\u4fe1\u606f\u8fdb\u884c\u5206\u8bca\u8bc4\u4f30\uff1a\n\n");
        prompt.append("\u60a3\u8005\u4e3b\u8bc9: ").append(chiefComplaint).append("\n");
        if (vitalSigns != null && !vitalSigns.isEmpty()) {
            prompt.append("\u751f\u547d\u4f53\u5f81: ").append(vitalSigns.toString()).append("\n");
        }
        prompt.append("\n\u8bf7\u6309\u4ee5\u4e0b\u683c\u5f0f\u56de\u7b54\uff1a\n");
        prompt.append("\u5206\u8bca\u7b49\u7ea7: [1-4\u7ea7]\n");
        prompt.append("\u63a8\u8350\u79d1\u5ba4: [\u5177\u4f53\u79d1\u5ba4\u540d\u79f0]\n");
        prompt.append("\u75c7\u72b6\u5206\u6790: [\u5206\u6790\u60a3\u8005\u75c7\u72b6\u7279\u70b9\u548c\u53ef\u80fd\u539f\u56e0]\n");
        prompt.append("\u5904\u7f6e\u5efa\u8bae: [\u5177\u4f53\u5904\u7f6e\u65b9\u6848]\n");
        prompt.append("\u7f6e\u4fe1\u5ea6: [0.x]");
        
        return prompt.toString();
    }

    /**
     * 构建诊断提示词
     */
    private String buildDiagnosisPrompt(String symptoms, String history, Map<String, Object> vitalSigns) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("作为资深急诊医生，请对以下病例进行深度分析：\n\n");
        prompt.append("症状描述: ").append(symptoms).append("\n");
        prompt.append("病史信息: ").append(history != null ? history : "无特殊病史").append("\n");
        prompt.append("生命体征: ").append(vitalSigns != null ? vitalSigns.toString() : "待完善").append("\n\n");
        
        prompt.append("请提供:\n");
        prompt.append("1. 可能诊断 (按可能性排序，至少3个)\n");
        prompt.append("2. 鉴别诊断要点\n");
        prompt.append("3. 建议检查项目\n");
        prompt.append("4. 初步治疗方案\n");
        prompt.append("5. 注意事项和随访建议\n");
        prompt.append("6. 诊断置信度评估\n");
        
        return prompt.toString();
    }

    /**
     * 解析分诊响应 - 优化版
     */
    private Map<String, Object> parseTriageResponse(String response) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String[] lines = response.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                // 提取分诊等级
                if (line.matches(".*分诊等级.*[1-4]级.*") || line.matches(".*[\u2160\u2161\u2162\u2163].*")) {
                    Integer level = extractTriageLevel(line);
                    if (level != null) result.put("triageLevel", level);
                }
                
                // 提取紧急程度
                if (line.contains("紧急程度") || line.contains("紧急度")) {
                    result.put("urgency", extractUrgencyLevel(line));
                }
                
                // 提取推荐科室
                if (line.contains("推荐科室") || line.contains("建议科室") || line.contains("就诊科室")) {
                    String dept = extractAfterColon(line, "急诊科");
                    result.put("department", dept);
                }
                
                // 提取症状分析
                if (line.contains("症状分析") || line.contains("主要症状")) {
                    result.put("analysis", extractAfterColon(line, "待进一步评估"));
                }
                
                // 提取处理建议
                if (line.contains("处理建议") || line.contains("初步处理")) {
                    result.put("suggestion", extractAfterColon(line, "密切观察"));
                }
                
                // 提取置信度
                if (line.contains("置信度")) {
                    result.put("confidence", extractDouble(line, 0.75));
                }
            }
            
        } catch (Exception e) {
            log.warn("分诊响应解析失败，使用默认值", e);
        }
        
        // 确保必需字段存在
        result.putIfAbsent("triageLevel", 3);
        result.putIfAbsent("urgency", "中");
        result.putIfAbsent("department", "急诊科");
        result.putIfAbsent("confidence", 0.75);
        // 如果没有解析出分析内容，使用AI原始响应
        if (!result.containsKey("analysis") || result.get("analysis") == null) {
            result.put("analysis", response != null && response.length() > 20 ? response.substring(0, Math.min(200, response.length())) : "根据患者主诉和体征进行分诊评估");
        }
        result.putIfAbsent("suggestion", "请根据患者具体情况进行处置");
        
        return result;
    }
    
    /**
     * 提取分诊等级
     */
    private Integer extractTriageLevel(String line) {
        try {
            // 提取罗马数字
            if (line.contains("Ⅰ") || line.contains("红色") || line.contains("极高")) return 1;
            if (line.contains("Ⅱ") || line.contains("橙色") || line.contains("高")) return 2;
            if (line.contains("Ⅲ") || line.contains("黄色") || line.contains("中")) return 3;
            if (line.contains("Ⅳ") || line.contains("绿色") || line.contains("低")) return 4;
            
            // 提取阿拉伯数字
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("[1-4]");
            java.util.regex.Matcher m = p.matcher(line);
            if (m.find()) {
                return Integer.parseInt(m.group());
            }
        } catch (Exception e) {
            log.warn("提取分诊等级失败", e);
        }
        return null;
    }
    
    /**
     * 提取紧急程度
     */
    private String extractUrgencyLevel(String line) {
        if (line.contains("极高") || line.contains("危急") || line.contains("急危")) return "极高";
        if (line.contains("高") || line.contains("紧急") || line.contains("急重")) return "高";
        if (line.contains("中") || line.contains("急症")) return "中";
        if (line.contains("低") || line.contains("亚急症")) return "低";
        return "中";
    }

    /**
     * 解析诊断响应
     */
    private Map<String, Object> parseDiagnosisResponse(String response) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("diagnosis", response);
        result.put("confidence", 0.8);
        result.put("suggestions", "请根据患者具体情况制定治疗方案");
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }

    /**
     * 获取默认分诊结果
     */
    private Map<String, Object> getDefaultTriageResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("triageLevel", 3);
        result.put("urgency", "中");
        result.put("department", "急诊科");
        result.put("analysis", "AI分析暂不可用，建议人工分诊");
        result.put("confidence", 0.5);
        return result;
    }

    /**
     * 获取默认诊断结果
     */
    private Map<String, Object> getDefaultDiagnosisResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("diagnosis", "AI诊断服务暂不可用，请医生根据临床经验诊断");
        result.put("confidence", 0.3);
        result.put("suggestions", "建议完善相关检查");
        return result;
    }

    // 辅助解析方法
    private int extractNumber(String line, int defaultValue) {
        try {
            String[] parts = line.split(":");
            if (parts.length > 1) {
                String numberStr = parts[1].replaceAll("[^0-9]", "");
                return Integer.parseInt(numberStr);
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return defaultValue;
    }

    private String extractAfterColon(String line, String defaultValue) {
        try {
            String[] parts = line.split(":");
            return parts.length > 1 ? parts[1].trim() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double extractDouble(String line, double defaultValue) {
        try {
            String[] parts = line.split(":");
            if (parts.length > 1) {
                String numberStr = parts[1].replaceAll("[^0-9.]", "");
                return Double.parseDouble(numberStr);
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return defaultValue;
    }

    /**
     * RAG增强的医疗诊断（护士端专用）
     * 1. 向量知识库检索相关医疗知识
     * 2. 结合检索结果调用百川大模型
     */
    public Map<String, Object> performRAGDiagnosis(String symptoms, String medicalHistory, Map<String, Object> vitalSigns) {
        try {
            // ========== RAG步骤1: 向量知识库检索 ==========
            String retrievedKnowledge = "";
            try {
                log.info("护士端RAG: 开始向量知识库检索...");
                Map<String, Object> vectorResult = vectorService.semanticEnhancedDiagnosis(symptoms, medicalHistory);
                if (vectorResult != null && Boolean.TRUE.equals(vectorResult.get("success"))) {
                    retrievedKnowledge = (String) vectorResult.getOrDefault("enhancedContext", "");
                    log.info("护士端RAG: 检索到相关知识 {} 字", retrievedKnowledge.length());
                }
            } catch (Exception e) {
                log.warn("向量知识库检索失败，继续使用大模型", e);
            }
            
            // ========== RAG步骤2: 构建增强提示词 + 调用大模型 ==========
            String ragPrompt = buildRAGPrompt(symptoms, medicalHistory, vitalSigns, retrievedKnowledge);
            String aiResponse = diagnosticChain.execute(ragPrompt);
            
            // 解析诊断结果
            Map<String, Object> diagnosisResult = parseTriageResponse(aiResponse);
            diagnosisResult.put("ragEnhancement", !retrievedKnowledge.isEmpty());
            diagnosisResult.put("knowledgeSource", retrievedKnowledge.isEmpty() ? "无" : "向量知识库");
            
            log.info("护士端RAG诊断完成");
            return diagnosisResult;
            
        } catch (Exception e) {
            log.error("RAG诊断失败，降级为普通分诊", e);
            return performAITriage(symptoms, vitalSigns);
        }
    }

    /**
     * 构建RAG增强提示词（精简版，加快响应）
     */
    private String buildRAGPrompt(String symptoms, String medicalHistory, Map<String, Object> vitalSigns, String enhancedContext) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("【护士分诊辅助】\n");
        prompt.append("症状: ").append(symptoms).append("\n");
        prompt.append("生命体征: ").append(vitalSigns != null ? vitalSigns.toString() : "待完善").append("\n");
        if (enhancedContext != null && !enhancedContext.isEmpty()) {
            prompt.append("参考: ").append(enhancedContext.length() > 100 ? enhancedContext.substring(0, 100) : enhancedContext).append("\n");
        }
        prompt.append("\n请简洁回答（100字内）：\n");
        prompt.append("1. 初步分析\n2. 分诊等级(1-5级)\n3. 推荐科室\n4. 处置建议");
        
        return prompt.toString();
    }

    /**
     * 解析增强诊断响应
     */
    private Map<String, Object> parseEnhancedDiagnosisResponse(String response) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("diagnosis", response);
        result.put("confidence", 0.85);
        result.put("differentialDiagnosis", extractDifferentialDiagnosis(response));
        result.put("recommendedTests", extractRecommendedTests(response));
        result.put("treatmentPlan", extractTreatmentPlan(response));
        result.put("urgency", extractUrgency(response));
        result.put("prognosis", extractPrognosis(response));
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }

    // 辅助解析方法 - 优化版
    private List<String> extractDifferentialDiagnosis(String response) {
        List<String> diagnoses = new ArrayList<>();
        try {
            // 查找"鉴别诊断"或"可能诊断"相关内容
            String[] patterns = {"鉴别诊断[：:]", "可能诊断[：:]", "诊断[：:]"};
            for (String pattern : patterns) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern + "([^\n]+)");
                java.util.regex.Matcher m = p.matcher(response);
                if (m.find()) {
                    String diagnosisText = m.group(1);
                    // 分割多个诊断
                    String[] items = diagnosisText.split("[,，、]");
                    for (String item : items) {
                        String cleaned = item.trim().replaceAll("^[0-9]+[.、]", "");
                        if (!cleaned.isEmpty() && cleaned.length() > 2) {
                            diagnoses.add(cleaned);
                        }
                    }
                    if (!diagnoses.isEmpty()) break;
                }
            }
        } catch (Exception e) {
            log.warn("提取鉴别诊断失败", e);
        }
        return diagnoses.isEmpty() ? Arrays.asList("需结合临床进一步判断") : diagnoses;
    }

    private List<String> extractRecommendedTests(String response) {
        List<String> tests = new ArrayList<>();
        try {
            // 查找"检查"相关内容
            String[] patterns = {"建议检查[：:]", "推荐检查[：:]", "检查项目[：:]", "进一步检查[：:]"};
            for (String pattern : patterns) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern + "([^\n]+)");
                java.util.regex.Matcher m = p.matcher(response);
                if (m.find()) {
                    String testsText = m.group(1);
                    String[] items = testsText.split("[,，、]");
                    for (String item : items) {
                        String cleaned = item.trim().replaceAll("^[0-9]+[.、]", "");
                        if (!cleaned.isEmpty() && cleaned.length() > 2) {
                            tests.add(cleaned);
                        }
                    }
                    if (!tests.isEmpty()) break;
                }
            }
        } catch (Exception e) {
            log.warn("提取检查项目失败", e);
        }
        // 如果没有提取到，根据症状推荐常规检查
        if (tests.isEmpty()) {
            tests.addAll(Arrays.asList("血常规", "生化全套", "心电图"));
        }
        return tests;
    }

    private String extractTreatmentPlan(String response) {
        try {
            // 查找"治疗"相关内容
            String[] patterns = {"治疗方案[：:]", "处理建议[：:]", "治疗建议[：:]"};
            for (String pattern : patterns) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern + "([^\n]{10,200})");
                java.util.regex.Matcher m = p.matcher(response);
                if (m.find()) {
                    return m.group(1).trim();
                }
            }
        } catch (Exception e) {
            log.warn("提取治疗方案失败", e);
        }
        return "请根据诊断结果制定个性化治疗方案，注意监测生命体征";
    }

    private String extractUrgency(String response) {
        if (response.contains("紧急") || response.contains("危急")) return "高";
        if (response.contains("急诊") || response.contains("立即")) return "中";
        return "低";
    }

    private String extractPrognosis(String response) {
        if (response.contains("良好")) return "良好";
        if (response.contains("谨慎")) return "谨慎乐观";
        return "需要观察";
    }

    /**
     * 获取AI服务健康状态
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 测试模型连接
            String testResponse = chatModel.generate("健康检查");
            
            status.put("status", "online");
            status.put("model", "Baichuan2-Turbo");
            status.put("framework", "LangChain4j");
            status.put("lastCheck", System.currentTimeMillis());
            status.put("testResponse", testResponse != null ? "正常" : "异常");
            
        } catch (Exception e) {
            status.put("status", "offline");
            status.put("error", e.getMessage());
            status.put("lastCheck", System.currentTimeMillis());
        }
        
        return status;
    }
}