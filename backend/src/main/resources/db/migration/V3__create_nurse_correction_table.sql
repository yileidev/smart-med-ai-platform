-- 创建护士修正记录表
CREATE TABLE IF NOT EXISTS nurse_correction_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    edge_data_id BIGINT NOT NULL COMMENT '原始边缘数据ID',
    nurse_id BIGINT NOT NULL COMMENT '护士ID',
    nurse_name VARCHAR(50) NOT NULL COMMENT '护士姓名',
    corrected_sensor_data TEXT COMMENT '修正后的生命体征数据(JSON)',
    corrected_chief_complaint TEXT COMMENT '修正后的主诉',
    nurse_notes TEXT COMMENT '护士备注',
    correction_time DATETIME NOT NULL COMMENT '修正时间',
    status VARCHAR(20) DEFAULT 'SENT_TO_EDGE' COMMENT '状态: SENT_TO_EDGE, REASSESSED, COMPLETED',
    final_triage_level INT COMMENT '最终分诊等级（重新分诊后）',
    final_received_time DATETIME COMMENT '收到最终结果时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_edge_data (edge_data_id),
    INDEX idx_nurse (nurse_id),
    INDEX idx_status (status),
    INDEX idx_correction_time (correction_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护士修正记录表';
