-- 医疗急诊分诊系统 MySQL数据库初始化脚本
-- 包含加密字段支持

-- 创建数据库
CREATE DATABASE IF NOT EXISTS medical_web 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE medical_web;

-- 患者表（支持加密字段）
CREATE TABLE IF NOT EXISTS patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_name VARCHAR(100) NOT NULL COMMENT '患者姓名',
    id_number VARCHAR(500) COMMENT '身份证号（加密存储）',
    id_card VARCHAR(500) COMMENT '身份证号（加密存储）',
    phone_number VARCHAR(500) COMMENT '手机号（加密存储）',
    gender VARCHAR(20) DEFAULT 'OTHER' COMMENT '性别',
    age INT COMMENT '年龄',
    date_of_birth DATE COMMENT '出生日期',
    address TEXT COMMENT '地址',
    emergency_contact VARCHAR(100) COMMENT '紧急联系人',
    emergency_phone VARCHAR(200) COMMENT '紧急联系电话',
    medical_history TEXT COMMENT '病史',
    allergies TEXT COMMENT '过敏史',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_patient_name (patient_name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者信息表';

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    full_name VARCHAR(100) NOT NULL COMMENT '姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone_number VARCHAR(20) COMMENT '手机号',
    role VARCHAR(20) NOT NULL COMMENT '角色：ADMIN/DOCTOR/NURSE',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    last_login_at DATETIME COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 分诊记录表
CREATE TABLE IF NOT EXISTS triage_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT COMMENT '患者ID',
    chief_complaint TEXT COMMENT '主诉',
    vital_signs TEXT COMMENT '生命体征（JSON）',
    triage_level INT COMMENT '分诊等级 1-5',
    triage_color VARCHAR(20) COMMENT '分诊颜色',
    ai_diagnosis TEXT COMMENT 'AI诊断结果',
    ai_confidence DOUBLE COMMENT 'AI置信度',
    nurse_notes TEXT COMMENT '护士备注',
    assigned_nurse_id BIGINT COMMENT '分诊护士ID',
    assigned_doctor_id BIGINT COMMENT '分配医生ID',
    assigned_department VARCHAR(50) COMMENT '分配科室',
    status VARCHAR(20) DEFAULT 'WAITING' COMMENT '状态：WAITING/CONFIRMED/IN_TREATMENT/COMPLETED',
    arrival_time DATETIME COMMENT '到达时间',
    triage_time DATETIME COMMENT '分诊时间',
    confirmed_time DATETIME COMMENT '确认时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_nurse_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_doctor_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_patient_id (patient_id),
    INDEX idx_status (status),
    INDEX idx_triage_level (triage_level),
    INDEX idx_arrival_time (arrival_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分诊记录表';

-- 边缘设备数据表
CREATE TABLE IF NOT EXISTS edge_device_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL COMMENT '设备ID',
    patient_temp_id VARCHAR(100) COMMENT '临时患者ID',
    patient_name VARCHAR(100) COMMENT '患者姓名',
    patient_id_card VARCHAR(500) COMMENT '身份证号（加密）',
    patient_phone VARCHAR(500) COMMENT '手机号（加密）',
    patient_age INT COMMENT '年龄',
    patient_gender VARCHAR(20) COMMENT '性别',
    
    -- 生命体征
    temperature DOUBLE COMMENT '体温',
    heart_rate INT COMMENT '心率',
    blood_oxygen INT COMMENT '血氧',
    respiratory_rate INT COMMENT '呼吸频率',
    systolic_bp INT COMMENT '收缩压',
    diastolic_bp INT COMMENT '舒张压',
    consciousness VARCHAR(50) COMMENT '意识状态',
    
    -- AI分析结果
    symptom_text TEXT COMMENT '症状描述',
    voice_text TEXT COMMENT '语音转文字',
    voice_confidence DOUBLE COMMENT '语音识别置信度',
    ai_diagnosis TEXT COMMENT 'AI诊断',
    ai_confidence DOUBLE COMMENT 'AI置信度',
    triage_level INT COMMENT '分诊等级',
    triage_color VARCHAR(20) COMMENT '分诊颜色',
    triage_score DOUBLE COMMENT '分诊评分',
    
    -- 设备状态
    device_status VARCHAR(50) DEFAULT 'ONLINE' COMMENT '设备状态',
    processing_status VARCHAR(50) COMMENT '处理状态',
    processed BOOLEAN DEFAULT FALSE COMMENT '是否已处理',
    error_message TEXT COMMENT '错误信息',
    
    -- 时间戳
    timestamp DATETIME COMMENT '数据时间戳',
    received_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_device_id (device_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_processed (processed),
    INDEX idx_received_time (received_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='边缘设备数据表';

-- 系统日志表
CREATE TABLE IF NOT EXISTS system_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    action VARCHAR(100) NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(50) COMMENT '资源类型',
    resource_id VARCHAR(100) COMMENT '资源ID',
    description TEXT COMMENT '操作描述',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent TEXT COMMENT '浏览器信息',
    status VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAIL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

-- 插入默认管理员账号
INSERT INTO users (username, password, full_name, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', '系统管理员', 'ADMIN', 'ACTIVE'),
('doctor', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', '医生', 'DOCTOR', 'ACTIVE'),
('nurse', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', '护士', 'NURSE', 'ACTIVE')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 创建数据库索引优化查询
CREATE INDEX idx_triage_status_level ON triage_records(status, triage_level);
CREATE INDEX idx_edge_device_processed ON edge_device_data(device_id, processed);
