package com.medical.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统通知实体
 */
@Data
@Entity
@Table(name = "notification")
public class      Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 接收用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 接收用户名
     */
    private String username;

    /**
     * 通知类型：SYSTEM, PATIENT, CONSULTATION, ALERT
     */
    private String type;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 关联ID（如患者ID、会诊ID等）
     */
    @Column(name = "related_id")
    private Long relatedId;

    /**
     * 是否已读
     */
    @Column(name = "is_read")
    private Boolean isRead = false;

    /**
     * 优先级：LOW, NORMAL, HIGH, URGENT
     */
    private String priority;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 读取时间
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
    }
}
