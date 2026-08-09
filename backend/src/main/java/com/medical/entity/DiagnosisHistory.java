package com.medical.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 诊断历史记录实体
 */
@Data
@Entity
@Table(name = "diagnosis_history")
public class DiagnosisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分诊记录ID
     */
    @Column(name = "triage_record_id")
    private Long triageRecordId;

    /**
     * 患者ID
     */
    @Column(name = "patient_id")
    private Long patientId;

    /**
     * 医生ID
     */
    @Column(name = "doctor_id")
    private Long doctorId;

    /**
     * 医生姓名
     */
    @Column(name = "doctor_name")
    private String doctorName;

    /**
     * 诊断结果
     */
    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    /**
     * 治疗方案
     */
    @Column(columnDefinition = "TEXT")
    private String treatment;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 诊断时间
     */
    @Column(name = "diagnosis_time")
    private LocalDateTime diagnosisTime;

    /**
     * AI诊断建议
     */
    @Column(name = "ai_diagnosis", columnDefinition = "TEXT")
    private String aiDiagnosis;

    /**
     * 置信度
     */
    @Column(name = "confidence_score")
    private Double confidenceScore;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (diagnosisTime == null) {
            diagnosisTime = LocalDateTime.now();
        }
    }
}
