package com.medical.service;

import com.medical.entity.*;
import com.medical.model.DiagnosisResult;
import com.medical.repository.EdgeDeviceDataRepository;
import com.medical.repository.DiagnosisResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 边缘设备数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdgeDataService {

    private final EdgeDeviceDataRepository edgeDeviceDataRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;
    private final SystemLogService systemLogService;
    private final TriageService triageService;
    private final MultimodalDiagnosisService multimodalDiagnosisService;
    private final MedicalResourceSchedulingService resourceSchedulingService;

    /**
     * 保存边缘设备数据
     */
    @Transactional
    public EdgeDeviceData saveEdgeData(EdgeDeviceData edgeData) {
        try {
            EdgeDeviceData saved = edgeDeviceDataRepository.save(edgeData);
            
            // 记录日志
            systemLogService.logUserAction(
                null, 
                "edge-device-" + edgeData.getDeviceId(),
                "RECEIVE_EDGE_DATA", 
                "EDGE_DATA", 
                saved.getId().toString(),
                "接收边缘设备数据，分诊等级：" + edgeData.getTriageLevel()
            );
            
            log.info("保存边缘设备数据成功 - 设备ID: {}, 数据ID: {}", 
                edgeData.getDeviceId(), saved.getId());
            
            return saved;
        } catch (Exception e) {
            log.error("保存边缘设备数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存边缘设备数据失败", e);
        }
    }

    /**
     * 处理边缘设备分诊数据 - 完整的多模态诊断和资源调度流程
     */
    @Transactional
    public Map<String, Object> processEdgeTriageData(EdgeDeviceData edgeData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始处理边缘分诊数据 - 设备ID: {}, 患者: {}", 
                edgeData.getDeviceId(), edgeData.getPatientName());

            // 1. 保存边缘设备原始数据
            EdgeDeviceData saved = saveEdgeData(edgeData);
            result.put("edgeDataId", saved.getId());

            // 2. 创建分诊记录（使用边缘端的AI分诊结果）
            TriageRecord triageRecord = createTriageRecordFromEdge(edgeData);
            result.put("triageRecordId", triageRecord.getId());

            // 3. 执行云端多模态AI诊断（结合电子病历）
            com.medical.model.DiagnosisResult modelDiagnosisResult = multimodalDiagnosisService.performMultimodalDiagnosis(triageRecord);
            
            // 转换为实体类并保存
            com.medical.entity.DiagnosisResult entityDiagnosisResult = convertToEntityDiagnosisResult(modelDiagnosisResult, triageRecord);
            entityDiagnosisResult = diagnosisResultRepository.save(entityDiagnosisResult);
            result.put("diagnosisResultId", entityDiagnosisResult.getId());

            // 4. 执行医疗资源调度
            ResourceAllocation resourceAllocation = resourceSchedulingService.scheduleResources(triageRecord, entityDiagnosisResult);
            result.put("resourceAllocationId", resourceAllocation.getId());

            // 5. 标记边缘数据已处理
            markAsProcessed(saved.getId());

            // 6. 构建处理结果摘要
            result.put("success", true);
            result.put("processingTime", System.currentTimeMillis() - saved.getReceivedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            result.put("triageLevel", triageRecord.getTriageLevel());
            result.put("preliminaryDiagnosis", entityDiagnosisResult.getPreliminaryDiagnosis());
            result.put("allocatedDepartment", resourceAllocation.getAllocatedDepartment());
            result.put("estimatedWaitTime", resourceAllocation.getEstimatedWaitTime());
            result.put("priorityScore", resourceAllocation.getPriorityScore());

            log.info("边缘分诊数据处理完成 - 患者: {}, 分诊等级: {}, 分配科室: {}", 
                edgeData.getPatientName(), triageRecord.getTriageLevel(), resourceAllocation.getAllocatedDepartment());

            return result;

        } catch (Exception e) {
            log.error("处理边缘分诊数据失败 - 设备ID: {}", edgeData.getDeviceId(), e);
            
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());
            
            // 更新边缘数据错误信息
            try {
                EdgeDeviceData errorData = edgeDeviceDataRepository.findById(edgeData.getId()).orElse(edgeData);
                errorData.setErrorMessage("云端处理失败: " + e.getMessage());
                edgeDeviceDataRepository.save(errorData);
            } catch (Exception saveError) {
                log.error("保存错误信息失败", saveError);
            }
            
            return result;
        }
    }

    /**
     * 从边缘数据创建分诊记录
     */
    private TriageRecord createTriageRecordFromEdge(EdgeDeviceData edgeData) {
        // 创建或查找患者
        Patient patient = new Patient();
        patient.setPatientName(edgeData.getPatientName());
        patient.setAge(edgeData.getPatientAge());
        patient.setGenderFromString(edgeData.getPatientGender());
        patient.setIdCard(edgeData.getPatientIdCard());
        patient.setPhoneNumber(edgeData.getPatientPhone());
        
        // 创建分诊记录
        TriageRecord triageRecord = new TriageRecord();
        triageRecord.setPatient(patient);
        triageRecord.setChiefComplaint(edgeData.getVoiceComplaint());
        triageRecord.setVitalSigns(edgeData.getVitalSigns());
        triageRecord.setTriageLevel(edgeData.getTriageLevel());
        triageRecord.setTriageScore(edgeData.getTriageScore());
        triageRecord.setAiDiagnosis(edgeData.getAiDiagnosis());
        triageRecord.setAiConfidence(edgeData.getAiConfidence());
        triageRecord.setArrivalTime(edgeData.getReceivedTime());
        
        // 使用边缘端的分诊结果保存
        return triageService.saveTriageRecordFromEdge(triageRecord);
    }

    /**
     * 标记数据已处理
     */
    @Transactional
    public void markAsProcessed(Long dataId) {
        try {
            EdgeDeviceData data = edgeDeviceDataRepository.findById(dataId)
                .orElseThrow(() -> new RuntimeException("边缘数据不存在"));
            
            data.setProcessed(true);
            edgeDeviceDataRepository.save(data);
            
            log.info("标记边缘数据已处理: {}", dataId);
        } catch (Exception e) {
            log.error("标记数据处理状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新设备状态
     */
    @Transactional
    public void updateDeviceStatus(String deviceId, String status, String errorMessage) {
        try {
            // 查找该设备最新的数据记录
            EdgeDeviceData latestData = edgeDeviceDataRepository
                .findTopByDeviceIdOrderByReceivedTimeDesc(deviceId);
            
            if (latestData == null) {
                // 如果没有数据记录，创建一个状态记录
                latestData = new EdgeDeviceData();
                latestData.setDeviceId(deviceId);
            }
            
            latestData.setDeviceStatus(status);
            latestData.setErrorMessage(errorMessage);
            latestData.setReceivedTime(LocalDateTime.now());
            
            edgeDeviceDataRepository.save(latestData);
            
            log.info("更新设备状态: {} -> {}", deviceId, status);
        } catch (Exception e) {
            log.error("更新设备状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新设备心跳
     */
    @Transactional
    public void updateDeviceHeartbeat(String deviceId, String timestamp, Map<String, Object> systemInfo) {
        try {
            EdgeDeviceData latestData = edgeDeviceDataRepository
                .findTopByDeviceIdOrderByReceivedTimeDesc(deviceId);
            
            if (latestData == null) {
                latestData = new EdgeDeviceData();
                latestData.setDeviceId(deviceId);
                latestData.setDeviceStatus("ONLINE");
            }
            
            // 更新心跳时间
            latestData.setReceivedTime(LocalDateTime.now());
            
            // 如果设备之前是离线状态，更新为在线
            if ("OFFLINE".equals(latestData.getDeviceStatus())) {
                latestData.setDeviceStatus("ONLINE");
                log.info("设备重新上线: {}", deviceId);
            }
            
            edgeDeviceDataRepository.save(latestData);
            
        } catch (Exception e) {
            log.error("更新设备心跳失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取边缘设备状态列表
     */
    public List<Map<String, Object>> getEdgeDeviceStatus() {
        try {
            // 获取所有设备的最新状态
            List<EdgeDeviceData> latestDataList = edgeDeviceDataRepository.findLatestByDevice();
            
            return latestDataList.stream().map(data -> {
                Map<String, Object> deviceStatus = new HashMap<>();
                deviceStatus.put("deviceId", data.getDeviceId());
                deviceStatus.put("status", data.getDeviceStatus());
                deviceStatus.put("lastHeartbeat", data.getReceivedTime());
                deviceStatus.put("errorMessage", data.getErrorMessage());
                
                // 计算在线状态
                boolean isOnline = "ONLINE".equals(data.getDeviceStatus()) && 
                    data.getReceivedTime() != null &&
                    data.getReceivedTime().isAfter(LocalDateTime.now().minusMinutes(5));
                
                deviceStatus.put("online", isOnline);
                
                // 统计该设备的数据量
                Long dataCount = edgeDeviceDataRepository.countByDeviceId(data.getDeviceId());
                deviceStatus.put("dataCount", dataCount);
                
                // 统计今日数据量
                Long todayDataCount = edgeDeviceDataRepository.countByDeviceIdAndReceivedTimeAfter(
                    data.getDeviceId(), LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
                deviceStatus.put("todayDataCount", todayDataCount);
                
                return deviceStatus;
            }).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("获取设备状态失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 获取未处理的边缘数据
     */
    public Page<EdgeDeviceData> getUnprocessedData(Pageable pageable) {
        return edgeDeviceDataRepository.findByProcessedFalseOrderByReceivedTimeDesc(pageable);
    }

    /**
     * 根据设备ID获取数据
     */
    public Page<EdgeDeviceData> getDataByDevice(String deviceId, Pageable pageable) {
        return edgeDeviceDataRepository.findByDeviceIdOrderByReceivedTimeDesc(deviceId, pageable);
    }

    /**
     * 获取边缘数据统计
     */
    public Map<String, Object> getEdgeDataStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 总数据量
            Long totalData = edgeDeviceDataRepository.count();
            stats.put("totalData", totalData);
            
            // 今日数据量
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            Long todayData = edgeDeviceDataRepository.countByReceivedTimeAfter(todayStart);
            stats.put("todayData", todayData);
            
            // 未处理数据量
            Long unprocessedData = edgeDeviceDataRepository.countByProcessedFalse();
            stats.put("unprocessedData", unprocessedData);
            
            // 在线设备数量
            List<String> onlineDevices = edgeDeviceDataRepository.findOnlineDevices(
                LocalDateTime.now().minusMinutes(5));
            stats.put("onlineDevices", onlineDevices.size());
            
            // 各分诊等级统计
            Map<Integer, Long> triageLevelStats = new HashMap<>();
            for (int level = 1; level <= 5; level++) {
                Long count = edgeDeviceDataRepository.countByTriageLevel(level);
                triageLevelStats.put(level, count);
            }
            stats.put("triageLevelStats", triageLevelStats);
            
            // 数据质量统计
            Double avgDataQuality = edgeDeviceDataRepository.getAverageDataQualityScore();
            stats.put("avgDataQuality", avgDataQuality != null ? avgDataQuality : 0.0);
            
            return stats;
        } catch (Exception e) {
            log.error("获取边缘数据统计失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 清理过期数据
     */
    @Transactional
    public void cleanExpiredData(int daysToKeep) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
            Long deletedCount = edgeDeviceDataRepository.deleteByReceivedTimeBefore(cutoffDate);
            
            log.info("清理过期边缘数据完成，删除 {} 条记录", deletedCount);
            
            systemLogService.logUserAction(
                null, "system", "CLEAN_EDGE_DATA", "MAINTENANCE", 
                null, "清理过期边缘数据：" + deletedCount + "条"
            );
            
        } catch (Exception e) {
            log.error("清理过期数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取数据质量报告
     */
    public Map<String, Object> getDataQualityReport() {
        try {
            Map<String, Object> report = new HashMap<>();
            
            // 按设备统计数据质量
            List<Map<String, Object>> deviceQualityStats = edgeDeviceDataRepository.getDataQualityByDevice();
            report.put("deviceQualityStats", deviceQualityStats);
            
            // 按时间统计数据质量趋势
            List<Map<String, Object>> qualityTrend = edgeDeviceDataRepository.getDataQualityTrend();
            report.put("qualityTrend", qualityTrend);
            
            // 异常数据统计
            Long lowQualityCount = edgeDeviceDataRepository.countByDataQualityScoreLessThan(0.7);
            report.put("lowQualityCount", lowQualityCount);
            
            return report;
        } catch (Exception e) {
            log.error("获取数据质量报告失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 重新处理失败的数据
     */
    @Transactional
    public void reprocessFailedData() {
        try {
            List<EdgeDeviceData> failedData = edgeDeviceDataRepository.findByProcessedFalseAndErrorMessageIsNotNull();
            
            for (EdgeDeviceData data : failedData) {
                try {
                    // 重置错误信息
                    data.setErrorMessage(null);
                    data.setProcessed(false);
                    edgeDeviceDataRepository.save(data);
                    
                    // 这里可以触发重新处理逻辑
                    log.info("重新处理边缘数据: {}", data.getId());
                    
                } catch (Exception e) {
                    log.error("重新处理数据失败 - ID: {}, 错误: {}", data.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("批量重新处理失败数据出错: {}", e.getMessage(), e);
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