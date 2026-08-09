package com.medical.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * HL7数据映射表
 * 用于存储和管理医疗信息交换标准数据
 */
@Entity
@Table(name = "hl7_message_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HL7MessageMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * HL7消息ID
     */
    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;

    /**
     * HL7消息类型 (ADT, ORM, ORU, etc.)
     */
    @Column(name = "message_type", nullable = false)
    private String messageType;

    /**
     * 患者ID (关联Patient表)
     */
    @Column(name = "patient_id")
    private Long patientId;

    /**
     * 分诊记录ID (关联TriageRecord表)
     */
    @Column(name = "triage_record_id")
    private Long triageRecordId;

    /**
     * 诊断结果ID (关联DiagnosisResult表)
     */
    @Column(name = "diagnosis_result_id")
    private Long diagnosisResultId;

    /**
     * HL7原始消息内容
     */
    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;

    /**
     * 解析后的JSON数据
     */
    @Column(name = "parsed_data", columnDefinition = "JSON")
    private String parsedData;

    /**
     * 发送机构
     */
    @Column(name = "sending_facility")
    private String sendingFacility;

    /**
     * 接收机构
     */
    @Column(name = "receiving_facility")
    private String receivingFacility;

    /**
     * 消息状态 (PENDING, PROCESSED, ERROR)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MessageStatus status;

    /**
     * 处理时间
     */
    @Column(name = "processed_time")
    private LocalDateTime processedTime;

    /**
     * 错误信息
     */
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        if (status == null) {
            status = MessageStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }

    /**
     * HL7消息状态枚举
     */
    public enum MessageStatus {
        PENDING("待处理"),
        PROCESSED("已处理"), 
        ERROR("处理失败");

        private final String description;

        MessageStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}