package com.medical.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@Entity
@Table(name = "operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 操作类型
     */
    @Column(name = "operation_type")
    private String operationType;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 操作IP
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求参数
     */
    @Column(columnDefinition = "TEXT")
    private String params;

    /**
     * 返回结果
     */
    @Column(columnDefinition = "TEXT")
    private String result;

    /**
     * 操作状态：SUCCESS, FAIL
     */
    private String status;

    /**
     * 错误信息
     */
    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    /**
     * 操作时间
     */
    @Column(name = "operation_time")
    private LocalDateTime operationTime;

    /**
     * 执行时长（毫秒）
     */
    private Long duration;

    @PrePersist
    protected void onCreate() {
        if (operationTime == null) {
            operationTime = LocalDateTime.now();
        }
    }
}
