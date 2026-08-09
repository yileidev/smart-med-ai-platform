package com.medical.repository;

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
public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {
    
    Page<TriageRecord> findByStatusOrderByTriageLevelAscCreatedAtAsc(TriageRecord.TriageStatus status, Pageable pageable);
    
    Page<TriageRecord> findByAssignedDoctorAndStatusOrderByTriageLevelAscCreatedAtAsc(User doctor, TriageRecord.TriageStatus status, Pageable pageable);
    
    Page<TriageRecord> findByAssignedNurseAndStatusOrderByTriageLevelAscCreatedAtAsc(User nurse, TriageRecord.TriageStatus status, Pageable pageable);
    
    List<TriageRecord> findByTriageLevelAndStatus(Integer triageLevel, TriageRecord.TriageStatus status);
    
    @Query("SELECT COUNT(t) FROM TriageRecord t WHERE t.status = :status")
    Long countByStatus(@Param("status") TriageRecord.TriageStatus status);
    
    @Query("SELECT COUNT(t) FROM TriageRecord t WHERE t.triageLevel = :level AND t.status = :status")
    Long countByTriageLevelAndStatus(@Param("level") Integer triageLevel, @Param("status") TriageRecord.TriageStatus status);
    
    @Query("SELECT COUNT(t) FROM TriageRecord t WHERE t.assignedDoctor = :doctor AND t.status = :status")
    Long countByAssignedDoctorAndStatus(@Param("doctor") User doctor, @Param("status") TriageRecord.TriageStatus status);
    
    @Query("SELECT COUNT(t) FROM TriageRecord t WHERE t.assignedDoctor = :doctor AND DATE(t.createdAt) = CURRENT_DATE")
    Long countTodayCompletedByDoctor(@Param("doctor") User doctor);
    
    @Query("SELECT t FROM TriageRecord t WHERE " +
           "(:department IS NULL OR t.assignedDepartment = :department) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:triageLevel IS NULL OR t.triageLevel = :triageLevel) " +
           "ORDER BY t.triageLevel ASC, t.createdAt ASC")
    Page<TriageRecord> findByFilters(@Param("department") String department,
                                   @Param("status") TriageRecord.TriageStatus status,
                                   @Param("triageLevel") Integer triageLevel,
                                   Pageable pageable);
    
    @Query("SELECT t FROM TriageRecord t WHERE t.createdAt BETWEEN :startTime AND :endTime")
    List<TriageRecord> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    Optional<TriageRecord> findByPatient_IdAndStatus(Long patientId, TriageRecord.TriageStatus status);
    
    /**
     * 分诊确认相关查询方法
     */
    Page<TriageRecord> findByStatusAndDataSourceOrderByArrivalTimeAsc(TriageRecord.TriageStatus status, String dataSource, Pageable pageable);
    
    long countByStatusAndDataSource(TriageRecord.TriageStatus status, String dataSource);
    
    long countByStatusAndConfirmedTimeAfter(TriageRecord.TriageStatus status, LocalDateTime confirmedTime);
    
    long countByStatusAndUpdatedAtAfter(TriageRecord.TriageStatus status, LocalDateTime updatedAt);
    
    long countByTriageLevelAndStatusAndConfirmedTimeAfter(Integer triageLevel, TriageRecord.TriageStatus status, LocalDateTime confirmedTime);
    
    /**
     * 根据患者ID查找所有分诊记录
     */
    List<TriageRecord> findByPatient_Id(Long patientId);
    
    /**
     * 根据状态查找所有分诊记录
     */
    List<TriageRecord> findByStatus(TriageRecord.TriageStatus status);
    
    /**
     * 根据状态分页查找
     */
    Page<TriageRecord> findByStatus(TriageRecord.TriageStatus status, Pageable pageable);
    
    /**
     * 根据状态和时间范围分页查找
     */
    Page<TriageRecord> findByStatusAndCreatedAtBetween(
        TriageRecord.TriageStatus status, 
        LocalDateTime startTime, 
        LocalDateTime endTime, 
        Pageable pageable);
    
    /**
     * 根据分配科室和状态查找
     */
    List<TriageRecord> findByAssignedDepartmentAndStatus(String department, TriageRecord.TriageStatus status);
    
    /**
     * 根据到院时间范围查询
     */
    List<TriageRecord> findByArrivalTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    // ========== 统计查询方法 ==========
    
    /**
     * 按分诊等级统计
     */
    Long countByTriageLevel(Integer triageLevel);
    
    /**
     * 按创建时间范围统计
     */
    Long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 按科室统计
     */
    Long countByAssignedDepartment(String department);
    
    /**
     * 统计AI置信度大于指定值的记录数
     */
    @Query("SELECT COUNT(t) FROM TriageRecord t WHERE t.aiConfidence > :confidence")
    Long countByAiConfidenceGreaterThan(@Param("confidence") Double confidence);
    
    /**
     * 按分诊等级和时间范围统计
     */
    @Query("SELECT COUNT(t) FROM TriageRecord t WHERE t.triageLevel = :level AND t.createdAt BETWEEN :startTime AND :endTime")
    Long countByTriageLevelAndCreatedAtBetween(@Param("level") Integer triageLevel, 
                                                @Param("startTime") LocalDateTime startTime, 
                                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 按科室和时间范围统计
     */
    @Query("SELECT t.assignedDepartment, COUNT(t) FROM TriageRecord t WHERE t.createdAt BETWEEN :startTime AND :endTime GROUP BY t.assignedDepartment")
    List<Object[]> countByDepartmentAndCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                                        @Param("endTime") LocalDateTime endTime);
}