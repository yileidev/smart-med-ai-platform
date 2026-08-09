package com.medical.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 资源分配实体 - 用于Drools规则引擎和数据库持久化
 */
@Data
@Entity
@Table(name = "resource_allocations")
public class ResourceAllocation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "triage_record_id")
    private TriageRecord triageRecord;
    
    @ManyToOne
    @JoinColumn(name = "assigned_doctor_id")
    private User assignedDoctor;
    
    @ManyToOne
    @JoinColumn(name = "diagnosis_result_id")
    private DiagnosisResult diagnosisResult;
    
    @Column(name = "allocated_department")
    private String allocatedDepartment;
    
    @Column(name = "allocated_bed")
    private String allocatedBed;
    
    @Column(name = "allocated_resources", columnDefinition = "TEXT")
    private String allocatedResources;
    
    @Column(name = "priority_score")
    private Integer priorityScore;
    
    @Column(name = "estimated_wait_time")
    private Integer estimatedWaitTime;
    
    @Column(name = "allocation_reason", columnDefinition = "TEXT")
    private String allocationReason;
    
    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AllocationStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public ResourceAllocation() {
        this.priorityScore = 0;
        this.estimatedWaitTime = 60;
        this.status = AllocationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 资源分配状态枚举
     */
    public enum AllocationStatus {
        PENDING,      // 待分配
        ALLOCATED,    // 已分配
        IN_PROGRESS,  // 处理中
        COMPLETED,    // 已完成
        CANCELLED     // 已取消
    }
}
