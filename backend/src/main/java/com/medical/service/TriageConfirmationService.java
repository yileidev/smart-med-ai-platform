package com.medical.service;

import com.medical.controller.TriageConfirmationController.*;
import com.medical.entity.Patient;
import com.medical.entity.TriageRecord;
import com.medical.entity.DiagnosisResult;
import com.medical.entity.ResourceAllocation;
import com.medical.repository.PatientRepository;
import com.medical.repository.TriageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 分诊确认服务
 * 处理护士确认边缘AI分诊结果的业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TriageConfirmationService {

    private final TriageRecordRepository triageRecordRepository;
    private final PatientRepository patientRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MultimodalDiagnosisService multimodalDiagnosisService;
    private final MedicalResourceSchedulingService resourceSchedulingService;

    /**
     * 获取待确认的分诊记录
     */
    public Page<TriageRecord> getPendingConfirmationRecords(Pageable pageable) {
        // 查找状态为待确认且来自边缘设备的分诊记录
        return triageRecordRepository.findByStatusAndDataSourceOrderByArrivalTimeAsc(
            TriageRecord.TriageStatus.PENDING_CONFIRMATION, 
            "EDGE_DEVICE", 
            pageable
        );
    }

    /**
     * 确认分诊结果 - 调用云端大模型诊断和资源调度
     */
    @Transactional
    public Map<String, Object> confirmTriage(Long recordId, TriageConfirmationRequest request) {
        try {
            log.info("护士确认分诊 - 记录ID: {}, 开始云端大模型诊断", recordId);
            
            TriageRecord record = triageRecordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("分诊记录不存在"));

            // 更新分诊等级（如果护士调整了）
            if (request.getConfirmedTriageLevel() != null) {
                record.setTriageLevel(request.getConfirmedTriageLevel());
            }

            // 更新主诉（如果护士修正了）
            if (request.getUpdatedChiefComplaint() != null) {
                record.setChiefComplaint(request.getUpdatedChiefComplaint());
            }

            // 添加护士备注
            if (request.getNurseComments() != null && !request.getNurseComments().trim().isEmpty()) {
                record.setNurseComments(request.getNurseComments());
            }

            // 更新生命体征（如果有补充）
            if (request.getUpdatedVitalSigns() != null) {
                record.setVitalSigns(request.getUpdatedVitalSigns());
            }

            // 更新患者信息（如果有补充）
            if (request.getPatientInfo() != null) {
                updatePatientInfo(record, request.getPatientInfo());
            }

            // 更新状态为已确认
            record.setStatus(TriageRecord.TriageStatus.CONFIRMED);
            record.setConfirmedTime(LocalDateTime.now());

            TriageRecord savedRecord = triageRecordRepository.save(record);

            // 调用云端医疗大模型进行诊断
            log.info("调用云端医疗大模型诊断 - 患者: {}", 
                savedRecord.getPatient() != null ? savedRecord.getPatient().getPatientName() : "未知");
                
            com.medical.model.DiagnosisResult modelDiagnosisResult = multimodalDiagnosisService.performMultimodalDiagnosis(savedRecord);
            
            // 执行医疗资源调度
            ResourceAllocation resourceAllocation = resourceSchedulingService.scheduleResources(savedRecord, null); // model版本不兼容，传null
            
            // 发送WebSocket通知到医生端（暂时不发送，因为model类型不匹配）
            // sendToDoctor(savedRecord, modelDiagnosisResult, resourceAllocation);
            
            // 发送WebSocket通知到护士端
            sendTriageConfirmationNotification(savedRecord, "CONFIRMED");

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("triageRecord", savedRecord);
            result.put("diagnosisResult", modelDiagnosisResult);
            result.put("resourceAllocation", resourceAllocation);
            result.put("message", "分诊确认成功，已生成AI诊断和资源调度方案");
            result.put("preliminaryDiagnosis", modelDiagnosisResult.getPrimaryDiagnosis());
            result.put("allocatedDepartment", resourceAllocation.getAllocatedDepartment());
            result.put("estimatedWaitTime", resourceAllocation.getEstimatedWaitTime());

            log.info("护士确认分诊完成 - 患者: {}, 诊断: {}, 科室: {}", 
                savedRecord.getPatient() != null ? savedRecord.getPatient().getPatientName() : "未知",
                modelDiagnosisResult.getPrimaryDiagnosis(),
                resourceAllocation.getAllocatedDepartment());

            return result;
            
        } catch (Exception e) {
            log.error("护士确认分诊失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "确认失败: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 拒绝分诊结果，要求重新分诊
     */
    @Transactional
    public void rejectTriage(Long recordId, TriageRejectionRequest request) {
        TriageRecord record = triageRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("分诊记录不存在"));

        // 更新主诉（如果护士修正了）
        if (request.getUpdatedChiefComplaint() != null && !request.getUpdatedChiefComplaint().trim().isEmpty()) {
            record.setChiefComplaint(request.getUpdatedChiefComplaint());
        }

        // 添加拒绝原因
        record.setNurseComments("拒绝原因: " + request.getReason());

        // 更新状态为需要重新分诊
        record.setStatus(TriageRecord.TriageStatus.PENDING_RETRIAGE);

        triageRecordRepository.save(record);

        // 发送WebSocket通知
        sendTriageConfirmationNotification(record, "REJECTED");

        log.info("分诊记录已拒绝: ID={}, 拒绝原因={}", recordId, request.getReason());
    }

    /**
     * 获取分诊记录详情
     */
    public TriageRecord getTriageRecordDetail(Long recordId) {
        return triageRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("分诊记录不存在"));
    }

    /**
     * 获取分诊确认统计信息
     */
    public Map<String, Object> getTriageConfirmationStats() {
        Map<String, Object> stats = new HashMap<>();

        // 待确认数量
        long pendingCount = triageRecordRepository.countByStatusAndDataSource(
            TriageRecord.TriageStatus.PENDING_CONFIRMATION, "EDGE_DEVICE"
        );

        // 已确认数量（今日）
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long confirmedTodayCount = triageRecordRepository.countByStatusAndConfirmedTimeAfter(
            TriageRecord.TriageStatus.CONFIRMED, startOfDay
        );

        // 拒绝/重新分诊数量(今日)
        long rejectedTodayCount = triageRecordRepository.countByStatusAndUpdatedAtAfter(
            TriageRecord.TriageStatus.PENDING_RETRIAGE, startOfDay
        );

        // 各等级分布
        Map<Integer, Long> levelDistribution = new HashMap<>();
        for (int level = 1; level <= 5; level++) {
            long count = triageRecordRepository.countByTriageLevelAndStatusAndConfirmedTimeAfter(
                level, TriageRecord.TriageStatus.CONFIRMED, startOfDay
            );
            levelDistribution.put(level, count);
        }

        stats.put("pendingConfirmation", pendingCount);
        stats.put("confirmedToday", confirmedTodayCount);
        stats.put("rejectedToday", rejectedTodayCount);
        stats.put("levelDistribution", levelDistribution);
        stats.put("lastUpdated", LocalDateTime.now());

        return stats;
    }

    /**
     * 批量确认分诊
     */
    @Transactional
    public List<TriageRecord> batchConfirmTriage(BatchConfirmRequest request) {
        List<TriageRecord> confirmedRecords = new ArrayList<>();

        for (Long recordId : request.getRecordIds()) {
            try {
                TriageRecord record = triageRecordRepository.findById(recordId)
                        .orElseThrow(() -> new IllegalArgumentException("分诊记录不存在: " + recordId));

                // 批量确认时保持原分诊等级
                record.setStatus(TriageRecord.TriageStatus.CONFIRMED);
                record.setConfirmedTime(LocalDateTime.now());
                
                if (request.getBatchComments() != null && !request.getBatchComments().trim().isEmpty()) {
                    record.setNurseComments("批量确认: " + request.getBatchComments());
                }

                TriageRecord savedRecord = triageRecordRepository.save(record);
                confirmedRecords.add(savedRecord);

                // 发送WebSocket通知
                sendTriageConfirmationNotification(savedRecord, "BATCH_CONFIRMED");

            } catch (Exception e) {
                log.error("批量确认失败，记录ID: {}", recordId, e);
            }
        }

        log.info("批量确认完成，成功确认 {} 条记录", confirmedRecords.size());
        return confirmedRecords;
    }

    /**
     * 更新患者信息
     */
    private void updatePatientInfo(TriageRecord record, PatientInfoUpdate patientInfo) {
        Patient patient = record.getPatient();
        if (patient == null) {
            // 如果没有关联患者，创建新患者
            patient = new Patient();
            record.setPatient(patient);
        }

        // 更新患者信息
        if (patientInfo.getName() != null && !patientInfo.getName().trim().isEmpty()) {
            patient.setPatientName(patientInfo.getName());
        }
        if (patientInfo.getAge() != null) {
            patient.setAge(patientInfo.getAge());
        }
        if (patientInfo.getGender() != null && !patientInfo.getGender().trim().isEmpty()) {
            patient.setGenderFromString(patientInfo.getGender());
        }
        if (patientInfo.getPhoneNumber() != null && !patientInfo.getPhoneNumber().trim().isEmpty()) {
            patient.setPhoneNumber(patientInfo.getPhoneNumber());
        }
        if (patientInfo.getIdCard() != null && !patientInfo.getIdCard().trim().isEmpty()) {
            patient.setIdCard(patientInfo.getIdCard());
        }

        patientRepository.save(patient);
    }

    /**
     * 发送分诊确认WebSocket通知 - 包含完整患者数据
     */
    private void sendTriageConfirmationNotification(TriageRecord record, String action) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_DIAGNOSIS");  // 统一使用NEW_DIAGNOSIS类型
            notification.put("action", action);
            notification.put("recordId", record.getId());
            notification.put("triageRecordId", record.getId());
            notification.put("triageLevel", record.getTriageLevel());
            notification.put("timestamp", LocalDateTime.now());
            
            // 患者信息
            if (record.getPatient() != null) {
                notification.put("patientId", record.getPatient().getId());
                notification.put("patientName", record.getPatient().getPatientName());
                notification.put("patientAge", record.getPatient().getAge());
                notification.put("patientGender", record.getPatient().getGender());
            } else {
                notification.put("patientName", "临时患者");
            }
            
            // 主诉和诊断
            notification.put("chiefComplaint", record.getChiefComplaint());
            notification.put("aiDiagnosis", record.getAiDiagnosis());
            notification.put("assignedDepartment", record.getAssignedDepartment());
            notification.put("arrivalTime", record.getArrivalTime());
            
            // 生命体征
            notification.put("vitalSigns", record.getVitalSigns());
            
            // 发送给护士确认主题
            messagingTemplate.convertAndSend("/topic/nurse-confirmation", notification);
            
            // 发送给医生诊断主题
            messagingTemplate.convertAndSend("/topic/doctor-diagnosis", notification);
            
            // 发送给医生新患者主题 - 确保医生端能收到数据
            messagingTemplate.convertAndSend("/topic/new-patients", notification);

            log.info("发送分诊确认WebSocket通知 - 患者: {}, 分诊等级: {}", 
                notification.get("patientName"), record.getTriageLevel());

        } catch (Exception e) {
            log.error("发送WebSocket通知失败", e);
        }
    }
    
    /**
     * 发送数据到医生端
     */
    private void sendToDoctor(TriageRecord triageRecord, DiagnosisResult diagnosisResult, ResourceAllocation resourceAllocation) {
        try {
            Map<String, Object> doctorMessage = new HashMap<>();
            doctorMessage.put("type", "NEW_PATIENT_DIAGNOSIS");
            doctorMessage.put("triageRecordId", triageRecord.getId());
            doctorMessage.put("diagnosisResultId", diagnosisResult.getId());
            
            // 患者信息
            Map<String, Object> patientInfo = new HashMap<>();
            if (triageRecord.getPatient() != null) {
                patientInfo.put("name", triageRecord.getPatient().getPatientName());
                patientInfo.put("idCard", triageRecord.getPatient().getIdNumber());
                patientInfo.put("age", triageRecord.getPatient().getAge());
                patientInfo.put("gender", triageRecord.getPatient().getGender());
            }
            doctorMessage.put("patient", patientInfo);
            
            // 分诊和诊断信息
            doctorMessage.put("triageLevel", triageRecord.getTriageLevel());
            doctorMessage.put("voiceComplaint", triageRecord.getChiefComplaint());
            doctorMessage.put("preliminaryDiagnosis", diagnosisResult.getPreliminaryDiagnosis());
            doctorMessage.put("urgencyLevel", diagnosisResult.getUrgencyLevel());
            
            // 生命体征
            if (triageRecord.getVitalSigns() != null) {
                // 解析生命体征JSON
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> vitalSigns = mapper.readValue(triageRecord.getVitalSigns(), Map.class);
                    doctorMessage.put("vitalSigns", vitalSigns);
                    
                    // 提取四个核心生理参数
                    Map<String, Object> coreVitals = new HashMap<>();
                    coreVitals.put("temperature", vitalSigns.get("temperature"));
                    coreVitals.put("systolicBP", vitalSigns.get("systolicBP"));
                    coreVitals.put("diastolicBP", vitalSigns.get("diastolicBP"));
                    coreVitals.put("heartRate", vitalSigns.get("heartRate"));
                    doctorMessage.put("coreVitalSigns", coreVitals);
                    
                } catch (Exception e) {
                    log.warn("解析生命体征数据失败", e);
                }
            }
            
            // 资源调度结果
            doctorMessage.put("resourceAllocation", resourceAllocation);
            doctorMessage.put("assignedDepartment", resourceAllocation.getAllocatedDepartment());
            doctorMessage.put("estimatedWaitTime", resourceAllocation.getEstimatedWaitTime());
            
            doctorMessage.put("timestamp", LocalDateTime.now());
            
            // 推送到医生端
            messagingTemplate.convertAndSend("/topic/doctor-new-patients", doctorMessage);
            
            // 如果是紧急患者，发送紧急通知
            if (triageRecord.getTriageLevel() != null && triageRecord.getTriageLevel() <= 2) {
                Map<String, Object> urgentAlert = new HashMap<>();
                urgentAlert.put("type", "URGENT_PATIENT_ALERT");
                urgentAlert.put("message", "紧急患者需要医生立即处理");
                urgentAlert.put("patientName", patientInfo.get("name"));
                urgentAlert.put("triageLevel", triageRecord.getTriageLevel());
                urgentAlert.put("department", resourceAllocation.getAllocatedDepartment());
                urgentAlert.put("preliminaryDiagnosis", diagnosisResult.getPreliminaryDiagnosis());
                urgentAlert.put("timestamp", LocalDateTime.now());
                
                messagingTemplate.convertAndSend("/topic/doctor-urgent-alerts", urgentAlert);
            }
            
            log.info("已发送患者数据到医生端 - 患者: {}, 科室: {}", 
                patientInfo.get("name"), resourceAllocation.getAllocatedDepartment());
                
        } catch (Exception e) {
            log.error("发送数据到医生端失败", e);
        }
    }
}