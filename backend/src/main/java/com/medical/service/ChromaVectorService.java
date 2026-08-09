package com.medical.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Chroma DB向量知识库服务
 */
@Slf4j
@Service
public class ChromaVectorService {

    private final WebClient webClient;
    private static final String COLLECTION_NAME = "medical_knowledge";

    public ChromaVectorService() {
        this.webClient = WebClient.builder()
            .baseUrl("http://localhost:8000")  // Chroma DB默认端口
            .build();
    }

    /**
     * 初始化医疗知识库
     */
    public void initializeMedicalKnowledge() {
        try {
            // 创建知识库集合
            createCollection();
            
            // 预加载症状-科室映射数据
            loadSymptomDepartmentMapping();
            
            // 预加载设备-科室关联数据
            loadDeviceDepartmentMapping();
            
            log.info("✅ 医疗知识库初始化完成");
        } catch (Exception e) {
            log.error("❌ 知识库初始化失败", e);
        }
    }

    /**
     * 创建向量集合
     */
    private void createCollection() {
        Map<String, Object> request = Map.of(
            "name", COLLECTION_NAME,
            "metadata", Map.of("description", "医疗症状科室设备关联知识库")
        );

        webClient.post()
            .uri("/api/v1/collections")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .doOnSuccess(result -> log.info("知识库集合创建成功"))
            .doOnError(error -> log.warn("集合可能已存在: {}", error.getMessage()))
            .onErrorResume(Exception.class, e -> Mono.just(""))
            .block();
    }

    /**
     * 预加载症状-科室映射知识
     */
    private void loadSymptomDepartmentMapping() {
        List<Map<String, Object>> knowledgeData = Arrays.asList(
            Map.of(
                "text", "胸痛 胸闷 心悸 心律不齐 气短 呼吸困难",
                "metadata", Map.of("type", "症状", "department", "心内科", "urgency", "高")
            ),
            Map.of(
                "text", "发热 咳嗽 咳痰 气喘 胸痛 呼吸急促",
                "metadata", Map.of("type", "症状", "department", "呼吸科", "urgency", "中")
            ),
            Map.of(
                "text", "腹痛 腹胀 恶心 呕吐 腹泻 便血",
                "metadata", Map.of("type", "症状", "department", "消化科", "urgency", "中")
            ),
            Map.of(
                "text", "头痛 头晕 肢体麻木 言语不清 意识障碍 癫痫发作",
                "metadata", Map.of("type", "症状", "department", "神经科", "urgency", "高")
            ),
            Map.of(
                "text", "外伤 骨折 关节疼痛 肌肉拉伤 韧带损伤",
                "metadata", Map.of("type", "症状", "department", "骨科", "urgency", "中")
            ),
            Map.of(
                "text", "心脏骤停 呼吸停止 重度外伤 中毒 休克",
                "metadata", Map.of("type", "症状", "department", "急诊科", "urgency", "极高")
            )
        );

        addVectorData(knowledgeData);
    }

    /**
     * 预加载设备-科室关联知识
     */
    private void loadDeviceDepartmentMapping() {
        List<Map<String, Object>> deviceData = Arrays.asList(
            Map.of(
                "text", "心电图机 除颤器 心电监护仪 起搏器",
                "metadata", Map.of("type", "设备", "department", "心内科", "priority", "高")
            ),
            Map.of(
                "text", "呼吸机 氧气 雾化器 肺功能仪",
                "metadata", Map.of("type", "设备", "department", "呼吸科", "priority", "高")
            ),
            Map.of(
                "text", "胃镜 肠镜 腹腔镜 超声波",
                "metadata", Map.of("type", "设备", "department", "消化科", "priority", "中")
            ),
            Map.of(
                "text", "CT MRI 脑电图 神经刺激器",
                "metadata", Map.of("type", "设备", "department", "神经科", "priority", "高")
            ),
            Map.of(
                "text", "X光机 石膏 牵引器 手术台",
                "metadata", Map.of("type", "设备", "department", "骨科", "priority", "中")
            ),
            Map.of(
                "text", "急救车 呼吸机 除颤器 输液泵",
                "metadata", Map.of("type", "设备", "department", "急诊科", "priority", "极高")
            )
        );

        addVectorData(deviceData);
    }

    /**
     * 向向量库添加数据
     */
    private void addVectorData(List<Map<String, Object>> dataList) {
        List<String> ids = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> data = dataList.get(i);
            ids.add("doc_" + System.currentTimeMillis() + "_" + i);
            documents.add((String) data.get("text"));
            metadatas.add((Map<String, Object>) data.get("metadata"));
        }

