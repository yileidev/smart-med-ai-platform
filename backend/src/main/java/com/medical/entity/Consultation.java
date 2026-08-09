package com.medical.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 会诊记录实体
 */
@Data
@Entity
@Table(name = "consultation")
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分诊记录ID
     */
    @Column(name = "triage_record_id")
    private Long triageRecordId;

    /**
     * 患者ID
     */
    @Column(name = "patient_id")
    private Long patientId;

    /**
     * 申请医生ID
     */
    @Column(name = "requesting_doctor_id")
    private Long requestingDoctorId;

    /**
     * 申请医生姓名
     */
    @Column(name = "requesting_doctor_name")
    private String requestingDoctorName;

    /**
     * 会诊医生ID
     */
    @Column(name = "consulting_doctor_id")
    private Long consultingDoctorId;

    /**
     * 会诊医生姓名
     */
    @Column(name = "consulting_doctor_name")
    private String consultingDoctorName;

    /**
     * 会诊原因
     */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /**
     * 会诊意见
     */
    @Column(columnDefinition = "TEXT")
    private String opinion;

    /**
     * 状态：PENDING, ACCEPTED, REJECTED, COMPLETED
     */
    private String status;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 申请时间
     */
    @Column(name = "request_time")
    private LocalDateTime requestTime;

    /**
     * 接受时间
     */
    @Column(name = "accept_time")
    private LocalDateTime acceptTime;

    /**
     * 完成时间
     */
    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (requestTime == null) {
            requestTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
