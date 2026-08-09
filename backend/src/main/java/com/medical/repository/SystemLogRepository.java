package com.medical.repository;

import com.medical.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    
    @Query("SELECT l FROM SystemLog l WHERE " +
           "(:level IS NULL OR l.level = :level) " +
           "AND (:action IS NULL OR l.action LIKE %:action%) " +
           "AND (:userName IS NULL OR l.userName LIKE %:userName%) " +
           "AND (:startTime IS NULL OR l.createdAt >= :startTime) " +
           "AND (:endTime IS NULL OR l.createdAt <= :endTime)")
    Page<SystemLog> findByConditions(@Param("level") SystemLog.LogLevel level,
                                   @Param("action") String action,
                                   @Param("userName") String userName,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   Pageable pageable);
    
    long countByLevel(SystemLog.LogLevel level);
    
    @Query("SELECT COUNT(l) FROM SystemLog l WHERE l.createdAt >= :startTime")
    long countByCreatedAtAfter(@Param("startTime") LocalDateTime startTime);
}