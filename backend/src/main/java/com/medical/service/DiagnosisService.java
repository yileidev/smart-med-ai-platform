package com.medical.service;

import com.medical.entity.DiagnosisRecord;
import com.medical.entity.TriageRecord;
import com.medical.entity.User;
import com.medical.repository.DiagnosisRecordRepository;
import com.medical.repository.TriageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisService {
    
    private final DiagnosisRecordRepository diagnosisRecordRepository;
    private final TriageRecordRepository triageRecordRepository;
    private final SystemLogService systemLogService;
    
    @Transactional
    @CacheEvict(value = {"triageStats", "patientList"}, allEntries = true)
    public DiagnosisRecord saveDiagnosis(DiagnosisRecord diagnosisRecord) {
        DiagnosisRecord saved = diagnosisRecordRepository.save(diagnosisRecord);
        
        // 如果是完成状态，更新分诊记录状态
        if (saved.getStatus() == DiagnosisRecord.DiagnosisStatus.COMPLETED) {
            TriageRecord triageRecord = saved.getTriageRecord();
            triageRecord.setStatus(TriageRecord.TriageStatus.COMPLETED);
            triageRecordRepository.save(triageRecord);
        }
        
        systemLogService.logUserAction(
            saved.getDoctor().getId(),
            saved.getDoctor().getUsername(),
            "CREATE_DIAGNOSIS",
            "DIAGNOSIS",
            saved.getId().toString(),
            "创建诊断记录，患者：" + saved.getTriageRecord().getPatient().getPatientName()
        );
        
        log.info("医生 {} 为患者 {} 创建诊断记录", 
            saved.getDoctor().getUsername(), 
            saved.getTriageRecord().getPatient().getPatientName());
        
        return saved;
    }
    
    public DiagnosisRecord findById(Long id) {
        return diagnosisRecordRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("诊断记录不存在"));
    }
    
    public DiagnosisRecord findByTriageRecord(TriageRecord triageRecord) {
        return diagnosisRecordRepository.findByTriageRecord(triageRecord)
            .orElse(null);
    }
    
    public Page<DiagnosisRecord> findByDoctor(User doctor, Pageable pageable) {
        return diagnosisRecordRepository.findByDoctorOrderByCreatedAtDesc(doctor, pageable);
    }
    
    public Page<DiagnosisRecord> findByStatus(DiagnosisRecord.DiagnosisStatus status, Pageable pageable) {
        return diagnosisRecordRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }
    
    public Long countByDoctorAndStatus(User doctor, DiagnosisRecord.DiagnosisStatus status) {
        return diagnosisRecordRepository.countByDoctorAndStatus(doctor, status);
    }
    
    public Long countTodayByDoctor(User doctor) {
        return diagnosisRecordRepository.countTodayByDoctor(doctor);
    }
    
    public Page<DiagnosisRecord> findByFilters(Long doctorId, DiagnosisRecord.DiagnosisStatus status, Pageable pageable) {
        return diagnosisRecordRepository.findByFilters(doctorId, status, pageable);
    }
    
    @Transactional
    public DiagnosisRecord updateDiagnosis(DiagnosisRecord diagnosisRecord) {
        DiagnosisRecord existing = findById(diagnosisRecord.getId());
        
        existing.setDiagnosis(diagnosisRecord.getDiagnosis());
        existing.setTreatmentPlan(diagnosisRecord.getTreatmentPlan());
        existing.setPrescription(diagnosisRecord.getPrescription());
        existing.setFollowUpInstructions(diagnosisRecord.getFollowUpInstructions());
        existing.setStatus(diagnosisRecord.getStatus());
        
        DiagnosisRecord updated = diagnosisRecordRepository.save(existing);
        
        // 如果状态改为完成，更新分诊记录
        if (updated.getStatus() == DiagnosisRecord.DiagnosisStatus.COMPLETED) {
            TriageRecord triageRecord = updated.getTriageRecord();
            triageRecord.setStatus(TriageRecord.TriageStatus.COMPLETED);
            triageRecordRepository.save(triageRecord);
        }
        
        systemLogService.logUserAction(
            updated.getDoctor().getId(),
            updated.getDoctor().getUsername(),
            "UPDATE_DIAGNOSIS",
            "DIAGNOSIS",
            updated.getId().toString(),
            "更新诊断记录"
        );
        
        return updated;
    }
    
    @Transactional
    public void deleteDiagnosis(Long id) {
        DiagnosisRecord diagnosis = findById(id);
        diagnosisRecordRepository.delete(diagnosis);
        
        systemLogService.logUserAction(
            diagnosis.getDoctor().getId(),
            diagnosis.getDoctor().getUsername(),
            "DELETE_DIAGNOSIS",
            "DIAGNOSIS",
            id.toString(),
            "删除诊断记录"
        );
    }
    
    /**
     * 统计今日完成的诊断数量
     */
    @Cacheable(value = "triageStats", key = "'todayCompleted'")
    public Long countTodayCompleted() {
        // 查询今天完成的诊断数量
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
        
        return diagnosisRecordRepository.countByStatusAndDiagnosisTimeBetween(
            DiagnosisRecord.DiagnosisStatus.COMPLETED,
            startOfDay,
            endOfDay
        );
    }
    
    /**
     * 获取待诊断患者列表
     */
    public List<TriageRecord> findPendingDiagnosisRecords() {
        // 获取已确认但未诊断的分诊记录
        return triageRecordRepository.findByStatusOrderByTriageLevelAscCreatedAtAsc(
            TriageRecord.TriageStatus.CONFIRMED,
            org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
    }
}