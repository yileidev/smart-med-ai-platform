package com.medical.service;

import com.medical.entity.DiagnosisHistory;
import com.medical.entity.OperationLog;
import com.medical.repository.DiagnosisHistoryRepository;
import com.medical.repository.OperationLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class HistoryService {

    private final DiagnosisHistoryRepository diagnosisHistoryRepository;
    private final OperationLogRepository operationLogRepository;

    public HistoryService(DiagnosisHistoryRepository diagnosisHistoryRepository,
                          OperationLogRepository operationLogRepository) {
        this.diagnosisHistoryRepository = diagnosisHistoryRepository;
        this.operationLogRepository = operationLogRepository;
    }

    /**
     * 保存诊断历史
     */
    public DiagnosisHistory saveDiagnosisHistory(DiagnosisHistory history) {
        return diagnosisHistoryRepository.save(history);
    }

    /**
     * 根据患者ID查询诊断历史
     */
    public List<DiagnosisHistory> getPatientDiagnosisHistory(Long patientId) {
        return diagnosisHistoryRepository.findByPatientIdOrderByDiagnosisTimeDesc(patientId);
    }

    /**
     * 根据医生ID查询诊断历史
     */
    public Page<DiagnosisHistory> getDoctorDiagnosisHistory(Long doctorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return diagnosisHistoryRepository.findByDoctorIdOrderByDiagnosisTimeDesc(doctorId, pageable);
    }

    /**
     * 根据时间范围查询诊断历史
     */
    public Page<DiagnosisHistory> getDiagnosisHistoryByTimeRange(
            LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return diagnosisHistoryRepository.findByDiagnosisTimeBetweenOrderByDiagnosisTimeDesc(
                startTime, endTime, pageable);
    }

    /**
     * 保存操作日志
     */
    public OperationLog saveOperationLog(OperationLog log) {
        return operationLogRepository.save(log);
    }

    /**
     * 根据用户ID查询操作日志
     */
    public Page<OperationLog> getUserOperationLogs(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return operationLogRepository.findByUserIdOrderByOperationTimeDesc(userId, pageable);
    }

    /**
     * 根据时间范围查询操作日志
     */
    public Page<OperationLog> getOperationLogsByTimeRange(
            LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return operationLogRepository.findByOperationTimeBetweenOrderByOperationTimeDesc(
                startTime, endTime, pageable);
    }

    /**
     * 根据操作类型查询日志
     */
    public Page<OperationLog> getOperationLogsByType(String operationType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return operationLogRepository.findByOperationTypeOrderByOperationTimeDesc(operationType, pageable);
    }
}
