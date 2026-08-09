package com.medical.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.entity.EdgeDeviceData;
import com.medical.entity.TriageRecord;
import com.medical.entity.Patient;
import com.medical.repository.EdgeDeviceDataRepository;
import com.medical.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * MQTT消息处理器 - 边缘-云端协同通信核心组件
 * 
 * 【系统定位】云端诊断与调度层的数据接入模块
 * 【技术栈】MQTT协议 + WebSocket推送 + Spring消息模板
 * 
 * 【核心职责】
 * 1. 接收边缘设备MQTT消息：分诊数据、设备状态、心跳信号
 * 2. 数据持久化：存储EdgeDeviceData到MySQL数据库
 * 3. 触发云端流程：创建TriageRecord，启动医疗大模型诊断
 * 4. 实时推送：通过WebSocket将数据推送到前端（护士/医生/管理员）
 * 5. 双向通信：处理护士修正数据，发回边缘端重新分诊
 * 
 * 【数据流向】
 * 边缘设备 -> MQTT -> MqttMessageHandler -> 云端服务 -> WebSocket -> 前端
 *                ↓
 *         EdgeDeviceData (MySQL)
 *                ↓
 *         TriageRecord + AI诊断
 * 
 * 【MQTT主题设计】
 * - medical/triage/data: 边缘分诊数据上报
 * - medical/triage/final: 边缘重新分诊结果
 * - medical/triage/correction/{deviceId}: 云端发送修正数据到边缘端
 * - medical/device/heartbeat: 设备心跳监控
 * 
 * @see EdgeDeviceData 边缘设备数据实体
 * @see TriageService 分诊服务
 * @see NurseTriageService 护士复核服务
 */
