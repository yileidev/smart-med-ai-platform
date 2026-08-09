-- 创建数据库
CREATE DATABASE IF NOT EXISTS medical_web CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE medical_web;

-- HL7消息映射表
CREATE TABLE IF NOT EXISTS hl7_message_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(50) NOT NULL UNIQUE,
    message_type VARCHAR(20) NOT NULL,
    patient_id BIGINT,
    triage_record_id BIGINT,
    diagnosis_result_id BIGINT,
    raw_message TEXT,
    parsed_data JSON,
    sending_facility VARCHAR(100),
    receiving_facility VARCHAR(100),
    status ENUM('PENDING', 'PROCESSED', 'ERROR') DEFAULT 'PENDING',
    processed_time TIMESTAMP NULL,
    error_message TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_message_id (message_id),
    INDEX idx_message_type (message_type),
    INDEX idx_patient_id (patient_id),
    INDEX idx_triage_record_id (triage_record_id),
    INDEX idx_status (status),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 向量知识库映射表
CREATE TABLE IF NOT EXISTS vector_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_id VARCHAR(100) NOT NULL UNIQUE,
    symptoms TEXT NOT NULL,
    department VARCHAR(100) NOT NULL,
    equipments TEXT,
    description TEXT,
    category VARCHAR(50),
    chroma_vector_id VARCHAR(100),
    vector_dimension INT DEFAULT 384,
    usage_count INT DEFAULT 0,
    accuracy_score DECIMAL(3,2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT TRUE,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    INDEX idx_knowledge_id (knowledge_id),
    INDEX idx_category (category),
    INDEX idx_department (department),
    INDEX idx_usage_count (usage_count),
    INDEX idx_accuracy_score (accuracy_score),
    INDEX idx_is_active (is_active),
    FULLTEXT idx_symptoms (symptoms),
    FULLTEXT idx_equipments (equipments)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    phone_number VARCHAR(20),
    role ENUM('ADMIN', 'DOCTOR', 'NURSE', 'USER') DEFAULT 'USER',
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 医疗资源表
CREATE TABLE IF NOT EXISTS medical_resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    type ENUM('EQUIPMENT', 'MEDICINE', 'ROOM', 'BED', 'VEHICLE') NOT NULL,
    status ENUM('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'OUT_OF_ORDER') DEFAULT 'AVAILABLE',
    location VARCHAR(200),
    total_quantity INT DEFAULT 1,
    available_quantity INT DEFAULT 1,
    unit_price DECIMAL(10,2) DEFAULT 0.00,
    maintenance_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 系统日志表
CREATE TABLE IF NOT EXISTS system_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    user_name VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(50),
    details TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    level ENUM('DEBUG', 'INFO', 'WARN', 'ERROR') DEFAULT 'INFO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_level (level),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 患者信息表
CREATE TABLE IF NOT EXISTS patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_name VARCHAR(100) NOT NULL,
    id_number VARCHAR(50),
    phone_number VARCHAR(20),
    gender ENUM('MALE', 'FEMALE', 'OTHER') DEFAULT 'OTHER',
    age INT,
    address TEXT,
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(20),
    medical_history TEXT,
    allergies TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_patient_name (patient_name),
    INDEX idx_id_number (id_number),
    INDEX idx_phone (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 分诊记录表
CREATE TABLE IF NOT EXISTS triage_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    arrival_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    chief_complaint TEXT,
    vital_signs JSON,
    triage_level INT DEFAULT 4 COMMENT '1=急危重症,2=急症,3=次急症,4=非急症',
    triage_score DECIMAL(3,2) DEFAULT 0.00,
    assigned_department VARCHAR(100),
    assigned_doctor_id BIGINT,
    assigned_nurse_id BIGINT,
    ai_diagnosis TEXT,
    ai_confidence DECIMAL(3,2) DEFAULT 0.00,
    status ENUM('WAITING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'WAITING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_patient_id (patient_id),
    INDEX idx_triage_level (triage_level),
    INDEX idx_status (status),
    INDEX idx_assigned_doctor (assigned_doctor_id),
    INDEX idx_assigned_nurse (assigned_nurse_id),
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_doctor_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_nurse_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 诊断记录表
CREATE TABLE IF NOT EXISTS diagnosis_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    triage_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    diagnosis TEXT NOT NULL,
    treatment_plan TEXT,
    prescription TEXT,
    follow_up_instructions TEXT,
    diagnosis_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('DRAFT', 'COMPLETED', 'REVIEWED') DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_triage_id (triage_id),
    INDEX idx_doctor_id (doctor_id),
    INDEX idx_status (status),
    FOREIGN KEY (triage_id) REFERENCES triage_records(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 边缘设备数据表
CREATE TABLE IF NOT EXISTS edge_device_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    patient_id BIGINT,
    data_type VARCHAR(50) NOT NULL,
    sensor_data JSON,
    processed_data JSON,
    confidence_score DECIMAL(5,4) DEFAULT 0.0000,
    anomaly_detected BOOLEAN DEFAULT FALSE,
    location VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_patient_id (patient_id),
    INDEX idx_data_type (data_type),
    INDEX idx_timestamp (timestamp),
    INDEX idx_anomaly (anomaly_detected),
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入默认用户 (密码都是对应角色名+123，已加密)
INSERT INTO users (username, password, email, full_name, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'admin@medical.com', '系统管理员', 'ADMIN', 'ACTIVE'),
('doctor', '$2a$10$8K1p/wgDKRoYBk3USWS2g.KTVl2jbO2CEVpkU8RJ.O3F9JXJJpWKG', 'doctor@medical.com', '张医生', 'DOCTOR', 'ACTIVE'),
('nurse', '$2a$10$YQJ2HEuxfudKGD5lE2gdyeGlq9EhQjJ5gvokJ0VEf6RJ9Jd1hOa9G', 'nurse@medical.com', '李护士', 'NURSE', 'ACTIVE')
ON DUPLICATE KEY UPDATE username=username;

-- 插入示例医疗资源
INSERT INTO medical_resources (name, description, type, status, location, total_quantity, available_quantity, unit_price) VALUES
('X光机', '数字化X光检查设备', 'EQUIPMENT', 'AVAILABLE', '检查科1楼', 2, 2, 50.00),
('CT扫描仪', '64排螺旋CT扫描仪', 'EQUIPMENT', 'AVAILABLE', '检查科2楼', 1, 1, 200.00),
('阿司匹林', '解热镇痛药', 'MEDICINE', 'AVAILABLE', '药房', 1000, 950, 0.50),
('手术室1', '标准手术室', 'ROOM', 'AVAILABLE', '手术科3楼', 1, 1, 500.00),
('病床001', '标准病床', 'BED', 'AVAILABLE', '内科病房', 1, 1, 100.00),
('救护车001', '标准救护车', 'VEHICLE', 'AVAILABLE', '停车场', 1, 1, 200.00)
ON DUPLICATE KEY UPDATE name=name;

-- 插入示例患者数据
INSERT INTO patients (patient_name, id_number, phone_number, gender, age, address, emergency_contact, emergency_phone, medical_history, allergies) VALUES
('李某某', '110101199001011234', '13800138001', 'MALE', 35, '北京市朝阳区', '王某某', '13800138002', '高血压病史', '青霉素过敏'),
('王某某', '110101198502021234', '13800138003', 'FEMALE', 39, '北京市海淀区', '李某某', '13800138004', '糖尿病病史', '无已知过敏'),
('张某某', '110101197503031234', '13800138005', 'MALE', 49, '北京市西城区', '赵某某', '13800138006', '心脏病病史', '磺胺类药物过敏')
ON DUPLICATE KEY UPDATE patient_name=patient_name;

-- 插入示例分诊记录
INSERT INTO triage_records (patient_id, chief_complaint, vital_signs, triage_level, triage_score, assigned_department, ai_diagnosis, ai_confidence, status) VALUES
(1, '胸痛伴心率异常', '{"temperature": 37.2, "heartRate": 120, "bloodPressure": "140/90", "bloodOxygen": 95, "respiratoryRate": 22}', 1, 0.92, '心内科', '疑似急性心肌梗死，建议立即心电图检查和心肌酶谱检测', 0.92, 'WAITING'),
(2, '高热伴意识模糊', '{"temperature": 39.5, "heartRate": 110, "bloodPressure": "130/85", "bloodOxygen": 98, "respiratoryRate": 20}', 2, 0.85, '急诊科', '高热症状明显，需要排除感染性疾病，建议血常规和生化检查', 0.85, 'WAITING'),
(3, '腹痛伴恶心呕吐', '{"temperature": 36.8, "heartRate": 88, "bloodPressure": "125/80", "bloodOxygen": 97, "respiratoryRate": 18}', 3, 0.75, '消化科', '急性胃肠炎可能性较大，建议腹部超声检查', 0.75, 'WAITING')
ON DUPLICATE KEY UPDATE patient_id=patient_id;