        Map<String, Object> request = Map.of(
            "ids", ids,
            "documents", documents,
            "metadatas", metadatas
        );

        webClient.post()
            .uri("/api/v1/collections/" + COLLECTION_NAME + "/add")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .doOnSuccess(result -> log.info("向量数据添加成功"))
            .doOnError(error -> log.error("向量数据添加失败", error))
            .block();
    }

    /**
     * 症状相似性搜索，推荐科室
     */
    public String recommendDepartment(String symptoms) {
        try {
            Map<String, Object> query = Map.of(
                "query_texts", Arrays.asList(symptoms),
                "n_results", 3,
                "where", Map.of("type", "症状")
            );

            String result = webClient.post()
                .uri("/api/v1/collections/" + COLLECTION_NAME + "/query")
                .bodyValue(query)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // 解析结果，提取推荐科室
            return parseRecommendedDepartment(result);
        } catch (Exception e) {
            log.error("科室推荐失败", e);
            return "急诊科"; // 默认推荐
        }
    }

    /**
     * 根据科室和病情严重程度，推荐医疗设备
     */
    public List<String> recommendDevices(String department, String urgency) {
        try {
            Map<String, Object> query = Map.of(
                "query_texts", Arrays.asList(department + " " + urgency),
                "n_results", 5,
                "where", Map.of("type", "设备", "department", department)
            );

            String result = webClient.post()
                .uri("/api/v1/collections/" + COLLECTION_NAME + "/query")
                .bodyValue(query)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return parseRecommendedDevices(result);
        } catch (Exception e) {
            log.error("设备推荐失败", e);
            return Arrays.asList("基础设备");
        }
    }

    /**
     * 解析推荐科室结果
     */
    private String parseRecommendedDepartment(String jsonResult) {
        // 简化解析，实际应使用JSON解析库
        if (jsonResult != null && jsonResult.contains("心内科")) return "心内科";
        if (jsonResult != null && jsonResult.contains("呼吸科")) return "呼吸科";
        if (jsonResult != null && jsonResult.contains("消化科")) return "消化科";
        if (jsonResult != null && jsonResult.contains("神经科")) return "神经科";
        if (jsonResult != null && jsonResult.contains("骨科")) return "骨科";
        return "急诊科";
    }

    /**
     * 解析推荐设备结果
     */
    private List<String> parseRecommendedDevices(String jsonResult) {
        // 简化解析，实际应使用JSON解析库
        List<String> devices = new ArrayList<>();
        if (jsonResult != null) {
            if (jsonResult.contains("心电图")) devices.add("心电图机");
            if (jsonResult.contains("呼吸机")) devices.add("呼吸机");
            if (jsonResult.contains("除颤器")) devices.add("除颤器");
            if (jsonResult.contains("CT")) devices.add("CT扫描");
            if (jsonResult.contains("X光")) devices.add("X光机");
        }
        return devices.isEmpty() ? Arrays.asList("监护设备") : devices;
    }

    /**
     * 执行语义搜索增强诊断
     */
    public Map<String, Object> semanticEnhancedDiagnosis(String symptoms, String medicalHistory) {
        try {
            // 尝试调用Chroma DB
            Map<String, Object> request = Map.of(
                "query_texts", Arrays.asList(symptoms + " " + medicalHistory),
                "n_results", 5
            );

            String result = webClient.post()
                .uri("/api/v1/collections/" + COLLECTION_NAME + "/query")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return Map.of(
                "success", true,
                "enhancedContext", result != null ? result : "无相关知识",
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.warn("向量知识库不可用（collection未创建或服务未就绪），降级为普通诊断: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 更新知识库统计信息
     */
    public void updateKnowledgeUsage(String knowledgeId) {
        try {
            // 模拟更新使用统计
            log.info("更新知识点使用统计: {}", knowledgeId);
        } catch (Exception e) {
            log.error("更新知识库统计失败", e);
        }
    }

    /**
     * 获取知识库健康状态
     */
    public Map<String, Object> getHealthStatus() {
        try {
            String healthResult = webClient.get()
                .uri("/api/v1/heartbeat")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return Map.of(
                "status", "online",
                "collection", COLLECTION_NAME,
                "timestamp", System.currentTimeMillis(),
                "details", healthResult != null ? "连接正常" : "连接异常"
            );
        } catch (Exception e) {
            log.error("获取向量数据库状态失败", e);
            return Map.of(
                "status", "offline",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
}