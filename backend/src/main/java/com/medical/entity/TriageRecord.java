package com.medical.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "triage_records")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TriageRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Patient patient;
    
    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;
    
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;
    
    @Column(name = "vital_signs", columnDefinition = "JSON")
    private String vitalSigns;
    
    @Column(name = "triage_level")
    private Integer triageLevel = 4; // 1=濒危,2=危急,3=急症,4=次急症,5=非急症
    
    @Column(name = "triage_priority")
    private String triagePriority; // 轻重缓急：濒危、危急、急症、次急症、非急症
    
    @Column(name = "triage_color")
    private String triageColor; // 分诊颜色：红色、橙色、黄色、绿色、蓝色
    
    @Column(name = "wait_time")
    private String waitTime; // 处理时限：立即处理、10分钟内、30分钟内、60分钟内、120分钟或预约
    
    @Column(name = "triage_score", precision = 3, scale = 2)
    private BigDecimal triageScore = BigDecimal.ZERO;
    
    @Column(name = "assigned_department")
    private String assignedDepartment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_doctor_id")
    private User assignedDoctor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_nurse_id")
    private User assignedNurse;
    
    @Column(name = "ai_diagnosis", columnDefinition = "TEXT")
    private String aiDiagnosis; // 保留用于云端大模型诊断，边缘AI不再使用此字段
    
    @Column(name = "ai_confidence", precision = 3, scale = 2)
    private BigDecimal aiConfidence = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    private TriageStatus status = TriageStatus.WAITING;
    
    /**
     * 数据来源 (EDGE_DEVICE, MANUAL)
     */
    @Column(name = "data_source")
    private String dataSource = "MANUAL";
    
    /**
     * 边缘设备ID（用于标识数据来源设备）
     */
    @Column(name = "edge_device_id")
    private String edgeDeviceId;
    
    /**
     * 分诊来源（用于EdgeCloudCollaborativeService）
     */
    @Column(name = "triage_source")
    private String triageSource; // 如："边缘AI"、"云端AI"、"护士手动"等
    
    /**
     * 护士备注
     */
    @Column(name = "nurse_comments", columnDefinition = "TEXT")
    private String nurseComments;
    
    /**
     * 确认时间
     */
    @Column(name = "confirmed_time")
    private LocalDateTime confirmedTime;
    
    /**
     * 检查结果
     */
    @Column(name = "lab_results", columnDefinition = "JSON")
    private String labResults;
    
    /**
     * 临时患者ID（用于边缘设备数据）
     */
    @Column(name = "patient_temp_id")
    private String patientTempId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (arrivalTime == null) {
            arrivalTime = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // ===== 辅助方法 =====
    
    /**
     * 设置分诊评分（支持Double类型）
     */
    public void setTriageScore(Double score) {
        if (score != null) {
            this.triageScore = BigDecimal.valueOf(score);
        }
    }
    
    /**
     * 设置AI置信度（支持Double类型）
     */
    public void setAiConfidence(Double confidence) {
        if (confidence != null) {
            this.aiConfidence = BigDecimal.valueOf(confidence);
        }
    }
    
    /**
     * 获取症状描述（从主诉中提取）
     */
    public String getSymptoms() {
        return this.chiefComplaint != null ? this.chiefComplaint : "";
    }
    
    /**
     * 设置护士备注（兼容方法）
     */
    public void setNurseNotes(String notes) {
        this.nurseComments = notes;
    }
    
    /**
     * 获取护士备注（兼容方法）
     */
    public String getNurseNotes() {
        return this.nurseComments;
    }
    
    // 手动添加getter方法（解决IDE Lombok识别问题）
    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public String getChiefComplaint() { return chiefComplaint; }
    public Integer getTriageLevel() { return triageLevel; }
    public String getAssignedDepartment() { return assignedDepartment; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public LocalDateTime getConfirmedTime() { return confirmedTime; }
    public String getVitalSigns() { return vitalSigns; }
    public String getAiDiagnosis() { return aiDiagnosis; }
    public String getDataSource() { return dataSource; }
    public String getEdgeDeviceId() { return edgeDeviceId; }
    public User getAssignedDoctor() { return assignedDoctor; }
    public User getAssignedNurse() { return assignedNurse; }
    public BigDecimal getTriageScore() { return triageScore; }
    public BigDecimal getAiConfidence() { return aiConfidence; }
    public String getNurseComments() { return nurseComments; }
    public TriageStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    // 手动添加setter方法
    public void setId(Long id) { this.id = id; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public void setStatus(TriageStatus status) { this.status = status; }
    public void setAssignedDoctor(User doctor) { this.assignedDoctor = doctor; }
    public void setAssignedDepartment(String dept) { this.assignedDepartment = dept; }
    public void setTriageLevel(Integer level) { this.triageLevel = level; }
    public void setChiefComplaint(String complaint) { this.chiefComplaint = complaint; }
    public void setVitalSigns(String signs) { this.vitalSigns = signs; }
    public void setAiDiagnosis(String diagnosis) { this.aiDiagnosis = diagnosis; }
    public void setEdgeDeviceId(String id) { this.edgeDeviceId = id; }
    public void setDataSource(String source) { this.dataSource = source; }
    public void setAssignedNurse(User nurse) { this.assignedNurse = nurse; }
    public void setConfirmedTime(LocalDateTime time) { this.confirmedTime = time; }
    public void setArrivalTime(LocalDateTime time) { this.arrivalTime = time; }
    public void setNurseComments(String comments) { this.nurseComments = comments; }
    
    public enum TriageStatus {
        WAITING("等待分诊"),
        PENDING_CONFIRMATION("待确认"),
        CONFIRMED("已确认"),
        IN_PROGRESS("诊疗中"),
        COMPLETED("已完成"),
        CANCELLED("已取消"),
        PENDING_RETRIAGE("待重新分诊");

        private final String description;

        TriageStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}