@Service
public class MqttMessageHandler implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageHandler.class);

    private final EdgeDataService edgeDataService;
    private final TriageService triageService;
    private final NurseTriageService nurseTriageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final EdgeDeviceDataRepository edgeDataRepository;
    private final EncryptionUtil encryptionUtil;
    
    public MqttMessageHandler(EdgeDataService edgeDataService,
                              TriageService triageService,
                              NurseTriageService nurseTriageService,
                              SimpMessagingTemplate messagingTemplate,
                              ObjectMapper objectMapper,
                              EdgeDeviceDataRepository edgeDataRepository,
                              EncryptionUtil encryptionUtil) {
        this.edgeDataService = edgeDataService;
        this.triageService = triageService;
        this.nurseTriageService = nurseTriageService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.edgeDataRepository = edgeDataRepository;
        this.encryptionUtil = encryptionUtil;
    }

    /**
     * 处理分诊数据消息
     */
    public void handleTriageMessage(String topic, String messagePayload) {
        try {
            log.info("收到边缘分诊数据: {}", messagePayload);
            
            // 解析JSON消息
            EdgeTriageMessage triageMessage = objectMapper.readValue(messagePayload, EdgeTriageMessage.class);
            
            // 创建边缘设备数据记录
            EdgeDeviceData edgeData = createEdgeDeviceData(triageMessage);
            
            // 保存边缘数据
            EdgeDeviceData savedData = edgeDataService.saveEdgeData(edgeData);
            
            // 创建患者和分诊记录
            TriageRecord triageRecord = createTriageRecordFromEdgeData(savedData);
            
            // 保存分诊记录（不再进行AI分诊，使用边缘端结果）
            TriageRecord savedRecord = triageService.saveTriageRecordFromEdge(triageRecord);
            
            // 实时推送到前端
            pushToFrontend(savedRecord, savedData);
            
            // 标记边缘数据已处理
            edgeDataService.markAsProcessed(savedData.getId());
            
            log.info("边缘分诊数据处理完成 - 设备ID: {}, 分诊等级: {}", 
                triageMessage.getDeviceId(), 
                triageMessage.getTriageResult() != null ? triageMessage.getTriageResult().getLevel() : "未知");
                
        } catch (Exception e) {
            log.error("处理分诊消息失败: {}", e.getMessage(), e);
            // 可以考虑发送错误通知到前端
            sendErrorNotification("分诊数据处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理设备状态消息
     */
    public void handleDeviceStatusMessage(String topic, String messagePayload) {
        try {
            log.info("收到设备状态更新: {}", messagePayload);
            
            DeviceStatusMessage statusMessage = objectMapper.readValue(messagePayload, DeviceStatusMessage.class);
            
            // 更新设备状态
            edgeDataService.updateDeviceStatus(
                statusMessage.getDeviceId(), 
                statusMessage.getStatus(),
                statusMessage.getErrorMessage()
            );
            
            // 推送设备状态更新到前端
            messagingTemplate.convertAndSend("/topic/device-status", statusMessage);
            
        } catch (Exception e) {
            log.error("处理设备状态消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理心跳消息
     */
    public void handleHeartbeatMessage(String topic, String messagePayload) {
        try {
            HeartbeatMessage heartbeat = objectMapper.readValue(messagePayload, HeartbeatMessage.class);
            
            // 更新设备最后心跳时间
            edgeDataService.updateDeviceHeartbeat(
                heartbeat.getDeviceId(),
                heartbeat.getTimestamp(),
                heartbeat.getSystemInfo()
            );
            
            log.debug("设备心跳更新: {}", heartbeat.getDeviceId());
            
        } catch (Exception e) {
            log.error("处理心跳消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建边缘设备数据记录
     */
    private EdgeDeviceData createEdgeDeviceData(EdgeTriageMessage message) {
        EdgeDeviceData edgeData = new EdgeDeviceData();
        edgeData.setDeviceId(message.getDeviceId());
        edgeData.setPatientTempId(message.getPatientTempId());
        
        // 传感器数据
        if (message.getSensorData() != null) {
            edgeData.setTemperature(message.getSensorData().getTemperature());
            edgeData.setHeartRate(message.getSensorData().getHeartRate());
            edgeData.setBloodOxygen(message.getSensorData().getBloodOxygen());
            // 设置血压数据
            edgeData.setSystolicBP(message.getSensorData().getSystolicBP());
            edgeData.setDiastolicBP(message.getSensorData().getDiastolicBP());
        }
        
        // 语音数据
        if (message.getVoiceData() != null) {
            edgeData.setVoiceText(message.getVoiceData().getText());
            edgeData.setVoiceConfidence(message.getVoiceData().getConfidence());
        }
        
        // 边缘AI分诊结果（结构化）
        if (message.getTriageResult() != null) {
            edgeData.setTriageLevel(message.getTriageResult().getLevel());
            edgeData.setTriageConfidence(message.getTriageResult().getConfidence());
            edgeData.setTriagePriority(message.getTriageResult().getPriority());
            edgeData.setTriageColor(message.getTriageResult().getColor());
            edgeData.setWaitTime(message.getTriageResult().getWaitTime());
        }
        edgeData.setEdgeProcessingTime(message.getProcessingTime());
        
        // 患者信息 - 重要！从边缘端获取患者信息
        if (message.getPatientInfo() != null) {
            log.info("边缘数据包含患者信息: 姓名={}, 年龄={}, 性别={}", 
                message.getPatientInfo().getName(),
                message.getPatientInfo().getAge(),
                message.getPatientInfo().getGender());
            edgeData.setPatientName(message.getPatientInfo().getName());
            edgeData.setPatientAge(message.getPatientInfo().getAge());
            edgeData.setPatientGender(message.getPatientInfo().getGender());
            edgeData.setPatientIdCard(message.getPatientInfo().getIdCard());
            edgeData.setPatientPhone(message.getPatientInfo().getPhone());
        } else {
            log.warn("边缘数据不包含patientInfo字段!");
        }
        
        // 原始数据和质量评分
        try {
            edgeData.setRawSensorData(objectMapper.writeValueAsString(message.getSensorData()));
        } catch (Exception e) {
            log.warn("序列化原始传感器数据失败", e);
        }
        
        edgeData.setDataQualityScore(message.getDataQuality());
        edgeData.setDeviceStatus("ONLINE");
        
        return edgeData;
    }

    /**
     * 从边缘数据创建分诊记录
     */
    private TriageRecord createTriageRecordFromEdge(EdgeDeviceData edgeData) {
        // 创建患者信息 - 使用边缘端发送的真实信息
        Patient patient = new Patient();
        // 优先使用边缘端发送的患者信息，如果没有则使用默认值
        String patientName = edgeData.getPatientName();
        if (patientName == null || patientName.isEmpty()) {
            patientName = "临时患者-" + edgeData.getPatientTempId();
        }
        patient.setPatientName(patientName);
        patient.setIdNumber(edgeData.getPatientIdCard() != null ? edgeData.getPatientIdCard() : edgeData.getPatientTempId());
        patient.setPhoneNumber(edgeData.getPatientPhone() != null ? edgeData.getPatientPhone() : "待补充");
        patient.setGenderFromString(edgeData.getPatientGender() != null ? edgeData.getPatientGender() : "未知");
        patient.setAge(edgeData.getPatientAge() != null ? edgeData.getPatientAge() : 0);
        
        // 创建分诊记录
        TriageRecord triageRecord = new TriageRecord();
        triageRecord.setPatient(patient);
        triageRecord.setChiefComplaint(edgeData.getVoiceText());
        triageRecord.setArrivalTime(LocalDateTime.now());
        
        // 构建生命体征JSON
        try {
            Map<String, Object> vitalSigns = Map.of(
                "temperature", edgeData.getTemperature(),
                "heartRate", edgeData.getHeartRate(),
                "bloodOxygen", edgeData.getBloodOxygen(),
                // 使用边缘设备采集的血压数据，如果为空则使用默认值
                "systolicBP", edgeData.getSystolicBP() != null ? edgeData.getSystolicBP() : 120,
                "diastolicBP", edgeData.getDiastolicBP() != null ? edgeData.getDiastolicBP() : 80,
                "respiratoryRate", 18,
                "consciousness", "清醒"
            );
            triageRecord.setVitalSigns(objectMapper.writeValueAsString(vitalSigns));
        } catch (Exception e) {
            log.warn("构建生命体征数据失败", e);
            triageRecord.setVitalSigns("{}");
        }
        
        // 使用边缘端AI分诊结果（结构化）
        triageRecord.setTriageLevel(edgeData.getTriageLevel());
        triageRecord.setTriagePriority(edgeData.getTriagePriority());
        triageRecord.setTriageColor(edgeData.getTriageColor());
        triageRecord.setWaitTime(edgeData.getWaitTime());
        triageRecord.setAiConfidence(
            edgeData.getTriageConfidence() != null ? 
            edgeData.getTriageConfidence() : 
            0.0
        );
        
        // 设置状态
        triageRecord.setStatus(TriageRecord.TriageStatus.WAITING);
        triageRecord.setAssignedDepartment(getDepartmentByTriageLevel(edgeData.getTriageLevel()));
        
        // 设置边缘设备信息
        triageRecord.setDataSource("edge-device");
        triageRecord.setEdgeDeviceId(edgeData.getDeviceId());
        
        return triageRecord;
    }

    /**
     * 根据分诊等级确定科室
     */
    private String getDepartmentByTriageLevel(int triageLevel) {
        switch (triageLevel) {
            case 1:
            case 2:
                return "急诊科"; // 危急患者直接急诊科
            case 3:
                return "急诊科"; // 急症患者急诊科
            case 4:
            case 5:
                return "门诊"; // 次急症和非急症可以门诊
            default:
                return "急诊科";
        }
    }

    /**
     * 推送数据到前端 - 包含完整的患者信息、生理数据、主诉、设备ID
     */
    private void pushToFrontend(TriageRecord triageRecord, EdgeDeviceData edgeData) {
        try {
            // 构建完整的生命体征数据
            Map<String, Object> vitalSigns = new HashMap<>();
            vitalSigns.put("temperature", edgeData.getTemperature());
            vitalSigns.put("heartRate", edgeData.getHeartRate());
            vitalSigns.put("bloodOxygen", edgeData.getBloodOxygen());
            vitalSigns.put("systolicBP", edgeData.getSystolicBP());
            vitalSigns.put("diastolicBP", edgeData.getDiastolicBP());
            
            // 构建患者信息 - 敏感数据脱敏处理
            Map<String, Object> patientInfo = new HashMap<>();
            patientInfo.put("patientName", edgeData.getPatientName());
            patientInfo.put("patientAge", edgeData.getPatientAge());
            patientInfo.put("patientGender", edgeData.getPatientGender());
            // 身份证脱敏显示（如：110101********1234）
            String idCard = edgeData.getPatientIdCard();
            if (idCard != null && !idCard.isEmpty()) {
                // 如果是加密的，先解密再脱敏
                if (idCard.length() > 50 && idCard.matches("^[A-Za-z0-9+/=]+$")) {
                    try {
                        idCard = encryptionUtil.decrypt(idCard);
                    } catch (Exception e) {
                        log.warn("身份证解密失败，使用原始值");
                    }
                }
                patientInfo.put("idCard", encryptionUtil.maskIdCard(idCard));
            } else {
                patientInfo.put("idCard", "待补充");
            }
            // 手机号脱敏显示
            String phone = edgeData.getPatientPhone();
            if (phone != null && !phone.isEmpty()) {
                if (phone.length() > 50 && phone.matches("^[A-Za-z0-9+/=]+$")) {
                    try {
                        phone = encryptionUtil.decrypt(phone);
                    } catch (Exception e) {
                        log.warn("手机号解密失败，使用原始值");
                    }
                }
                patientInfo.put("phone", encryptionUtil.maskPhone(phone));
            } else {
                patientInfo.put("phone", "待补充");
            }
            
            // 构建前端需要的完整数据结构
            Map<String, Object> frontendData = new HashMap<>();
            
            // 消息类型 - 前端根据这个字段识别消息
            frontendData.put("type", "EDGE_TRIAGE");
            
            // 记录ID
            frontendData.put("id", triageRecord.getId());
            frontendData.put("triageRecordId", triageRecord.getId());
            frontendData.put("edgeDataId", edgeData.getId());
            
            // 边缘设备ID - 护士界面需要显示
            frontendData.put("deviceId", edgeData.getDeviceId());
            frontendData.put("edgeDeviceId", edgeData.getDeviceId());
            
            // 患者信息 - 护士界面需要显示
            frontendData.put("patient", patientInfo);
            frontendData.put("patientName", edgeData.getPatientName());
            frontendData.put("patientAge", edgeData.getPatientAge());
            frontendData.put("patientGender", edgeData.getPatientGender());
            frontendData.put("patientTempId", edgeData.getPatientTempId());
            
            // 重要！患者ID - 护士复核时需要这个ID
            if (triageRecord.getPatient() != null) {
                frontendData.put("patientId", triageRecord.getPatient().getId());
                patientInfo.put("id", triageRecord.getPatient().getId());
            }
            
            // 生命体征 - 护士界面需要显示
            frontendData.put("vitalSigns", vitalSigns);
            frontendData.put("temperature", edgeData.getTemperature());
            frontendData.put("heartRate", edgeData.getHeartRate());
            frontendData.put("bloodOxygen", edgeData.getBloodOxygen());
            frontendData.put("systolicBP", edgeData.getSystolicBP());
            frontendData.put("diastolicBP", edgeData.getDiastolicBP());
            
            // 主诉/症状 - 护士界面需要显示
            frontendData.put("chiefComplaint", edgeData.getVoiceText());
            frontendData.put("voiceText", edgeData.getVoiceText());
            frontendData.put("symptomText", edgeData.getVoiceText());
            
            // 分诊结果
            frontendData.put("triageLevel", triageRecord.getTriageLevel());
            frontendData.put("triagePriority", edgeData.getTriagePriority());
            frontendData.put("triageColor", edgeData.getTriageColor());
            frontendData.put("waitTime", edgeData.getWaitTime());
            frontendData.put("triageScore", edgeData.getTriageConfidence());
            frontendData.put("confidence", edgeData.getTriageConfidence());
            
            // 处理信息
            frontendData.put("edgeProcessingTime", edgeData.getEdgeProcessingTime());
            frontendData.put("dataQuality", edgeData.getDataQualityScore());
            frontendData.put("timestamp", LocalDateTime.now());
            frontendData.put("arrivalTime", LocalDateTime.now());
            frontendData.put("source", "edge-device");
            
            // 完整的triageRecord和edgeData对象（备用）
            frontendData.put("triageRecord", triageRecord);
            frontendData.put("edgeData", edgeData);
            
            log.info("推送边缘分诊数据到前端 - 设备ID: {}, 患者: {}, 分诊等级: {}", 
                edgeData.getDeviceId(), 
                edgeData.getPatientName(),
                triageRecord.getTriageLevel());
            
            // 推送到不同的前端页面
            messagingTemplate.convertAndSend("/topic/new-patient", frontendData);
            messagingTemplate.convertAndSend("/topic/edge-triage", frontendData);
            
            // 如果是高优先级患者，发送紧急通知
            if (triageRecord.getTriageLevel() != null && triageRecord.getTriageLevel() <= 2) {
                frontendData.put("urgent", true);
                messagingTemplate.convertAndSend("/topic/urgent-patient", frontendData);
                log.warn("🚨 紧急患者通知 - 分诊等级: {}", triageRecord.getTriageLevel());
            }
            
        } catch (Exception e) {
            log.error("推送前端数据失败", e);
        }
    }

    /**
     * 推送完整处理结果到前端
     */
    private void pushProcessingResultToFrontend(Map<String, Object> result) {
        try {
            // 推送到护士端 - 需要确认分诊等级
            Map<String, Object> nurseData = Map.of(
                "type", "triage_confirmation",
                "triageRecordId", result.get("triageRecordId"),
                "triageLevel", result.get("triageLevel"),
                "edgeDataId", result.get("edgeDataId"),
                "timestamp", LocalDateTime.now()
            );
            messagingTemplate.convertAndSend("/topic/nurse-confirmation", nurseData);
            
            // 推送到医生端 - 显示初步诊断和语音主诉
            Map<String, Object> doctorData = Map.of(
                "type", "new_diagnosis",
                "diagnosisResultId", result.get("diagnosisResultId"),
                "preliminaryDiagnosis", result.get("preliminaryDiagnosis"),
                "triageLevel", result.get("triageLevel"),
                "timestamp", LocalDateTime.now()
            );
            messagingTemplate.convertAndSend("/topic/doctor-diagnosis", doctorData);
            
            // 推送到管理员端 - 显示医疗资源调度
            Map<String, Object> adminData = Map.of(
                "type", "resource_allocation",
                "resourceAllocationId", result.get("resourceAllocationId"),
                "allocatedDepartment", result.get("allocatedDepartment"),
                "estimatedWaitTime", result.get("estimatedWaitTime"),
                "priorityScore", result.get("priorityScore"),
                "timestamp", LocalDateTime.now()
            );
            messagingTemplate.convertAndSend("/topic/admin-resource", adminData);
            
            // 如果是紧急情况，发送紧急通知
            Integer triageLevel = (Integer) result.get("triageLevel");
            if (triageLevel != null && triageLevel <= 2) {
                Map<String, Object> urgentAlert = Map.of(
                    "type", "urgent_alert",
                    "message", "紧急患者需要立即处理",
                    "triageLevel", triageLevel,
                    "department", result.get("allocatedDepartment"),
                    "timestamp", LocalDateTime.now()
                );
                messagingTemplate.convertAndSend("/topic/urgent-alerts", urgentAlert);
            }
            
        } catch (Exception e) {
            log.error("推送处理结果到前端失败", e);
        }
    }

    /**
     * 发送错误通知
     */
    private void sendErrorNotification(String errorMessage) {
        try {
            Map<String, Object> errorData = Map.of(
                "type", "error",
                "message", errorMessage,
                "timestamp", LocalDateTime.now()
            );
            messagingTemplate.convertAndSend("/topic/system-alerts", errorData);
        } catch (Exception e) {
            log.error("发送错误通知失败", e);
        }
    }

    // MQTT回调方法
    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT连接丢失: {}", cause.getMessage());
        // 可以实现重连逻辑
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // 这个方法在订阅时已经处理，这里留空
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 消息发送完成回调（如果需要发送消息到边缘设备）
    }
    
    /**
     * 处理边缘设备重新分诊后的最终结果
     */
    public void handleFinalTriageMessage(String topic, String messagePayload) {
        try {
            log.info("收到边缘设备最终分诊结果: {}", messagePayload);
            
            FinalTriageMessage finalMessage = objectMapper.readValue(
                messagePayload, FinalTriageMessage.class);
            
            // 构建重新分诊数据（使用结构化分诊结果）
            Map<String, Object> reassessmentData = new HashMap<>();
            if (finalMessage.getFinalTriageResult() != null) {
                reassessmentData.put("triageLevel", finalMessage.getFinalTriageResult().getLevel());
                reassessmentData.put("triagePriority", finalMessage.getFinalTriageResult().getPriority());
                reassessmentData.put("triageColor", finalMessage.getFinalTriageResult().getColor());
                reassessmentData.put("waitTime", finalMessage.getFinalTriageResult().getWaitTime());
                reassessmentData.put("triageScore", finalMessage.getFinalTriageResult().getConfidence());
                reassessmentData.put("aiConfidence", finalMessage.getFinalTriageResult().getConfidence());
            }
            
            // 调用NurseTriageService处理边缘重新分诊结果
            nurseTriageService.handleEdgeReassessmentResult(
                finalMessage.getOriginalDataId(),
                reassessmentData
            );
            
            log.info("边缘设备重新分诊结果已处理 - 边缘数据ID: {}, 新分诊等级: {}", 
                finalMessage.getOriginalDataId(), 
                finalMessage.getFinalTriageResult() != null ? finalMessage.getFinalTriageResult().getLevel() : "未知");
            
        } catch (Exception e) {
            log.error("处理边缘最终分诊结果失败", e);
            sendErrorNotification("边缘重新分诊结果处理失败: " + e.getMessage());
        }
    }

    // 内部消息类
    public static class EdgeTriageMessage {
        private String deviceId;
        private String patientTempId;
        private SensorData sensorData;
        private VoiceData voiceData;
        private TriageResultData triageResult;  // 改为结构化对象
        private PatientInfo patientInfo;        // 患者信息
        private Long processingTime;
        private Double dataQuality;
        private String timestamp;

        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getPatientTempId() { return patientTempId; }
        public void setPatientTempId(String patientTempId) { this.patientTempId = patientTempId; }
        
        public SensorData getSensorData() { return sensorData; }
        public void setSensorData(SensorData sensorData) { this.sensorData = sensorData; }
        
        public VoiceData getVoiceData() { return voiceData; }
        public void setVoiceData(VoiceData voiceData) { this.voiceData = voiceData; }
        
        public TriageResultData getTriageResult() { return triageResult; }
        public void setTriageResult(TriageResultData triageResult) { this.triageResult = triageResult; }
        
        public PatientInfo getPatientInfo() { return patientInfo; }
        public void setPatientInfo(PatientInfo patientInfo) { this.patientInfo = patientInfo; }
        
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
        
        public Double getDataQuality() { return dataQuality; }
        public void setDataQuality(Double dataQuality) { this.dataQuality = dataQuality; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * 患者信息（从边缘端传入）
     */
    public static class PatientInfo {
        private String name;
        private Integer age;
        private String gender;
        private String idCard;
        private String phone;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        
        public String getIdCard() { return idCard; }
        public void setIdCard(String idCard) { this.idCard = idCard; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
    
    /**
     * 边缘AI分诊结果数据（仅包含等级与轻重缓急）
     */
    public static class TriageResultData {
        private Integer level;       // 分诊等级 1-5
        private String priority;     // 轻重缓急
        private String color;        // 颜色标识
        private String waitTime;     // 处理时限
        private Double confidence;   // 置信度
        
        public Integer getLevel() { return level; }
        public void setLevel(Integer level) { this.level = level; }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public String getWaitTime() { return waitTime; }
        public void setWaitTime(String waitTime) { this.waitTime = waitTime; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
    }

    public static class SensorData {
        private Double temperature;
        private Integer heartRate;
        private Integer bloodOxygen;
        private Integer systolicBP;      // 收缩压
        private Integer diastolicBP;     // 舒张压
        private Double ambientTemperature;
        private Double humidity;
        
        // Getters and Setters
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
        
        public Double getAmbientTemperature() { return ambientTemperature; }
        public void setAmbientTemperature(Double ambientTemperature) { this.ambientTemperature = ambientTemperature; }
        
        public Double getHumidity() { return humidity; }
        public void setHumidity(Double humidity) { this.humidity = humidity; }
    }

    public static class VoiceData {
        private String text;
        private Double confidence;
        private String language;
        private Integer duration;
        
        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
    }

    public static class DeviceStatusMessage {
        private String deviceId;
        private String status;
        private String errorMessage;
        private String timestamp;
        
        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    public static class HeartbeatMessage {
        private String deviceId;
        private String timestamp;
        private Map<String, Object> systemInfo;
        
        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getSystemInfo() { return systemInfo; }
        public void setSystemInfo(Map<String, Object> systemInfo) { this.systemInfo = systemInfo; }
    }
    
    /**
     * 最终分诊结果消息（边缘设备重新分诊后）
     */
    public static class FinalTriageMessage {
        private String deviceId;
        private Long correctionId;
        private Long originalDataId;
        private String patientTempId;
        private TriageResultData finalTriageResult;  // 改为结构化对象
        private Long processingTime;
        private String status;
        private String timestamp;
        
        // Getters and Setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public Long getCorrectionId() { return correctionId; }
        public void setCorrectionId(Long correctionId) { this.correctionId = correctionId; }
        
        public Long getOriginalDataId() { return originalDataId; }
        public void setOriginalDataId(Long originalDataId) { this.originalDataId = originalDataId; }
        
        public String getPatientTempId() { return patientTempId; }
        public void setPatientTempId(String patientTempId) { this.patientTempId = patientTempId; }
        
        public TriageResultData getFinalTriageResult() { return finalTriageResult; }
        public void setFinalTriageResult(TriageResultData finalTriageResult) { this.finalTriageResult = finalTriageResult; }
        
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * 从边缘设备数据创建分诊记录
     */
    private TriageRecord createTriageRecordFromEdgeData(EdgeDeviceData edgeData) {
        TriageRecord triageRecord = new TriageRecord();
        
        // 创建患者 - 优先使用边缘端发送的真实患者信息
        Patient patient = new Patient();
        String patientName = edgeData.getPatientName();
        if (patientName == null || patientName.isEmpty()) {
            patientName = "边缘设备患者-" + edgeData.getDeviceId();
        }
        patient.setPatientName(patientName);
        patient.setIdNumber(edgeData.getPatientIdCard() != null ? edgeData.getPatientIdCard() : edgeData.getPatientTempId());
        patient.setPhoneNumber(edgeData.getPatientPhone() != null ? edgeData.getPatientPhone() : "待补充");
        patient.setGenderFromString(edgeData.getPatientGender() != null ? edgeData.getPatientGender() : "未知");
        patient.setAge(edgeData.getPatientAge() != null ? edgeData.getPatientAge() : 0);
        
        // 设置基本信息
        triageRecord.setPatient(patient);
        triageRecord.setArrivalTime(LocalDateTime.now());
        triageRecord.setChiefComplaint(edgeData.getVoiceText() != null ? edgeData.getVoiceText() : "待补充");
        
        // 设置状态为 WAITING - 这是关键！护士端API查询的是这个状态
        triageRecord.setStatus(TriageRecord.TriageStatus.WAITING);
        
        // 设置科室
        triageRecord.setAssignedDepartment(getDepartmentByTriageLevel(edgeData.getTriageLevel()));
        
        // 设置数据来源
        triageRecord.setDataSource("edge-device");
        triageRecord.setEdgeDeviceId(edgeData.getDeviceId());
        
        // 设置生命体征
        Map<String, Object> vitalSigns = new HashMap<>();
        vitalSigns.put("temperature", edgeData.getTemperature());
        // TODO: EdgeDeviceData没有getSystolicBloodPressure等方法
        // vitalSigns.put("systolicBP", edgeData.getSystolicBloodPressure());
        // vitalSigns.put("diastolicBP", edgeData.getDiastolicBloodPressure());
        vitalSigns.put("systolicBP", edgeData.getSystolicBP() != null ? edgeData.getSystolicBP() : 120);
        vitalSigns.put("diastolicBP", edgeData.getDiastolicBP() != null ? edgeData.getDiastolicBP() : 80);
        vitalSigns.put("heartRate", edgeData.getHeartRate());
        vitalSigns.put("bloodOxygen", edgeData.getBloodOxygen());
        // vitalSigns.put("respiratoryRate", edgeData.getRespiratoryRate());
        vitalSigns.put("respiratoryRate", 18);
        
        // 转换为JSON字符串
        try {
            ObjectMapper mapper = new ObjectMapper();
            triageRecord.setVitalSigns(mapper.writeValueAsString(vitalSigns));
        } catch (Exception e) {
            log.error("生命体征JSON转换失败", e);
            triageRecord.setVitalSigns("{}");
        }
        
        // 设置边缘端分诊结果
        triageRecord.setTriageLevel(edgeData.getTriageLevel());
        triageRecord.setTriagePriority(getPriorityFromLevel(edgeData.getTriageLevel()));
        triageRecord.setTriageColor(getColorFromLevel(edgeData.getTriageLevel()));
        triageRecord.setTriageScore(edgeData.getTriageScore());
        // TODO: EdgeDeviceData没有getConfidence方法，使用getTriageConfidence
        // triageRecord.setAiConfidence(edgeData.getConfidence());
        triageRecord.setAiConfidence(
            edgeData.getTriageConfidence() != null ? 
            edgeData.getTriageConfidence() : 
            0.0
        );
        // TODO: TriageRecord没有setTriageSource方法
        // triageRecord.setTriageSource("边缘设备AI");
        
        return triageRecord;
    }
    
    /**
     * 根据分诊等级获取优先级描述
     */
    private String getPriorityFromLevel(Integer level) {
        if (level == null) return "未知";
        switch (level) {
            case 1: return "危急";
            case 2: return "危重";
            case 3: return "急症";
            case 4: return "非急症";
            case 5: return "观察";
            default: return "未知";
        }
    }
    
    /**
     * 根据分诊等级获取颜色标识
     */
    private String getColorFromLevel(Integer level) {
        if (level == null) return "灰色";
        switch (level) {
            case 1: return "红色";
            case 2: return "橙色";
            case 3: return "黄色";
            case 4: return "绿色";
            case 5: return "蓝色";
            default: return "灰色";
        }
    }
}