package com.medical.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI诊断结果实体类
 */
@Entity
@Table(name = "diagnosis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triage_record_id", nullable = false)
    private TriageRecord triageRecord;

    @Column(name = "preliminary_diagnosis", columnDefinition = "TEXT")
    private String preliminaryDiagnosis;

    @Column(name = "diagnosis_confidence", precision = 5, scale = 4)
    private BigDecimal diagnosisConfidence;

    @Column(name = "symptoms_analysis", columnDefinition = "TEXT")
    private String symptomsAnalysis;

    @Column(name = "vital_signs_analysis", columnDefinition = "TEXT")
    private String vitalSignsAnalysis;

    @Column(name = "medical_history_analysis", columnDefinition = "TEXT")
    private String medicalHistoryAnalysis;

    @Column(name = "recommended_examinations", columnDefinition = "TEXT")
    private String recommendedExaminations;

    @Column(name = "treatment_suggestions", columnDefinition = "TEXT")
    private String treatmentSuggestions;

    @Column(name = "risk_assessment", columnDefinition = "TEXT")
    private String riskAssessment;

    @Column(name = "urgency_level")
    private Integer urgencyLevel;

    @Column(name = "ai_model_used")
    private String aiModelUsed;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DiagnosisStatus status = DiagnosisStatus.PENDING;

    public enum DiagnosisStatus {
        PENDING, CONFIRMED, REVISED, REJECTED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}