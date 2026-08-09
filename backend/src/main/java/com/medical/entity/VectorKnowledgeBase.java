package com.medical.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 向量知识库实体
 * 存储医疗知识的向量表示，用于RAG检索
 */
@Entity
@Table(name = "vector_knowledge_base")
@Data
public class VectorKnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "knowledge_type", length = 50)
    private String knowledgeType; // 知识类型：disease(疾病), symptom(症状), department(科室), equipment(设备)

    @Column(name = "title", length = 500)
    private String title; // 知识标题

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // 知识内容

    @Column(name = "department", length = 100)
    private String department; // 关联科室

    @Column(name = "disease_category", length = 100)
    private String diseaseCategory; // 疾病分类

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms; // 相关症状（逗号分隔）

    @Column(name = "equipments", columnDefinition = "TEXT")
    private String equipments; // 相关设备（逗号分隔）

    @Column(name = "vector_embedding", columnDefinition = "TEXT")
    private String vectorEmbedding; // 向量表示（JSON格式）

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel; // 使用的嵌入模型

    @Column(name = "priority")
    private Integer priority; // 优先级（数字越大越优先）

    @Column(name = "status", length = 20)
    private String status; // 状态:active, inactive
    
    @Column(name = "is_active")
    private Boolean isActive; // 是否激活
    
    @Column(name = "usage_count")
    private Integer usageCount; // 使用次数
    
    @Column(name = "accuracy_score")
    private Double accuracyScore; // 准确率评分

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "active";
        }
        if (isActive == null) {
            isActive = true;
        }
        if (priority == null) {
            priority = 0;
        }
        if (usageCount == null) {
            usageCount = 0;
        }
        if (accuracyScore == null) {
            accuracyScore = 1.0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
