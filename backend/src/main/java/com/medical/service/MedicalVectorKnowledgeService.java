package com.medical.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 医疗向量知识库服务
 * 管理"症状-科室-设备"语义关联数据
 */
@Service
@Slf4j
public class MedicalVectorKnowledgeService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    
    public MedicalVectorKnowledgeService(
            EmbeddingStore<TextSegment> embeddingStore,
            @org.springframework.beans.factory.annotation.Autowired(required = false) EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 初始化医疗知识库
     */
    @PostConstruct
    public void initializeMedicalKnowledge() {
        if (embeddingModel == null) {
            log.info("知识库未启用（需启动ChromaDB）");
            return;
        }
        
        log.info("开始初始化医疗向量知识库...");
        
        // 构建症状-科室-设备关联知识
        List<MedicalKnowledge> knowledgeList = buildMedicalKnowledgeBase();
        
        // 向量化并存储到Chroma DB
        for (MedicalKnowledge knowledge : knowledgeList) {
            storeKnowledge(knowledge);
        }
        
        log.info("医疗向量知识库初始化完成，共存储 {} 条知识", knowledgeList.size());
    }

    /**
     * 构建医疗知识库数据
     */
    private List<MedicalKnowledge> buildMedicalKnowledgeBase() {
        List<MedicalKnowledge> knowledgeList = new ArrayList<>();

        // 心血管科知识
        knowledgeList.add(new MedicalKnowledge(
            "胸痛、胸闷、心悸、呼吸困难、心律不齐",
            "心血管内科",
            "心电图机、超声心动图、动态心电图、血压监测仪、除颤器",
            "心血管疾病常见症状包括胸痛、胸闷、心悸等，需要心电图和超声心动图检查",
            "cardiovascular"
        ));

        knowledgeList.add(new MedicalKnowledge(
            "急性心肌梗死、心绞痛、心律失常",
            "心血管内科",
            "心电图机、除颤器、心脏监护仪、血管造影设备",
            "急性心血管事件需要立即心电图检查和持续心脏监护",
            "cardiovascular_emergency"
        ));

        // 呼吸科知识
        knowledgeList.add(new MedicalKnowledge(
            "咳嗽、咳痰、呼吸困难、胸痛、发热",
            "呼吸内科",
            "胸部X光机、CT扫描仪、肺功能仪、血氧监测仪、呼吸机",
            "呼吸系统疾病常见症状，需要胸部影像学检查和肺功能评估",
            "respiratory"
        ));

        knowledgeList.add(new MedicalKnowledge(
            "肺炎、哮喘、慢阻肺、肺栓塞",
            "呼吸内科",
            "CT扫描仪、血氧监测仪、呼吸机、雾化器",
            "呼吸系统疾病需要影像学检查和呼吸功能监测",
            "respiratory_disease"
        ));

        // 神经科知识
        knowledgeList.add(new MedicalKnowledge(
            "头痛、头晕、意识障碍、肢体无力、言语障碍",
            "神经内科",
            "CT扫描仪、MRI、脑电图机、颅内压监测仪",
            "神经系统症状需要神经影像学检查和神经电生理检查",
            "neurology"
        ));

        knowledgeList.add(new MedicalKnowledge(
            "脑梗死、脑出血、癫痫、帕金森病",
            "神经内科",
            "MRI、CT扫描仪、脑电图机、经颅多普勒",
            "神经系统疾病需要高精度脑部影像学检查",
            "neurology_disease"
        ));

        // 消化科知识
        knowledgeList.add(new MedicalKnowledge(
            "腹痛、恶心、呕吐、腹泻、便血",
            "消化内科",
            "胃镜、肠镜、腹部超声、CT扫描仪、X光机",
            "消化系统症状需要内镜检查和腹部影像学评估",
            "gastroenterology"
        ));

        knowledgeList.add(new MedicalKnowledge(
            "急性阑尾炎、肠梗阻、消化道出血、胆囊炎",
            "消化内科",
            "腹部超声、CT扫描仪、胃镜、肠镜",
            "急腹症需要紧急腹部影像学检查和内镜评估",
            "gastroenterology_emergency"
        ));

        // 骨科知识
        knowledgeList.add(new MedicalKnowledge(
            "骨折、关节疼痛、肢体活动受限、外伤",
            "骨科",
            "X光机、CT扫描仪、MRI、骨密度仪",
            "骨骼肌肉系统损伤需要X光和CT检查评估",
            "orthopedics"
        ));

        // 妇产科知识
        knowledgeList.add(new MedicalKnowledge(
            "腹痛、阴道出血、妊娠相关症状",
            "妇产科",
            "超声诊断仪、胎心监护仪、阴道镜",
            "妇科症状需要超声检查和妇科专科设备",
            "gynecology"
        ));

        // 儿科知识
        knowledgeList.add(new MedicalKnowledge(
            "发热、咳嗽、呕吐、腹泻、皮疹",
            "儿科",
            "儿童体温计、儿童血压计、儿童超声、儿童X光机",
            "儿科患者需要专用的儿童医疗设备",
            "pediatrics"
        ));

        // 急诊科知识
        knowledgeList.add(new MedicalKnowledge(
            "外伤、中毒、休克、心跳呼吸骤停",
            "急诊科",
            "除颤器、呼吸机、心电监护仪、急救车、输液泵",
            "急诊患者需要生命支持设备和急救监护",
            "emergency"
        ));

        return knowledgeList;
    }

    /**
     * 存储知识到向量数据库
     */
    private void storeKnowledge(MedicalKnowledge knowledge) {
        if (embeddingModel == null) {
            return;
        }
        
        try {
            // 创建文本段
            String content = String.format(
                "症状：%s\n科室：%s\n设备：%s\n描述：%s",
                knowledge.getSymptoms(),
                knowledge.getDepartment(),
                knowledge.getEquipments(),
                knowledge.getDescription()
            );

            // 创建元数据
            Map<String, String> metadataMap = new HashMap<>();
            metadataMap.put("symptoms", knowledge.getSymptoms());
            metadataMap.put("department", knowledge.getDepartment());
            metadataMap.put("equipments", knowledge.getEquipments());
            metadataMap.put("category", knowledge.getCategory());
            metadataMap.put("type", "medical_knowledge");
            
            Metadata metadata = Metadata.from(metadataMap);

            TextSegment segment = TextSegment.from(content, metadata);

            // 生成嵌入向量
            Embedding embedding = embeddingModel.embed(segment).content();

            // 存储到向量数据库
            embeddingStore.add(embedding, segment);

            log.debug("存储医疗知识: {} -> {}", knowledge.getCategory(), knowledge.getDepartment());

        } catch (Exception e) {
            log.error("存储医疗知识失败: {}", knowledge.getCategory(), e);
        }
    }

    /**
     * 根据症状检索相关科室和设备
     */
    public List<MedicalRecommendation> searchBySymptoms(String symptoms) {
        try {
            // 生成查询向量
            Embedding queryEmbedding = embeddingModel.embed(symptoms).content();

            // 向量检索
            var searchResults = embeddingStore.findRelevant(queryEmbedding, 3, 0.6);

            List<MedicalRecommendation> recommendations = new ArrayList<>();
            for (var result : searchResults) {
                TextSegment segment = result.embedded();
                Metadata metadata = segment.metadata();

                MedicalRecommendation recommendation = new MedicalRecommendation();
                recommendation.setSymptoms(metadata.get("symptoms") != null ? metadata.get("symptoms").toString() : "");
                recommendation.setDepartment(metadata.get("department") != null ? metadata.get("department").toString() : "");
                recommendation.setEquipments(metadata.get("equipments") != null ? metadata.get("equipments").toString() : "");
                recommendation.setConfidence(result.score());
                recommendation.setReason("基于症状语义匹配");

                recommendations.add(recommendation);
            }

            return recommendations;

        } catch (Exception e) {
            log.error("症状检索失败: {}", symptoms, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 搜索相似病例（用于EdgeCloudCollaborativeService）
     */
    public Map<String, Object> searchSimilarCases(String chiefComplaint, String vitalSigns) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 组合查询文本
            String queryText = String.format("主诉: %s, 生命体征: %s", 
                chiefComplaint != null ? chiefComplaint : "", 
                vitalSigns != null ? vitalSigns : "");
            
            // 生成查询向量
            Embedding queryEmbedding = embeddingModel.embed(queryText).content();
            
            // 向量检索
            var searchResults = embeddingStore.findRelevant(queryEmbedding, 5, 0.5);
            
            List<Map<String, Object>> similarCases = new ArrayList<>();
            for (var searchResult : searchResults) {
                TextSegment segment = searchResult.embedded();
                Metadata metadata = segment.metadata();
                
                Map<String, Object> caseInfo = new HashMap<>();
                caseInfo.put("symptoms", metadata.get("symptoms") != null ? metadata.get("symptoms").toString() : "");
                caseInfo.put("department", metadata.get("department") != null ? metadata.get("department").toString() : "");
                caseInfo.put("equipments", metadata.get("equipments") != null ? metadata.get("equipments").toString() : "");
                caseInfo.put("similarity", searchResult.score());
                
                similarCases.add(caseInfo);
            }
            
            result.put("similarCases", similarCases);
            result.put("count", similarCases.size());
            result.put("enhancementApplied", true);
            
        } catch (Exception e) {
            log.error("搜索相似病例失败", e);
            result.put("similarCases", new ArrayList<>());
            result.put("count", 0);
            result.put("enhancementApplied", false);
        }
        
        return result;
    }
    
    /**
     * 更新知识库（用于EdgeCloudCollaborativeService）
     */
    public void updateKnowledgeBase(String symptoms, String diagnosis, Double confidence) {
        try {
            // 只存储高置信度的病例
            if (confidence != null && confidence > 0.8) {
                String content = String.format(
                    "症状：%s\n诊断：%s\n置信度：%.2f",
                    symptoms != null ? symptoms : "",
                    diagnosis != null ? diagnosis : "",
                    confidence
                );
                
                Map<String, String> metadataMap = new HashMap<>();
                metadataMap.put("symptoms", symptoms != null ? symptoms : "");
                metadataMap.put("diagnosis", diagnosis != null ? diagnosis : "");
                metadataMap.put("confidence", String.valueOf(confidence));
                metadataMap.put("type", "clinical_case");
                metadataMap.put("timestamp", java.time.LocalDateTime.now().toString());
                
                Metadata metadata = Metadata.from(metadataMap);
                TextSegment segment = TextSegment.from(content, metadata);
                
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
                
                log.info("知识库更新成功: 症状={}, 诊断={}, 置信度={}", symptoms, diagnosis, confidence);
            }
        } catch (Exception e) {
            log.error("知识库更新失败", e);
        }
    }

    /**
     * 添加新的医疗知识
     */
    public void addKnowledge(String symptoms, String department, String equipments, String description, String category) {
        MedicalKnowledge knowledge = new MedicalKnowledge(symptoms, department, equipments, description, category);
        storeKnowledge(knowledge);
    }

    /**
     * 医疗知识数据结构
     */
    public static class MedicalKnowledge {
        private String symptoms;      // 症状
        private String department;    // 科室
        private String equipments;    // 设备
        private String description;   // 描述
        private String category;      // 分类

        public MedicalKnowledge(String symptoms, String department, String equipments, 
                               String description, String category) {
            this.symptoms = symptoms;
            this.department = department;
            this.equipments = equipments;
            this.description = description;
            this.category = category;
        }

        // Getters
        public String getSymptoms() { return symptoms; }
        public String getDepartment() { return department; }
        public String getEquipments() { return equipments; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
    }

    /**
     * 医疗推荐结果
     */
    public static class MedicalRecommendation {
        private String symptoms;
        private String department;
        private String equipments;
        private double confidence;
        private String reason;

        // Getters and Setters
        public String getSymptoms() { return symptoms; }
        public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getEquipments() { return equipments; }
        public void setEquipments(String equipments) { this.equipments = equipments; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}