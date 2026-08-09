package com.medical.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 边缘设备数据接收实体
 */
@Entity
@Table(name = "edge_device_data")
public class EdgeDeviceData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_id", nullable = false, length = 50)
    private String deviceId; // Jetson设备ID
    
    @Column(name = "patient_temp_id", length = 50)
    private String patientTempId; // 临时患者ID
    
    // ===== 患者基本信息 =====
    @Column(name = "patient_name", length = 100)
    private String patientName;
    
    @Column(name = "patient_age")
    private Integer patientAge;
    
    @Column(name = "patient_gender", length = 20)
    private String patientGender;
    
    @Column(name = "patient_id_card", length = 500)
    private String patientIdCard;
    
    @Column(name = "patient_phone", length = 500)
    private String patientPhone;
    
    // ===== 传感器数据 =====
    @Column(name = "temperature")
    private Double temperature; // DS18B20体温数据
    
    @Column(name = "heart_rate")
    private Integer heartRate; // MAX30102心率数据
    
    @Column(name = "blood_oxygen")
    private Integer bloodOxygen; // MAX30102血氧数据
    
    @Column(name = "systolic_bp")
    private Integer systolicBP; // 血压计收缩压数据
    
    @Column(name = "diastolic_bp")
    private Integer diastolicBP; // 血压计舒张压数据
    
    // ===== 语音数据 =====
    @Column(name = "voice_text", columnDefinition = "TEXT")
    private String voiceText; // 讯飞语音转文字结果
    
    @Column(name = "voice_complaint", columnDefinition = "TEXT")
    private String voiceComplaint; // 语音主诉
    
    @Column(name = "voice_confidence")
    private Double voiceConfidence; // 语音识别置信度
    
    // ===== 生命体征综合 =====
    @Column(name = "vital_signs", columnDefinition = "TEXT")
    private String vitalSigns; // 生命体征JSON字符串
    
    // ===== 边缘端AI分诊结果 =====
    @Column(name = "triage_level")
    private Integer triageLevel; // 边缘端AI分诊等级 (1-5)
    
    @Column(name = "triage_score")
    private Double triageScore; // 分诊评分
    
    @Column(name = "triage_priority", length = 20)
    private String triagePriority; // 轻重缓急：濒危、危急、急症、次急症、非急症
    
    @Column(name = "triage_color", length = 20)
    private String triageColor; // 分诊颜色：红色、橙色、黄色、绿色、蓝色
    
    @Column(name = "wait_time", length = 30)
    private String waitTime; // 处理时限：立即处理、10分钟内、30分钟内等
    
    @Column(name = "triage_confidence")
    private Double triageConfidence; // 分诊置信度
    
    // ===== AI诊断结果 =====
    @Column(name = "ai_diagnosis", columnDefinition = "TEXT")
    private String aiDiagnosis; // 边缘端AI初步诊断
    
    @Column(name = "ai_confidence")
    private Double aiConfidence; // AI诊断置信度
    
    @Column(name = "edge_processing_time")
    private Long edgeProcessingTime; // 边缘端处理时间(ms)
    
    // ===== 原始数据和状态 =====
    @Column(name = "raw_sensor_data", columnDefinition = "JSON")
    private String rawSensorData; // 原始传感器数据JSON
    
    @Column(name = "device_status", length = 20)
    private String deviceStatus; // ONLINE, OFFLINE, ERROR
    
    @Column(name = "data_quality_score")
    private Double dataQualityScore; // 数据质量评分
    
    @Column(name = "received_time")
    private LocalDateTime receivedTime; // 云端接收时间
    
    @Column(name = "processed", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean processed; // 是否已处理
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // 错误信息
    
    @Column(name = "processing_status", length = 30)
    private String processingStatus; // RECEIVED, WAITING_REASSESSMENT, FINAL_COMPLETED
    
    @Column(name = "final_diagnosis", columnDefinition = "TEXT")
    private String finalDiagnosis; // 最终AI诊断结果JSON
    
    // ===== EdgeCloudCollaborativeService 需要的额外字段 =====
    @Column(name = "timestamp")
    private LocalDateTime timestamp; // 数据时间戳
    
    @Column(name = "symptom_text", columnDefinition = "TEXT")
    private String symptomText; // 症状文本描述
    
    @Column(name = "consciousness")
    private Integer consciousness; // 意识状态等级
    
    @Column(name = "confidence")
    private Double confidence; // 总体置信度
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs; // 处理时间(毫秒)
    
    @Column(name = "systolic_blood_pressure")
    private Integer systolicBloodPressure; // 收缩压（用于协同服务）
    
    @Column(name = "diastolic_blood_pressure")
    private Integer diastolicBloodPressure; // 舒张压（用于协同服务）
    
    @Column(name = "respiratory_rate")
    private Integer respiratoryRate; // 呼吸频率
    
    @Column(name = "created_at")
    private LocalDateTime createdAt; // 创建时间
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 更新时间
    
    // 构造函数
    public EdgeDeviceData() {
        this.receivedTime = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.processed = false;
        this.deviceStatus = "ONLINE";
        this.processingStatus = "RECEIVED";
    }
    
    // ===== Getters and Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getPatientTempId() { return patientTempId; }
    public void setPatientTempId(String patientTempId) { this.patientTempId = patientTempId; }
    
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    
    public Integer getPatientAge() { return patientAge; }
    public void setPatientAge(Integer patientAge) { this.patientAge = patientAge; }
    
    public String getPatientGender() { return patientGender; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }
    
    public String getPatientIdCard() { return patientIdCard; }
    public void setPatientIdCard(String patientIdCard) { this.patientIdCard = patientIdCard; }
    
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    
    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }
    
    public Integer getBloodOxygen() { return bloodOxygen; }
    public void setBloodOxygen(Integer bloodOxygen) { this.bloodOxygen = bloodOxygen; }
    
    public Integer getSystolicBP() { return systolicBP; }
    public void setSystolicBP(Integer systolicBP) { this.systolicBP = systolicBP; }
    
    public Integer getDiastolicBP() { return diastolicBP; }
    public void setDiastolicBP(Integer diastolicBP) { this.diastolicBP = diastolicBP; }
    
    public String getVoiceText() { return voiceText; }
    public void setVoiceText(String voiceText) { this.voiceText = voiceText; }
    
    public String getVoiceComplaint() { return voiceComplaint; }
    public void setVoiceComplaint(String voiceComplaint) { this.voiceComplaint = voiceComplaint; }
    
    public Double getVoiceConfidence() { return voiceConfidence; }
    public void setVoiceConfidence(Double voiceConfidence) { this.voiceConfidence = voiceConfidence; }
    
    public String getVitalSigns() { return vitalSigns; }
    public void setVitalSigns(String vitalSigns) { this.vitalSigns = vitalSigns; }
    
    public Integer getTriageLevel() { return triageLevel; }
    public void setTriageLevel(Integer triageLevel) { this.triageLevel = triageLevel; }
    
    public Double getTriageScore() { return triageScore; }
    public void setTriageScore(Double triageScore) { this.triageScore = triageScore; }
    
    public String getTriagePriority() { return triagePriority; }
    public void setTriagePriority(String triagePriority) { this.triagePriority = triagePriority; }
    
    public String getTriageColor() { return triageColor; }
    public void setTriageColor(String triageColor) { this.triageColor = triageColor; }
    
    public String getWaitTime() { return waitTime; }
    public void setWaitTime(String waitTime) { this.waitTime = waitTime; }
    
    public Double getTriageConfidence() { return triageConfidence; }
    public void setTriageConfidence(Double triageConfidence) { this.triageConfidence = triageConfidence; }
    
    public String getAiDiagnosis() { return aiDiagnosis; }
    public void setAiDiagnosis(String aiDiagnosis) { this.aiDiagnosis = aiDiagnosis; }
    
    public Double getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(Double aiConfidence) { this.aiConfidence = aiConfidence; }
    
    public Long getEdgeProcessingTime() { return edgeProcessingTime; }
    public void setEdgeProcessingTime(Long edgeProcessingTime) { this.edgeProcessingTime = edgeProcessingTime; }
    
    public String getRawSensorData() { return rawSensorData; }
    public void setRawSensorData(String rawSensorData) { this.rawSensorData = rawSensorData; }
    
    public String getDeviceStatus() { return deviceStatus; }
    public void setDeviceStatus(String deviceStatus) { this.deviceStatus = deviceStatus; }
    
    public Double getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(Double dataQualityScore) { this.dataQualityScore = dataQualityScore; }
    
    public LocalDateTime getReceivedTime() { return receivedTime; }
    public void setReceivedTime(LocalDateTime receivedTime) { this.receivedTime = receivedTime; }
    
    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }
    
    public String getFinalDiagnosis() { return finalDiagnosis; }
    public void setFinalDiagnosis(String finalDiagnosis) { this.finalDiagnosis = finalDiagnosis; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // ===== EdgeCloudCollaborativeService 额外字段的 Getters and Setters =====
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getSymptomText() { return symptomText; }
    public void setSymptomText(String symptomText) { this.symptomText = symptomText; }
    
    public Integer getConsciousness() { return consciousness; }
    public void setConsciousness(Integer consciousness) { this.consciousness = consciousness; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public Integer getSystolicBloodPressure() { return systolicBloodPressure; }
    public void setSystolicBloodPressure(Integer systolicBloodPressure) { this.systolicBloodPressure = systolicBloodPressure; }
    
    public Integer getDiastolicBloodPressure() { return diastolicBloodPressure; }
    public void setDiastolicBloodPressure(Integer diastolicBloodPressure) { this.diastolicBloodPressure = diastolicBloodPressure; }
    
    public Integer getRespiratoryRate() { return respiratoryRate; }
    public void setRespiratoryRate(Integer respiratoryRate) { this.respiratoryRate = respiratoryRate; }
}
