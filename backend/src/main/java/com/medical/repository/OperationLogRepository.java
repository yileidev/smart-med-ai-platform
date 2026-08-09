package com.medical.repository;

import com.medical.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    
    /**
     * 根据用户ID查询操作日志
     */
    Page<OperationLog> findByUserIdOrderByOperationTimeDesc(Long userId, Pageable pageable);
    
    /**
     * 根据操作类型查询
     */
    Page<OperationLog> findByOperationTypeOrderByOperationTimeDesc(String operationType, Pageable pageable);
    
    /**
     * 根据时间范围查询
     */
    Page<OperationLog> findByOperationTimeBetweenOrderByOperationTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据状态查询
     */
    Page<OperationLog> findByStatusOrderByOperationTimeDesc(String status, Pageable pageable);
}
