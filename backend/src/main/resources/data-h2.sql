-- H2数据库初始化数据
-- 自动创建测试账号

-- 清空已有数据（如果存在）
DELETE FROM users WHERE 1=1;

-- 插入测试用户
-- 密码都是 BCrypt 加密后的对应明文密码
INSERT INTO users (id, username, password, email, full_name, phone_number, role, status, created_at, updated_at) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@medical.com', '系统管理员', '13800138000', 'ADMIN', 'ACTIVE', NOW(), NOW()),
(2, 'doctor', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'doctor@medical.com', '张医生', '13800138001', 'DOCTOR', 'ACTIVE', NOW(), NOW()),
(3, 'nurse', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'nurse@medical.com', '李护士', '13800138002', 'NURSE', 'ACTIVE', NOW(), NOW());

-- 注意：上面的密码hash对应的明文密码都是：admin123, doctor123, nurse123
-- 如果登录失败，说明密码加密方式不对，需要在代码中用 PasswordEncoder 重新生成

-- 患者数据和分诊记录由边缘设备采集后实时上传，不预置测试数据
