package com.medical.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "diagnosis_records")
@EqualsAndHashCode(callSuper = false)
public class DiagnosisRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triage_id", nullable = false)
    private TriageRecord triageRecord;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnosis;
    
    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;
    
    @Column(columnDefinition = "TEXT")
    private String prescription;
    
    @Column(name = "follow_up_instructions", columnDefinition = "TEXT")
    private String followUpInstructions;
    
    @Column(name = "diagnosis_time")
    private LocalDateTime diagnosisTime;
    
    @Enumerated(EnumType.STRING)
    private DiagnosisStatus status = DiagnosisStatus.DRAFT;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (diagnosisTime == null) {
            diagnosisTime = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum DiagnosisStatus {
        DRAFT, COMPLETED, REVIEWED
    }
    
    // 手动添加getter/setter方法（解决IDE Lombok识别问题）
    public Long getId() { return id; }
    public TriageRecord getTriageRecord() { return triageRecord; }
    public User getDoctor() { return doctor; }
    public String getDiagnosis() { return diagnosis; }
    public String getTreatmentPlan() { return treatmentPlan; }
    public String getPrescription() { return prescription; }
    public String getFollowUpInstructions() { return followUpInstructions; }
    public LocalDateTime getDiagnosisTime() { return diagnosisTime; }
    public DiagnosisStatus getStatus() { return status; }
    
    public void setId(Long id) { this.id = id; }
    public void setTriageRecord(TriageRecord triageRecord) { this.triageRecord = triageRecord; }
    public void setDoctor(User doctor) { this.doctor = doctor; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public void setTreatmentPlan(String treatmentPlan) { this.treatmentPlan = treatmentPlan; }
    public void setPrescription(String prescription) { this.prescription = prescription; }
    public void setFollowUpInstructions(String followUpInstructions) { this.followUpInstructions = followUpInstructions; }
    public void setDiagnosisTime(LocalDateTime diagnosisTime) { this.diagnosisTime = diagnosisTime; }
    public void setStatus(DiagnosisStatus status) { this.status = status; }
}