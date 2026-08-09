package com.medical.config;

import com.medical.entity.*;
import com.medical.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 基础数据初始化器
 * 仅初始化用户账号和医疗资源，患者数据由边缘设备实时采集上传
 */
@Configuration
@SuppressWarnings("unused")
public class TestDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    private final UserRepository userRepository;
    private final MedicalResourceRepository medicalResourceRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public TestDataInitializer(UserRepository userRepository,
                               MedicalResourceRepository medicalResourceRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.medicalResourceRepository = medicalResourceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("🚀 开始初始化基础数据...");
            initUsers();
            initMedicalResources();
            log.info("✅ 基础数据初始化完成！");
        } else {
            log.info("⚠️ 数据库已有数据，跳过初始化");
        }
    }

    /**
     * 初始化用户
     */
    private void initUsers() {
        // 管理员
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@medical.com");
        admin.setFullName("系统管理员");
        admin.setPhoneNumber("13800138000");
        admin.setRole(User.Role.ADMIN);
        admin.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(admin);

        // 医生
        User doctor = new User();
        doctor.setUsername("doctor");
        doctor.setPassword(passwordEncoder.encode("doctor123"));
        doctor.setEmail("doctor@medical.com");
        doctor.setFullName("张医生");
        doctor.setPhoneNumber("13800138001");
        doctor.setRole(User.Role.DOCTOR);
        doctor.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(doctor);

        // 护士
        User nurse = new User();
        nurse.setUsername("nurse");
        nurse.setPassword(passwordEncoder.encode("nurse123"));
        nurse.setEmail("nurse@medical.com");
        nurse.setFullName("李护士");
        nurse.setPhoneNumber("13800138002");
        nurse.setRole(User.Role.NURSE);
        nurse.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(nurse);

        log.info("✅ 已创建3个测试用户: admin, doctor, nurse");
    }

    // 已移除测试患者和分诊记录初始化方法
    // 患者数据由边缘设备采集后实时上传

    /**
     * 初始化医疗资源
     */
    private void initMedicalResources() {
        createResource("心电监护仪", "用于监测患者心电图", MedicalResource.ResourceType.EQUIPMENT, "急诊室1", 5, 5);
        createResource("除颤仪", "心脏除颤设备", MedicalResource.ResourceType.EQUIPMENT, "急诊室2", 3, 3);
        createResource("呼吸机", "辅助呼吸设备", MedicalResource.ResourceType.EQUIPMENT, "ICU", 10, 7);
        createResource("急诊病床", "急诊科病床", MedicalResource.ResourceType.BED, "急诊科", 20, 15);
        createResource("救护车", "急救转运车辆", MedicalResource.ResourceType.VEHICLE, "停车场", 5, 4);
        
        log.info("✅ 已创建5种医疗资源");
    }

    private void createResource(String name, String desc, MedicalResource.ResourceType type, 
                                String location, int total, int available) {
        MedicalResource resource = new MedicalResource();
        resource.setName(name);
        resource.setDescription(desc);
        resource.setType(type);
        resource.setStatus(MedicalResource.ResourceStatus.AVAILABLE);
        resource.setLocation(location);
        resource.setTotalQuantity(total);
        resource.setAvailableQuantity(available);
        medicalResourceRepository.save(resource);
    }
}
