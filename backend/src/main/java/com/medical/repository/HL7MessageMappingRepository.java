package com.medical.repository;

import com.medical.entity.HL7MessageMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HL7MessageMappingRepository extends JpaRepository<HL7MessageMapping, Long> {

    /**
     * 根据消息ID查找
     */
    Optional<HL7MessageMapping> findByMessageId(String messageId);

    /**
     * 根据患者ID查找相关消息
     */
    List<HL7MessageMapping> findByPatientIdOrderByCreatedTimeDesc(Long patientId);

    /**
     * 根据分诊记录ID查找相关消息
     */
    List<HL7MessageMapping> findByTriageRecordIdOrderByCreatedTimeDesc(Long triageRecordId);

    /**
     * 根据消息类型查找
     */
    List<HL7MessageMapping> findByMessageTypeOrderByCreatedTimeDesc(String messageType);

    /**
     * 根据状态查找
     */
    List<HL7MessageMapping> findByStatusOrderByCreatedTimeDesc(HL7MessageMapping.MessageStatus status);

    /**
     * 查找待处理的消息
     */
    @Query("SELECT h FROM HL7MessageMapping h WHERE h.status = 'PENDING' ORDER BY h.createdTime ASC")
    List<HL7MessageMapping> findPendingMessages();

    /**
     * 查找指定时间范围内的消息
     */
    @Query("SELECT h FROM HL7MessageMapping h WHERE h.createdTime BETWEEN :startTime AND :endTime ORDER BY h.createdTime DESC")
    List<HL7MessageMapping> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各状态的消息数量
     */
    @Query("SELECT h.status, COUNT(h) FROM HL7MessageMapping h GROUP BY h.status")
    List<Object[]> countByStatus();

    /**
     * 查找处理失败的消息
     */
    @Query("SELECT h FROM HL7MessageMapping h WHERE h.status = 'ERROR' AND h.createdTime >= :since ORDER BY h.createdTime DESC")
    List<HL7MessageMapping> findRecentErrors(@Param("since") LocalDateTime since);
}