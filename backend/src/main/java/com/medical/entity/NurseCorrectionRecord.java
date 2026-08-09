package com.medical.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 护士修正记录实体
 */
@Data
@Entity
@Table(name = "nurse_correction_record")
public class NurseCorrectionRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 原始边缘数据ID
     */
    @Column(name = "edge_data_id", nullable = false)
    private Long edgeDataId;
    
    /**
     * 护士ID
     */
    @Column(name = "nurse_id", nullable = false)
    private Long nurseId;
    
    /**
     * 护士姓名
     */
    @Column(name = "nurse_name", length = 50, nullable = false)
    private String nurseName;
    
    /**
     * 修正后的生命体征数据(JSON)
     */
    @Column(name = "corrected_sensor_data", columnDefinition = "TEXT")
    private String correctedSensorData;
    
    /**
     * 修正后的主诉
     */
    @Column(name = "corrected_chief_complaint", columnDefinition = "TEXT")
    private String correctedChiefComplaint;
    
    /**
     * 护士备注
     */
    @Column(name = "nurse_notes", columnDefinition = "TEXT")
    private String nurseNotes;
    
    /**
     * 修正时间
     */
    @Column(name = "correction_time", nullable = false)
    private LocalDateTime correctionTime;
    
    /**
     * 状态: SENT_TO_EDGE, REASSESSED, COMPLETED
     */
    @Column(length = 20)
    private String status;
    
    /**
     * 最终分诊等级（重新分诊后）
     */
    @Column(name = "final_triage_level")
    private Integer finalTriageLevel;
    
    /**
     * 收到最终结果时间
     */
    @Column(name = "final_received_time")
    private LocalDateTime finalReceivedTime;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
