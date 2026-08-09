package com.medical.repository;

import com.medical.entity.DiagnosisRecord;
import com.medical.entity.TriageRecord;
import com.medical.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisRecordRepository extends JpaRepository<DiagnosisRecord, Long> {
    
    Optional<DiagnosisRecord> findByTriageRecord(TriageRecord triageRecord);
    
    Page<DiagnosisRecord> findByDoctorOrderByCreatedAtDesc(User doctor, Pageable pageable);
    
    Page<DiagnosisRecord> findByStatusOrderByCreatedAtDesc(DiagnosisRecord.DiagnosisStatus status, Pageable pageable);
    
    @Query("SELECT COUNT(d) FROM DiagnosisRecord d WHERE d.doctor = :doctor AND d.status = :status")
    Long countByDoctorAndStatus(@Param("doctor") User doctor, @Param("status") DiagnosisRecord.DiagnosisStatus status);
    
    @Query("SELECT COUNT(d) FROM DiagnosisRecord d WHERE d.doctor = :doctor AND DATE(d.createdAt) = CURRENT_DATE")
    Long countTodayByDoctor(@Param("doctor") User doctor);
    
    @Query("SELECT d FROM DiagnosisRecord d WHERE d.createdAt BETWEEN :startTime AND :endTime")
    List<DiagnosisRecord> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT d FROM DiagnosisRecord d WHERE " +
           "(:doctorId IS NULL OR d.doctor.id = :doctorId) AND " +
           "(:status IS NULL OR d.status = :status) " +
           "ORDER BY d.createdAt DESC")
    Page<DiagnosisRecord> findByFilters(@Param("doctorId") Long doctorId,
                                      @Param("status") DiagnosisRecord.DiagnosisStatus status,
                                      Pageable pageable);
    
    @Query("SELECT COUNT(d) FROM DiagnosisRecord d WHERE d.status = :status AND d.diagnosisTime BETWEEN :startTime AND :endTime")
    Long countByStatusAndDiagnosisTimeBetween(@Param("status") DiagnosisRecord.DiagnosisStatus status,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
}