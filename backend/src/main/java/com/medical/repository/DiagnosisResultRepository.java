package com.medical.repository;

import com.medical.entity.DiagnosisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisResultRepository extends JpaRepository<DiagnosisResult, Long> {

    Optional<DiagnosisResult> findByTriageRecordId(Long triageRecordId);

    List<DiagnosisResult> findByTriageRecordIdOrderByCreatedAtDesc(Long triageRecordId);

    @Query("SELECT dr FROM DiagnosisResult dr WHERE dr.createdAt >= :fromDate " +
           "ORDER BY dr.createdAt DESC")
    List<DiagnosisResult> findRecentDiagnoses(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT dr FROM DiagnosisResult dr WHERE dr.urgencyLevel >= :minUrgency " +
           "AND dr.status = 'PENDING' ORDER BY dr.urgencyLevel DESC, dr.createdAt ASC")
    List<DiagnosisResult> findUrgentPendingDiagnoses(@Param("minUrgency") Integer minUrgency);

    long countByStatus(DiagnosisResult.DiagnosisStatus status);

    @Query("SELECT AVG(dr.processingTimeMs) FROM DiagnosisResult dr WHERE dr.createdAt >= :fromDate")
    Double getAverageProcessingTime(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * 按时间范围统计诊断数量
     */
    @Query("SELECT COUNT(dr) FROM DiagnosisResult dr WHERE dr.createdAt BETWEEN :startTime AND :endTime")
    Long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                 @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按时间范围计算平均置信度
     */
    @Query("SELECT AVG(dr.diagnosisConfidence) FROM DiagnosisResult dr WHERE dr.createdAt BETWEEN :startTime AND :endTime")
    Double averageConfidenceByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计某时间之后的诊断数量
     */
    long countByCreatedAtAfter(LocalDateTime time);
    
    /**
     * 获取平均响应时间
     */
    @Query("SELECT AVG(dr.processingTimeMs) FROM DiagnosisResult dr")
    Double averageResponseTime();
}