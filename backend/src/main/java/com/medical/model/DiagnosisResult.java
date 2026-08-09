package com.medical.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * AI诊断结果模型类（用于业务逻辑）
 * 注意：这是一个DTO类，不是JPA实体，不需要@Embeddable
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResult {

    private Long id;
    private String primaryDiagnosis;
    private Double confidence;
    private String symptomAnalysis;
    private String vitalSignsAnalysis;
    private String medicalHistoryAnalysis;
    private List<String> differentialDiagnosis;
    private List<String> recommendedExams;
    private String treatmentRecommendation;
    private String urgencyLevel;
    private String modelVersion;
    private String triageLevel;
    private Date diagnosisTime;
    private Boolean success;
    private String errorMessage;
    private Long processingTimeMs;
    
    /**
     * 判断诊断是否成功
     */
    public boolean isSuccess() {
        return success != null && success;
    }
    
    // 手动添加getter方法（解决IDE Lombok识别问题）
    public Long getId() { return id; }
    public String getPrimaryDiagnosis() { return primaryDiagnosis; }
    public Double getConfidence() { return confidence; }
    public String getSymptomAnalysis() { return symptomAnalysis; }
    public String getVitalSignsAnalysis() { return vitalSignsAnalysis; }
    public String getMedicalHistoryAnalysis() { return medicalHistoryAnalysis; }
    public List<String> getDifferentialDiagnosis() { return differentialDiagnosis; }
    public List<String> getRecommendedExams() { return recommendedExams; }
    public String getTreatmentRecommendation() { return treatmentRecommendation; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public String getModelVersion() { return modelVersion; }
    public String getTriageLevel() { return triageLevel; }
    public Date getDiagnosisTime() { return diagnosisTime; }
    public String getErrorMessage() { return errorMessage; }
    public Long getProcessingTimeMs() { return processingTimeMs; }

}