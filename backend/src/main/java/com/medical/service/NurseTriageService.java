package com.medical.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.dto.NurseCorrectionRequest;
import com.medical.entity.*;
import com.medical.repository.EdgeDeviceDataRepository;
import com.medical.repository.NurseCorrectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 护士分诊服务 - 处理护士复核流程
 * 
 * 流程1：需要修正 -> 发回边缘端重新分诊 -> 返回新结果给护士
 * 流程2：确认无误 -> 提交到云端医疗大模型进行完整诊断
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NurseTriageService {
    
    private final EdgeDeviceDataRepository edgeDataRepository;
    private final NurseCorrectionRepository correctionRepository;
    private final MultimodalDiagnosisService multimodalDiagnosisService;
    private final TriageService triageService;
    private final MedicalResourceSchedulingService resourceSchedulingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;
    
    /**
     * 流程1：护士修正数据后，发回边缘端重新分诊
     */
    @Transactional
    public Map<String, Object> sendCorrectionToEdge(NurseCorrectionRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("护士要求边缘端重新分诊 - 边缘数据ID: {}, 护士: {}", 
                request.getEdgeDataId(), request.getNurseName());
            
            // 1. 查找原始边缘数据
            EdgeDeviceData edgeData = edgeDataRepository.findById(request.getEdgeDataId())
                .orElseThrow(() -> new RuntimeException("原始边缘数据不存在，ID: " + request.getEdgeDataId()));
            
            // 2. 保存护士修正记录
            NurseCorrectionRecord correction = new NurseCorrectionRecord();
            correction.setEdgeDataId(request.getEdgeDataId());
            correction.setNurseId(request.getNurseId());
            correction.setNurseName(request.getNurseName());
            correction.setCorrectedSensorData(objectMapper.writeValueAsString(
                request.getCorrectedSensorData()));
            correction.setCorrectedChiefComplaint(request.getCorrectedChiefComplaint());
            correction.setNurseNotes(request.getNurseNotes());
            correction.setCorrectionTime(LocalDateTime.now());
            correction.setStatus("SENT_TO_EDGE");
            
            NurseCorrectionRecord savedCorrection = correctionRepository.save(correction);
            log.info("护士修正记录已保存 - ID: {}", savedCorrection.getId());
            
            // 3. 构建发送给Jetson的MQTT消息
            Map<String, Object> mqttPayload = buildMqttCorrectionMessage(
                savedCorrection, edgeData, request);
            
            // 4. 发送MQTT消息到边缘设备进行重新分诊
            sendCorrectionToEdgeDevice(edgeData.getDeviceId(), mqttPayload);
            
            // 5. 更新边缘数据状态
            edgeData.setProcessingStatus("WAITING_EDGE_REASSESSMENT");
            edgeData.setUpdatedAt(LocalDateTime.now());
            edgeDataRepository.save(edgeData);
            
            result.put("success", true);
            result.put("message", "修正数据已发送到边缘端，等待重新分诊结果");
            result.put("correctionId", savedCorrection.getId());
            result.put("status", "WAITING_EDGE_REASSESSMENT");
            
            log.info("护士修正已发送到边缘端 - 修正ID: {}, 设备ID: {}", 
                savedCorrection.getId(), edgeData.getDeviceId());
            
            return result;
            
        } catch (Exception e) {
            log.error("发送修正到边缘端失败", e);
            result.put("success", false);
            result.put("message", "发送失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 流程2：护士确认数据无误，提交到云端医疗大模型进行完整诊断
     */
    @Transactional
    public Map<String, Object> confirmAndSubmitToCloud(NurseCorrectionRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("护士确认无误，提交到云端大模型 - 边缘数据ID: {}, 护士: {}", 
                request.getEdgeDataId(), request.getNurseName());
            
            // 1. 查找边缘数据
            EdgeDeviceData edgeData = edgeDataRepository.findById(request.getEdgeDataId())
                .orElseThrow(() -> new RuntimeException("边缘数据不存在，ID: " + request.getEdgeDataId()));
            
            // 2. 保存护士确认记录
            NurseCorrectionRecord confirmation = new NurseCorrectionRecord();
            confirmation.setEdgeDataId(request.getEdgeDataId());
            confirmation.setNurseId(request.getNurseId());
            confirmation.setNurseName(request.getNurseName());
            confirmation.setCorrectedSensorData(objectMapper.writeValueAsString(
                request.getCorrectedSensorData()));
            confirmation.setCorrectedChiefComplaint(request.getCorrectedChiefComplaint());
            confirmation.setNurseNotes(request.getNurseNotes());
            confirmation.setCorrectionTime(LocalDateTime.now());
            confirmation.setStatus("CONFIRMED_BY_NURSE");
            
            NurseCorrectionRecord savedConfirmation = correctionRepository.save(confirmation);
            log.info("护士确认记录已保存 - ID: {}", savedConfirmation.getId());
            
            // 3. 更新边缘数据状态
            edgeData.setProcessingStatus("CONFIRMED_SUBMITTING_TO_CLOUD");
            edgeData.setUpdatedAt(LocalDateTime.now());
            edgeDataRepository.save(edgeData);
            
            // 4. 创建分诊记录（使用护士确认后的数据）
            TriageRecord triageRecord = createTriageRecordFromNurseConfirmation(
                edgeData, request);
            
            // 5. 执行云端多模态AI诊断（百川大模型 + RAG）
            com.medical.model.DiagnosisResult modelDiagnosisResult = multimodalDiagnosisService
                .performMultimodalDiagnosis(triageRecord);
            
            // 转换为entity类型
            com.medical.entity.DiagnosisResult diagnosisResult = convertToEntityDiagnosisResult(modelDiagnosisResult, triageRecord);
            
            // ★★★ 将百川AI诊断结果更新回分诊记录 ★★★
            triageRecord.setAiDiagnosis(diagnosisResult.getPreliminaryDiagnosis());
            triageRecord.setAiConfidence(diagnosisResult.getDiagnosisConfidence() != null ? diagnosisResult.getDiagnosisConfidence().doubleValue() : 0.85);
            triageService.updateTriageRecord(triageRecord); // 保存更新后的AI诊断结果
            
            // 6. 执行医疗资源调度
            ResourceAllocation resourceAllocation = resourceSchedulingService
                .scheduleResources(triageRecord, diagnosisResult);
            
            // 7. 更新确认记录状态
            savedConfirmation.setStatus("CLOUD_DIAGNOSIS_COMPLETED");
            savedConfirmation.setFinalTriageLevel(triageRecord.getTriageLevel());
            savedConfirmation.setFinalReceivedTime(LocalDateTime.now());
            correctionRepository.save(savedConfirmation);
            
            // 8. 更新边缘数据最终状态
            edgeData.setProcessingStatus("COMPLETED");
            edgeData.setFinalDiagnosis(objectMapper.writeValueAsString(diagnosisResult));
            edgeData.setProcessed(true);
            edgeDataRepository.save(edgeData);
            
            // 9. 通过WebSocket推送完整结果到医生端
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "NEW_DIAGNOSIS");
            wsMessage.put("triageRecordId", triageRecord.getId());
            wsMessage.put("diagnosisResultId", diagnosisResult.getId());
            
            // 患者信息
            wsMessage.put("patientId", triageRecord.getPatient().getId());
            wsMessage.put("patientName", triageRecord.getPatient().getPatientName());
            wsMessage.put("patientAge", triageRecord.getPatient().getAge());
            wsMessage.put("patientGender", triageRecord.getPatient().getGender());
            
            // 分诊信息
            wsMessage.put("triageLevel", triageRecord.getTriageLevel());
            wsMessage.put("department", resourceAllocation.getAllocatedDepartment());
            wsMessage.put("assignedDepartment", resourceAllocation.getAllocatedDepartment());
            wsMessage.put("urgency", diagnosisResult.getUrgencyLevel());
            
            // 主诉和诊断
            wsMessage.put("chiefComplaint", triageRecord.getChiefComplaint());
            wsMessage.put("aiDiagnosis", triageRecord.getAiDiagnosis());
            wsMessage.put("preliminaryDiagnosis", diagnosisResult.getPreliminaryDiagnosis());
            
            // 生命体征
            wsMessage.put("vitalSigns", triageRecord.getVitalSigns());
            
            // 到院时间
            wsMessage.put("arrivalTime", triageRecord.getArrivalTime());
            wsMessage.put("timestamp", LocalDateTime.now());
            
            messagingTemplate.convertAndSend("/topic/new-patients", wsMessage);
            messagingTemplate.convertAndSend("/topic/doctor-diagnosis", wsMessage);
            
            // 10. 通知护士完成
            Map<String, Object> nurseMessage = new HashMap<>();
            nurseMessage.put("type", "CLOUD_DIAGNOSIS_COMPLETED");
            nurseMessage.put("edgeDataId", edgeData.getId());
            nurseMessage.put("triageRecordId", triageRecord.getId());
            nurseMessage.put("diagnosis", diagnosisResult.getPreliminaryDiagnosis());
            nurseMessage.put("department", resourceAllocation.getAllocatedDepartment());
            messagingTemplate.convertAndSend("/topic/nurse-confirmation-result", nurseMessage);
            
            result.put("success", true);
            result.put("message", "云端诊断完成");
            result.put("confirmationId", savedConfirmation.getId());
            result.put("triageRecordId", triageRecord.getId());
            result.put("diagnosisResultId", diagnosisResult.getId());
            result.put("resourceAllocationId", resourceAllocation.getId());
            result.put("diagnosis", diagnosisResult.getPreliminaryDiagnosis());
            result.put("department", resourceAllocation.getAllocatedDepartment());
            result.put("waitTime", resourceAllocation.getEstimatedWaitTime());
            
            log.info("云端诊断流程完成 - 患者: {}, 科室: {}, 诊断: {}", 
                triageRecord.getPatient().getPatientName(),
                resourceAllocation.getAllocatedDepartment(),
                diagnosisResult.getPreliminaryDiagnosis());
            
            return result;
            
        } catch (Exception e) {
            log.error("云端诊断流程失败", e);
            result.put("success", false);
            result.put("message", "云端诊断失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 处理边缘端返回的重新分诊结果
     */
    @Transactional
    public void handleEdgeReassessmentResult(Long edgeDataId, Map<String, Object> reassessmentData) {
        try {
            log.info("收到边缘端重新分诊结果 - 边缘数据ID: {}", edgeDataId);
            
            EdgeDeviceData edgeData = edgeDataRepository.findById(edgeDataId)
                .orElseThrow(() -> new RuntimeException("边缘数据不存在"));
            
            // 更新边缘数据
            edgeData.setTriageLevel((Integer) reassessmentData.get("triageLevel"));
            edgeData.setTriageScore((Double) reassessmentData.get("triageScore"));
            edgeData.setAiDiagnosis((String) reassessmentData.get("aiDiagnosis"));
            edgeData.setAiConfidence((Double) reassessmentData.get("aiConfidence"));
            edgeData.setProcessingStatus("EDGE_REASSESSED");
            edgeData.setUpdatedAt(LocalDateTime.now());
            edgeDataRepository.save(edgeData);
            
            // 更新最近的修正记录
            List<NurseCorrectionRecord> corrections = correctionRepository
                .findByEdgeDataIdOrderByCreatedAtDesc(edgeDataId);
            if (!corrections.isEmpty()) {
                NurseCorrectionRecord latestCorrection = corrections.get(0);
                latestCorrection.setStatus("EDGE_REASSESSED");
                latestCorrection.setFinalTriageLevel((Integer) reassessmentData.get("triageLevel"));
                latestCorrection.setFinalReceivedTime(LocalDateTime.now());
                correctionRepository.save(latestCorrection);
            }
            
            // 通过WebSocket推送给护士端
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "EDGE_REASSESSMENT_RESULT");
            wsMessage.put("edgeDataId", edgeDataId);
            wsMessage.put("newTriageLevel", reassessmentData.get("triageLevel"));
            wsMessage.put("newDiagnosis", reassessmentData.get("aiDiagnosis"));
            wsMessage.put("confidence", reassessmentData.get("aiConfidence"));
            wsMessage.put("message", "边缘端重新分诊完成，请护士再次确认");
            messagingTemplate.convertAndSend("/topic/nurse-reassessment-result", wsMessage);
            
            log.info("边缘重新分诊结果已推送给护士 - 新等级: {}", reassessmentData.get("triageLevel"));
            
        } catch (Exception e) {
            log.error("处理边缘重新分诊结果失败", e);
        }
    }
    
    /**
     * 从护士确认的数据创建分诊记录
     */
    private TriageRecord createTriageRecordFromNurseConfirmation(
            EdgeDeviceData edgeData, 
            NurseCorrectionRequest request) {
        
        // 创建患者
        Patient patient = new Patient();
        patient.setPatientName(edgeData.getPatientName());
        patient.setAge(edgeData.getPatientAge());
        patient.setGenderFromString(edgeData.getPatientGender());
        patient.setIdCard(edgeData.getPatientIdCard());
        patient.setPhoneNumber(edgeData.getPatientPhone());
        
        // 创建分诊记录（使用护士确认的数据）
        TriageRecord triageRecord = new TriageRecord();
        triageRecord.setPatient(patient);
        triageRecord.setChiefComplaint(request.getCorrectedChiefComplaint()); // 护士确认/修正的主诉
        
        try {
            // 使用护士确认/修正的生命体征
            triageRecord.setVitalSigns(objectMapper.writeValueAsString(
                request.getCorrectedSensorData()));
        } catch (Exception e) {
            triageRecord.setVitalSigns(edgeData.getVitalSigns());
        }
        
        triageRecord.setTriageLevel(edgeData.getTriageLevel());
        triageRecord.setTriageScore(edgeData.getTriageScore());
        triageRecord.setAiDiagnosis(edgeData.getAiDiagnosis());
        triageRecord.setAiConfidence(edgeData.getAiConfidence());
        triageRecord.setArrivalTime(edgeData.getReceivedTime());
        triageRecord.setNurseNotes("护士确认：" + request.getNurseNotes());
        
        // 保存分诊记录
        return triageService.saveTriageRecordFromEdge(triageRecord);
    }
    
    /**
     * 构建发送给边缘设备的MQTT修正消息
     */
    private Map<String, Object> buildMqttCorrectionMessage(
            NurseCorrectionRecord correction,
            EdgeDeviceData edgeData,
            NurseCorrectionRequest request) {
        
        Map<String, Object> message = new HashMap<>();
        message.put("correctionId", correction.getId());
        message.put("originalDataId", edgeData.getId());
        message.put("patientTempId", edgeData.getPatientTempId());
        message.put("nurseName", request.getNurseName());
        message.put("nurseId", request.getNurseId());
        
        // 修正后的数据
        Map<String, Object> correctedData = new HashMap<>();
        correctedData.put("sensorData", request.getCorrectedSensorData());
        correctedData.put("voiceComplaint", request.getCorrectedChiefComplaint());
        correctedData.put("nurseNotes", request.getNurseNotes());
        message.put("correctedData", correctedData);
        
        message.put("command", "REASSESS");
        message.put("timestamp", LocalDateTime.now().toString());
        
        return message;
    }
    
    /**
     * 通过MQTT发送修正数据到边缘设备
     */
    private void sendCorrectionToEdgeDevice(String deviceId, Map<String, Object> payload) {
        try {
            String topic = "medical/triage/correction/" + deviceId;
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            MqttMessage message = new MqttMessage(payloadJson.getBytes("UTF-8"));
            message.setQos(1);
            message.setRetained(false);
            
            if (!mqttClient.isConnected()) {
                log.warn("MQTT客户端未连接，尝试重连...");
                throw new RuntimeException("MQTT客户端未连接");
            }
            
            mqttClient.publish(topic, message);
            
            log.info("护士修正数据已发送到边缘端 - 主题: {}, 设备ID: {}", topic, deviceId);
            log.debug("MQTT消息内容: {}", payloadJson);
            
        } catch (MqttException e) {
            log.error("发送MQTT消息失败 - 设备ID: {}", deviceId, e);
            throw new RuntimeException("发送MQTT消息失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("构建MQTT消息失败", e);
            throw new RuntimeException("构建MQTT消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取边缘数据的所有修正/确认记录
     */
    public List<NurseCorrectionRecord> getCorrectionsByEdgeDataId(Long edgeDataId) {
        return correctionRepository.findByEdgeDataId(edgeDataId);
    }
    
    /**
     * 获取护士的所有操作记录
     */
    public List<NurseCorrectionRecord> getCorrectionsByNurseId(Long nurseId) {
        return correctionRepository.findByNurseId(nurseId);
    }
    
    /**
     * 获取待复核患者列表
     * 返回所有状态为PENDING_NURSE_REVIEW的患者
     */
    public List<Map<String, Object>> getPendingPatientList() {
        try {
            // 查询所有等待护士复核的边缘数据
            List<EdgeDeviceData> pendingList = edgeDataRepository
                .findByProcessingStatus("PENDING_NURSE_REVIEW");
            
            // 转换为前端需要的格式
            return pendingList.stream().map(data -> {
                Map<String, Object> patient = new HashMap<>();
                patient.put("id", data.getId());
                patient.put("patientName", data.getPatientName());
                patient.put("gender", data.getPatientGender());
                patient.put("age", data.getPatientAge());
                patient.put("triageLevel", data.getTriageLevel());
                patient.put("triagePriority", data.getTriagePriority());
                patient.put("triageColor", data.getTriageColor());
                patient.put("waitTime", data.getWaitTime());
                patient.put("voiceComplaint", data.getVoiceComplaint());
                patient.put("arrivalTime", data.getCreatedAt().toString());
                patient.put("status", data.getProcessingStatus());
                return patient;
            }).toList();
            
        } catch (Exception e) {
            log.error("获取待复核患者列表失败", e);
            throw new RuntimeException("获取待复核患者列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取护士工作台统计数据
     */
    public Map<String, Object> getNurseStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 新到患者 - 今日新增的患者数
            long newArrivals = edgeDataRepository.countByCreatedAtAfter(
                LocalDateTime.now().minusDays(1));
            
            // 待复核数量
            long pendingReview = edgeDataRepository.countByProcessingStatus(
                "PENDING_NURSE_REVIEW");
            
            // 今日确认数量 - 今日护士确认提交云端的数量
            long confirmedToday = correctionRepository.countByStatusAndCorrectionTimeAfter(
                "CONFIRMED_TO_CLOUD", LocalDateTime.now().minusDays(1));
            
            // 紧急病例 - I级和II级分诊的患者
            long urgentCases = edgeDataRepository.countByProcessingStatusAndTriageLevelLessThanEqual(
                "PENDING_NURSE_REVIEW", 2);
            
            stats.put("newArrivals", newArrivals);
            stats.put("pendingReview", pendingReview);
            stats.put("confirmedToday", confirmedToday);
            stats.put("urgentCases", urgentCases);
            
            return stats;
            
        } catch (Exception e) {
            log.error("获取护士统计数据失败", e);
            // 返回默认值而不是抛出异常
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("newArrivals", 0L);
            defaultStats.put("pendingReview", 0L);
            defaultStats.put("confirmedToday", 0L);
            defaultStats.put("urgentCases", 0L);
            return defaultStats;
        }
    }
    
    /**
     * 将model.DiagnosisResult转换为entity.DiagnosisResult
     */
    private com.medical.entity.DiagnosisResult convertToEntityDiagnosisResult(
            com.medical.model.DiagnosisResult modelResult, 
            com.medical.entity.TriageRecord triageRecord) {
        
        com.medical.entity.DiagnosisResult entityResult = new com.medical.entity.DiagnosisResult();
        
        // 关联分诊记录
        entityResult.setTriageRecord(triageRecord);
        
        // 基本信息
        entityResult.setPreliminaryDiagnosis(modelResult.getPrimaryDiagnosis());
        entityResult.setDiagnosisConfidence(java.math.BigDecimal.valueOf(modelResult.getConfidence()));
        
        // 分析结果
        entityResult.setSymptomsAnalysis(modelResult.getSymptomAnalysis());
        entityResult.setVitalSignsAnalysis(modelResult.getVitalSignsAnalysis());
        entityResult.setMedicalHistoryAnalysis(modelResult.getMedicalHistoryAnalysis());
        
        // 推荐内容
        entityResult.setRecommendedExaminations(modelResult.getRecommendedExams() != null ? 
            String.join(", ", modelResult.getRecommendedExams()) : "");
        entityResult.setTreatmentSuggestions(modelResult.getTreatmentRecommendation());
        
        // 风险评估
        entityResult.setRiskAssessment("紧急程度: " + modelResult.getUrgencyLevel());
        
        // 解析紧急程度
        if (modelResult.getUrgencyLevel() != null) {
            switch (modelResult.getUrgencyLevel()) {
                case "紧急":
                    entityResult.setUrgencyLevel(1);
                    break;
                case "较紧急":
                    entityResult.setUrgencyLevel(2);
                    break;
                case "一般":
                    entityResult.setUrgencyLevel(3);
                    break;
                default:
                    entityResult.setUrgencyLevel(4);
                    break;
            }
        } else {
            entityResult.setUrgencyLevel(3);
        }
        
        // AI模型信息
        entityResult.setAiModelUsed(modelResult.getModelVersion());
        entityResult.setProcessingTimeMs(modelResult.getProcessingTimeMs());
        
        // 设置状态
        entityResult.setStatus(com.medical.entity.DiagnosisResult.DiagnosisStatus.PENDING);
        
        return entityResult;
    }
}
