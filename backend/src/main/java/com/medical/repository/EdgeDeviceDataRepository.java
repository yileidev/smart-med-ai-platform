package com.medical.repository;

import com.medical.entity.EdgeDeviceData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 边缘设备数据仓库
 */
@Repository
public interface EdgeDeviceDataRepository extends JpaRepository<EdgeDeviceData, Long> {

    /**
     * 根据设备ID查找最新数据
     */
    EdgeDeviceData findTopByDeviceIdOrderByReceivedTimeDesc(String deviceId);

    /**
     * 根据设备ID查找数据（分页）
     */
    Page<EdgeDeviceData> findByDeviceIdOrderByReceivedTimeDesc(String deviceId, Pageable pageable);

    /**
     * 查找未处理的数据
     */
    Page<EdgeDeviceData> findByProcessedFalseOrderByReceivedTimeDesc(Pageable pageable);

    /**
     * 查找有错误信息且未处理的数据
     */
    List<EdgeDeviceData> findByProcessedFalseAndErrorMessageIsNotNull();

    /**
     * 统计设备数据量
     */
    Long countByDeviceId(String deviceId);

    /**
     * 统计指定时间后的设备数据量
     */
    Long countByDeviceIdAndReceivedTimeAfter(String deviceId, LocalDateTime time);

    /**
     * 统计指定时间后的总数据量
     */
    Long countByReceivedTimeAfter(LocalDateTime time);

    /**
     * 统计未处理数据量
     */
    Long countByProcessedFalse();

    /**
     * 统计指定分诊等级的数据量
     */
    Long countByTriageLevel(Integer triageLevel);

    /**
     * 统计数据质量评分低于指定值的数据量
     */
    Long countByDataQualityScoreLessThan(Double score);

    /**
     * 获取各设备最新状态
     */
    @Query("SELECT e FROM EdgeDeviceData e WHERE e.id IN " +
           "(SELECT MAX(e2.id) FROM EdgeDeviceData e2 GROUP BY e2.deviceId)")
    List<EdgeDeviceData> findLatestByDevice();

