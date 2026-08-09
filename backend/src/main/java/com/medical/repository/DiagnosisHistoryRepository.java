package com.medical.repository;

import com.medical.entity.DiagnosisHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiagnosisHistoryRepository extends JpaRepository<DiagnosisHistory, Long> {
    
    /**
     * 根据患者ID查询诊断历史
     */
    List<DiagnosisHistory> findByPatientIdOrderByDiagnosisTimeDesc(Long patientId);
    
    /**
     * 根据医生ID查询诊断历史
     */
    Page<DiagnosisHistory> findByDoctorIdOrderByDiagnosisTimeDesc(Long doctorId, Pageable pageable);
    
    /**
     * 根据时间范围查询
     */
    Page<DiagnosisHistory> findByDiagnosisTimeBetweenOrderByDiagnosisTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据分诊记录ID查询
     */
    DiagnosisHistory findByTriageRecordId(Long triageRecordId);
}
