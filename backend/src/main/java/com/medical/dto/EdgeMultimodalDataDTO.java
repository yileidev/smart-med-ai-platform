package com.medical.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 边缘设备多模态数据传输对象
 * 
 * 标准化边缘设备发送的多模态医疗数据格式，
 * 支持传感器数据和语音转文字结果的统一传输。
 * 
 * 设计符合医疗物联网标准，确保数据完整性和实时性。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "边缘设备多模态数据")
public class EdgeMultimodalDataDTO {

    @NotNull(message = "设备ID不能为空")
    @Schema(description = "边缘设备唯一标识", example = "JETSON-001")
    private String deviceId;

    @Schema(description = "数据采集时间", example = "2025-01-15T10:30:45")
    private LocalDateTime timestamp;

    @NotNull(message = "传感器数据不能为空")
    @Schema(description = "传感器数据集合")
    private SensorDataDTO sensorData;

    @Schema(description = "语音转文字结果")
    private SpeechDataDTO speechData;

    @Schema(description = "患者基本信息")
    private PatientBasicInfoDTO patientInfo;

    @Schema(description = "设备状态信息")
    private DeviceStatusDTO deviceStatus;

    /**
     * 传感器数据DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "传感器数据")
    public static class SensorDataDTO {

        @NotNull(message = "体温不能为空")
        @Min(value = 30, message = "体温值异常")
        @Max(value = 45, message = "体温值异常")
        @Schema(description = "体温(°C)", example = "37.5")
        private Double temperature;

        @NotNull(message = "收缩压不能为空")
        @Min(value = 60, message = "收缩压值异常")
        @Max(value = 250, message = "收缩压值异常")
        @Schema(description = "收缩压(mmHg)", example = "120")
        private Integer systolicBloodPressure;

        @NotNull(message = "舒张压不能为空")
        @Min(value = 40, message = "舒张压值异常")
        @Max(value = 150, message = "舒张压值异常")
        @Schema(description = "舒张压(mmHg)", example = "80")
        private Integer diastolicBloodPressure;

        @NotNull(message = "心率不能为空")
        @Min(value = 30, message = "心率值异常")
        @Max(value = 200, message = "心率值异常")
        @Schema(description = "心率(bpm)", example = "75")
        private Integer heartRate;

        @NotNull(message = "血氧饱和度不能为空")
        @Min(value = 70, message = "血氧值异常")
        @Max(value = 100, message = "血氧值异常")
        @Schema(description = "血氧饱和度(%)", example = "98")
        private Double bloodOxygen;

        @Min(value = 8, message = "呼吸频率值异常")
        @Max(value = 40, message = "呼吸频率值异常")
        @Schema(description = "呼吸频率(bpm)", example = "16")
        private Integer respiratoryRate;

        @Schema(description = "ECG数据(mV)")
        private Map<String, Double> ecgData;

        @Schema(description = "其他传感器数据")
        private Map<String, Object> additionalSensors;
        
        // 手动添加getter方法
        public Double getTemperature() { return temperature; }
        public Integer getSystolicBloodPressure() { return systolicBloodPressure; }
        public Integer getDiastolicBloodPressure() { return diastolicBloodPressure; }
        public Integer getHeartRate() { return heartRate; }
        public Double getBloodOxygen() { return bloodOxygen; }
        public Integer getRespiratoryRate() { return respiratoryRate; }
        public Map<String, Double> getEcgData() { return ecgData; }
        public Map<String, Object> getAdditionalSensors() { return additionalSensors; }
    }

    /**
     * 语音数据DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "语音转文字数据")
    public static class SpeechDataDTO {

        @Schema(description = "语音转文字结果", example = "我胸痛伴呼吸困难，持续了大约30分钟")
        private String transcribedText;

        @Schema(description = "语音置信度", example = "0.92")
        private Double confidence;

        @Schema(description = "语音时长(秒)", example = "15.5")
        private Double duration;

        @Schema(description = "语音语言", example = "zh-CN")
        private String language;

        @Schema(description = "语音文件路径")
        private String audioFilePath;
        
        // 手动添加getter方法
        public String getTranscribedText() { return transcribedText; }
        public Double getConfidence() { return confidence; }
        public Double getDuration() { return duration; }
        public String getLanguage() { return language; }
        public String getAudioFilePath() { return audioFilePath; }
    }

    /**
     * 患者基本信息DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "患者基本信息")
    public static class PatientBasicInfoDTO {

        @Schema(description = "患者姓名", example = "张三")
        private String name;

        @Schema(description = "年龄", example = "45")
        private Integer age;

        @Schema(description = "性别", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
        private String gender;

        @Schema(description = "身份证号", example = "320101199001011234")
        private String idNumber;

        @Schema(description = "联系电话", example = "13800138000")
        private String phoneNumber;

        @Schema(description = "过敏史", example = "青霉素过敏")
        private String allergies;

        @Schema(description = "既往病史", example = "高血压、糖尿病")
        private String medicalHistory;
    }

    /**
     * 设备状态DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "设备状态信息")
    public static class DeviceStatusDTO {

        @Schema(description = "设备状态", example = "ONLINE", allowableValues = {"ONLINE", "OFFLINE", "ERROR"})
        private String status;

        @Schema(description = "电池电量(%)", example = "85")
        private Integer batteryLevel;

        @Schema(description = "CPU使用率(%)", example = "45.2")
        private Double cpuUsage;

        @Schema(description = "内存使用率(%)", example = "67.8")
        private Double memoryUsage;

        @Schema(description = "网络信号强度", example = "GOOD")
        private String signalStrength;

        @Schema(description = "错误信息")
        private String errorMessage;

        @Schema(description = "系统信息")
        private Map<String, Object> systemInfo;
    }

    /**
     * 验证数据完整性
     */
    public boolean isValid() {
        return deviceId != null && !deviceId.trim().isEmpty() 
            && sensorData != null && sensorData.getTemperature() != null
            && sensorData.getSystolicBloodPressure() != null
            && sensorData.getDiastolicBloodPressure() != null
            && sensorData.getHeartRate() != null
            && sensorData.getBloodOxygen() != null;
    }

    /**
     * 获取数据摘要
     */
    public String getDataSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("设备: ").append(deviceId);
        
        if (sensorData != null) {
            summary.append(", 体温: ").append(sensorData.getTemperature()).append("°C");
            summary.append(", 血压: ").append(sensorData.getSystolicBloodPressure())
                  .append("/").append(sensorData.getDiastolicBloodPressure()).append("mmHg");
            summary.append(", 心率: ").append(sensorData.getHeartRate()).append("bpm");
            summary.append(", 血氧: ").append(sensorData.getBloodOxygen()).append("%");
        }
        
        if (speechData != null && speechData.getTranscribedText() != null) {
            String text = speechData.getTranscribedText();
            if (text.length() > 20) {
                text = text.substring(0, 20) + "...";
            }
            summary.append(", 主诉: ").append(text);
        }
        
        return summary.toString();
    }
}