    /**
     * 查找在线设备
     */
    @Query("SELECT DISTINCT e.deviceId FROM EdgeDeviceData e WHERE " +
           "e.deviceStatus = 'ONLINE' AND e.receivedTime > :cutoffTime")
    List<String> findOnlineDevices(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 获取平均数据质量评分
     */
    @Query("SELECT AVG(e.dataQualityScore) FROM EdgeDeviceData e WHERE e.dataQualityScore IS NOT NULL")
    Double getAverageDataQualityScore();

    /**
     * 按设备统计数据质量
     */
    @Query("SELECT e.deviceId as deviceId, AVG(e.dataQualityScore) as avgQuality, " +
           "COUNT(e) as totalCount FROM EdgeDeviceData e " +
           "WHERE e.dataQualityScore IS NOT NULL " +
           "GROUP BY e.deviceId")
    List<Map<String, Object>> getDataQualityByDevice();

    /**
     * 获取数据质量趋势(按小时)
     */
    @Query(value = "SELECT HOUR(received_time) as hour, " +
           "AVG(data_quality_score) as avgQuality, COUNT(*) as count " +
           "FROM edge_device_data " +
           "WHERE received_time >= :startTime AND data_quality_score IS NOT NULL " +
           "GROUP BY HOUR(received_time) " +
           "ORDER BY hour", nativeQuery = true)
    List<Map<String, Object>> getDataQualityTrend(@Param("startTime") LocalDateTime startTime);

    /**
     * 获取数据质量趋势（默认24小时）
     */
    default List<Map<String, Object>> getDataQualityTrend() {
        return getDataQualityTrend(LocalDateTime.now().minusHours(24));
    }

    /**
     * 删除指定时间之前的数据
     */
    @Modifying
    @Query("DELETE FROM EdgeDeviceData e WHERE e.receivedTime < :cutoffTime")
    Long deleteByReceivedTimeBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 按分诊等级和时间范围统计
     */
    @Query("SELECT e.triageLevel as level, COUNT(e) as count " +
           "FROM EdgeDeviceData e " +
           "WHERE e.receivedTime BETWEEN :startTime AND :endTime " +
           "GROUP BY e.triageLevel " +
           "ORDER BY e.triageLevel")
    List<Map<String, Object>> getTriageLevelStatsByTimeRange(
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 获取设备处理时间统计
     */
    @Query("SELECT e.deviceId as deviceId, " +
           "AVG(e.edgeProcessingTime) as avgProcessingTime, " +
           "MIN(e.edgeProcessingTime) as minProcessingTime, " +
           "MAX(e.edgeProcessingTime) as maxProcessingTime " +
           "FROM EdgeDeviceData e " +
           "WHERE e.edgeProcessingTime IS NOT NULL " +
           "GROUP BY e.deviceId")
    List<Map<String, Object>> getProcessingTimeStatsByDevice();

    /**
     * 获取语音识别置信度统计
     */
    @Query("SELECT AVG(e.voiceConfidence) as avgConfidence, " +
           "MIN(e.voiceConfidence) as minConfidence, " +
           "MAX(e.voiceConfidence) as maxConfidence, " +
           "COUNT(e) as totalCount " +
           "FROM EdgeDeviceData e " +
           "WHERE e.voiceConfidence IS NOT NULL")
    Map<String, Object> getVoiceConfidenceStats();

    /**
     * 获取分诊置信度统计
     */
    @Query("SELECT AVG(e.triageConfidence) as avgConfidence, " +
           "MIN(e.triageConfidence) as minConfidence, " +
           "MAX(e.triageConfidence) as maxConfidence, " +
           "COUNT(e) as totalCount " +
           "FROM EdgeDeviceData e " +
           "WHERE e.triageConfidence IS NOT NULL")
    Map<String, Object> getTriageConfidenceStats();

    /**
     * 按时间段统计数据接收量
     */
    @Query(value = "SELECT DATE(received_time) as date, " +
           "HOUR(received_time) as hour, " +
           "COUNT(*) as count " +
           "FROM edge_device_data " +
           "WHERE received_time >= :startTime " +
           "GROUP BY DATE(received_time), HOUR(received_time) " +
           "ORDER BY date, hour", nativeQuery = true)
    List<Map<String, Object>> getDataReceiveStats(@Param("startTime") LocalDateTime startTime);

    /**
     * 查找异常数据（数据质量低或处理时间过长）
     */
    @Query("SELECT e FROM EdgeDeviceData e WHERE " +
           "(e.dataQualityScore IS NOT NULL AND e.dataQualityScore < :qualityThreshold) OR " +
           "(e.edgeProcessingTime IS NOT NULL AND e.edgeProcessingTime > :timeThreshold)")
    Page<EdgeDeviceData> findAbnormalData(
        @Param("qualityThreshold") Double qualityThreshold,
        @Param("timeThreshold") Long timeThreshold,
        Pageable pageable
    );

    /**
     * 获取设备健康状态
     */
    @Query("SELECT e.deviceId as deviceId, " +
           "e.deviceStatus as status, " +
           "e.receivedTime as lastHeartbeat, " +
           "AVG(e.dataQualityScore) as avgQuality, " +
           "COUNT(e) as dataCount " +
           "FROM EdgeDeviceData e " +
           "WHERE e.receivedTime >= :startTime " +
           "GROUP BY e.deviceId, e.deviceStatus, e.receivedTime " +
           "ORDER BY e.receivedTime DESC")
    List<Map<String, Object>> getDeviceHealthStatus(@Param("startTime") LocalDateTime startTime);
    
    /**
     * 根据处理状态查询数据
     */
    List<EdgeDeviceData> findByProcessingStatus(String processingStatus);
    
    /**
     * 统计指定处理状态的数据量
     */
    Long countByProcessingStatus(String processingStatus);
    
    /**
     * 统计指定创建时间之后的数据量
     */
    Long countByCreatedAtAfter(LocalDateTime createdAt);
    
    /**
     * 统计指定处理状态和分诊等级的数据量
     */
    Long countByProcessingStatusAndTriageLevelLessThanEqual(String processingStatus, Integer triageLevel);
    
    /**
     * 根据接收时间查询数据（用于边缘设备监控）
     */
    List<EdgeDeviceData> findByReceivedTimeAfterOrderByReceivedTimeDesc(LocalDateTime receivedTime);
}