package com.medical.repository;

import com.medical.entity.NurseCorrectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 护士修正记录Repository
 */
@Repository
public interface NurseCorrectionRepository extends JpaRepository<NurseCorrectionRecord, Long> {
    
    /**
     * 根据边缘数据ID查找修正记录
     */
    List<NurseCorrectionRecord> findByEdgeDataId(Long edgeDataId);
    
    /**
     * 根据边缘数据ID查找修正记录，按创建时间倒序
     */
    List<NurseCorrectionRecord> findByEdgeDataIdOrderByCreatedAtDesc(Long edgeDataId);
    
    /**
     * 根据护士ID查找修正记录
     */
    List<NurseCorrectionRecord> findByNurseId(Long nurseId);
    
    /**
     * 根据状态查找修正记录
     */
    List<NurseCorrectionRecord> findByStatus(String status);
    
    /**
     * 根据边缘数据ID查找最新的修正记录
     */
    Optional<NurseCorrectionRecord> findTopByEdgeDataIdOrderByCreatedAtDesc(Long edgeDataId);
    
    /**
     * 统计指定状态和时间之后的记录数量
     */
    Long countByStatusAndCorrectionTimeAfter(String status, LocalDateTime correctionTime);
}
