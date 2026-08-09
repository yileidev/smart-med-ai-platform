package com.medical.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 百川智能医疗大模型服务
 * 基于百川智能API文档实现医疗专用大模型调用
 */
@Service
public class BaichuanMedicalAIService {

    @Value("${medical.ai.model.base-url}")
    private String baseUrl;

    @Value("${medical.ai.model.api-key}")
    private String apiKey;

    @Value("${medical.ai.model.name}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 添加初始化日志
    @javax.annotation.PostConstruct
    public void init() {
        System.out.println("🔑 百川AI配置信息:");
        System.out.println("  Base URL: " + baseUrl);
        System.out.println("  API Key: " + (apiKey != null && apiKey.length() > 10 ? apiKey.substring(0, 10) + "..." : "[未配置]"));
        System.out.println("  Model: " + modelName);
    }

    /**
     * 医疗症状分析和诊断建议
     */
    public String analyzeMedicalSymptoms(String patientInfo, String symptoms, String vitalSigns, String medicalHistory) {
        String systemPrompt = "你是一个专业的医疗AI助手，具有丰富的临床经验和医学知识。请基于提供的患者信息、症状、生命体征和病史，进行专业的医疗分析和诊断建议。请按照以下格式回复：\n" +
                "1. 主要症状分析\n2. 可能的诊断\n3. 建议的检查项目\n4. 紧急程度评估\n5. 初步治疗建议";
        
        String userPrompt = String.format(
                "患者信息：%s\n症状描述：%s\n生命体征：%s\n既往病史：%s", 
                patientInfo, symptoms, vitalSigns, medicalHistory
        );

        return callBaichuanAPI(systemPrompt, userPrompt);
    }

    /**
     * 分诊等级评估
     * 严格遵循国家卫健委《急诊预检分诊专家共识》(2018年版)
     */
    public String assessTriageLevel(String symptoms, String vitalSigns, String patientAge, String consciousness) {
        String systemPrompt = "你是急诊科分诊专家，请严格遵循国家卫健委《急诊预检分诊专家共识》(2018年版)，" +
                "对患者进行四级分诊评估：" +
                "Ⅰ级-急危(红色)，即刺处理，复苏区/抢救区；" +
                "Ⅱ级-急重(橙色)，10分钟内，抢救区；" +
                "Ⅲ级-急症(黄色)，30分钟内，优先诊疗区；" +
                "Ⅳ级-亚急症(绿色)，60分钟-2小时，普通诊疗区。" +
                "请依据客观评估指标(心率、血压、SpO2、体温)和人工评定指标(ABCD评估)给出分诊等级。";
        
        String userPrompt = String.format(
                "患者年龄：%s\n症状：%s\n生命体征：%s\n意识状态：%s\n请给出分诊等级(Ⅰ-Ⅳ级)、响应时限、分诊分区及详细依据。", 
                patientAge, symptoms, vitalSigns, consciousness
        );

        return callBaichuanAPI(systemPrompt, userPrompt);
    }

    /**
     * 药物相互作用检查
     */
    public String checkDrugInteractions(String currentMedications, String proposedMedication, String patientCondition) {
        String systemPrompt = "你是临床药师，请分析药物相互作用，评估用药安全性，并提供专业建议。";
        
        String userPrompt = String.format(
                "患者当前用药：%s\n拟用药物：%s\n患者状况：%s\n请分析可能的药物相互作用及用药建议。", 
                currentMedications, proposedMedication, patientCondition
        );

        return callBaichuanAPI(systemPrompt, userPrompt);
    }

    /**
     * 治疗方案生成
     */
    public String generateTreatmentPlan(String diagnosis, String patientCondition, String guidelines) {
        String systemPrompt = "你是临床医生，请基于诊断结果和临床指南，制定个性化的治疗方案。";
        
        String userPrompt = String.format(
                "诊断结果：%s\n患者状况：%s\n临床指南：%s\n请制定详细的治疗方案。", 
                diagnosis, patientCondition, guidelines
        );

        return callBaichuanAPI(systemPrompt, userPrompt);
    }

    /**
     * 调用百川智能医疗大模型API
     * 基于官方文档的标准调用方式
     */
    private String callBaichuanAPI(String systemPrompt, String userPrompt) {
        try {
            // 检查配置
            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("❌ 百川AI API Key未配置");
                return "医疗AI服务未配置，请检查API Key。";
            }
            
            System.out.println("🚀 调用百川AI - URL: " + baseUrl + "/chat/completions");
            System.out.println("🚀 使用模型: " + modelName);
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.2);  // 医疗场景使用较低温度
            requestBody.put("max_tokens", 300);   // 根据百川文档的医疗示例配置

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            String url = baseUrl + "/chat/completions";
            System.out.println("📤 发送请求到: " + url);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            System.out.println("📥 响应状态: " + response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // 解析响应
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode choices = jsonResponse.get("choices");
                if (choices != null && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null) {
                        String content = message.get("content").asText();
                        System.out.println("✅ 百川AI响应成功，内容长度: " + content.length());
                        return content;
                    }
                }
            }

            System.err.println("❌ 百川AI响应无效: " + response.getBody());
            return "医疗AI分析暂时不可用，请稍后重试。";

        } catch (Exception e) {
            System.err.println("❌ 调用百川智能医疗大模型API失败: " + e.getMessage());
            e.printStackTrace();
            return "医疗AI分析出现错误: " + e.getMessage();
        }
    }

    /**
     * 检查API连接状态
     */
    public boolean isAPIAvailable() {
        try {
            String testResponse = callBaichuanAPI(
                "你是医疗AI助手", 
                "请回复'连接正常'"
            );
            return testResponse.contains("连接正常");
        } catch (Exception e) {
            return false;
        }
    }
}