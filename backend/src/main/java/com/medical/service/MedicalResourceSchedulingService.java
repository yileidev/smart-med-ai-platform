package com.medical.service;

import com.medical.entity.*;
import com.medical.repository.ResourceAllocationRepository;
import com.medical.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 医疗资源调度服务 - 云端智能资源配置模块
 * 
 * 【技术架构】Drools规则引擎 + 动态调度算法
 * 【核心功能】基于"分诊等级 + 科室负荷 + 病情风险"的多维度资源调度
 * 
 * 【规则引擎优势】
 * 1. 可配置性：医疗规则可灵活调整，无需修改代码
 * 2. 可扩展性：支持复杂的多条件判断和优先级设置
 * 3. 可维护性：规则与代码分离，便于医疗专家参与维护
 * 4. 实时性：规则引擎快速执行，满足急诊场景需求
 * 
 * 【调度策略】
 * 1. 分诊等级优先：I/II级患者优先分配资源
 * 2. 科室负荷均衡：避免某科室过载
 * 3. 医生能力匹配：危重症分配高年资医生
 * 4. 设备资源考虑：确保必要设备可用
 * 5. 床位动态分配：抢救室、急诊床位、观察床位分级管理
 * 
 * 【Drools规则示例】
 * - 规则1：分诊等级≤2 -> 立即分配抢救室 + 高年资医生
 * - 规则2：胸痛患者 -> 心内科 + 心电监护设备
 * - 规则3：科室患者数>5 -> 转移至负荷低的科室
 * 
 * 【系统集成】
 * - 输入：TriageRecord(分诊记录) + DiagnosisResult(诊断结果)
 * - 处理：Drools规则引擎计算
 * - 输出：ResourceAllocation(资源分配方案)
 *   - 分配科室
 *   - 分配医生
 *   - 分配床位
 *   - 预计等待时间
 *   - 优先级分数
 * 
 * 【实践意义】
 * 解决基层急诊"危急患者等待设备""普通患者占用紧急资源"的错配问题
 * 
 * @see DroolsConfig Drools规则引擎配置
 * @see ResourceAllocation 资源分配实体
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalResourceSchedulingService {

    private final KieSession kieSession;
    private final ResourceAllocationRepository resourceAllocationRepository;
    private final UserRepository userRepository;

    /**
     * 执行医疗资源调度
     */
    @Transactional
    public ResourceAllocation scheduleResources(TriageRecord triageRecord, DiagnosisResult diagnosisResult) {
        try {
            log.info("开始医疗资源调度，患者ID: {}, 分诊等级: {}", 
                triageRecord.getPatient().getId(), triageRecord.getTriageLevel());

            // 1. 创建资源分配对象
            ResourceAllocation allocation = new ResourceAllocation();
            allocation.setTriageRecord(triageRecord);
            allocation.setDiagnosisResult(diagnosisResult);
            allocation.setAllocatedDepartment("急诊科"); // 默认急诊科
            allocation.setPriorityScore(50); // 默认优先级
            allocation.setEstimatedWaitTime(60); // 默认等待时间
            allocation.setAllocationReason("基于分诊等级和AI诊断的资源分配");
            allocation.setSpecialRequirements("");

            // 2. 获取可用资源
            List<User> availableDoctors = getAvailableDoctors();
            List<String> availableBeds = getAvailableBeds();
            List<String> availableEquipment = getAvailableEquipment();

            // 3. 使用规则引擎（如果可用）
            if (kieSession != null) {
                // 设置全局变量
                kieSession.setGlobal("availableDoctors", availableDoctors);
                kieSession.setGlobal("availableBeds", availableBeds);
                kieSession.setGlobal("availableEquipment", availableEquipment);

                // 插入事实到规则引擎
                kieSession.insert(triageRecord);
                kieSession.insert(diagnosisResult);
                kieSession.insert(allocation);

                // 触发规则执行
                int rulesExecuted = kieSession.fireAllRules();
                log.info("规则引擎执行完成，触发了 {} 条规则", rulesExecuted);
            } else {
                log.warn("⚠️ Drools规则引擎未初始化，使用简单分配策略");
                // 使用简单的分配逻辑作为降级方案
                applySimpleAllocationStrategy(allocation, triageRecord, diagnosisResult);
            }

            // 6. 分配最合适的医生
            assignBestDoctor(allocation, availableDoctors);

            // 7. 保存资源分配结果
            ResourceAllocation saved = resourceAllocationRepository.save(allocation);

            log.info("医疗资源调度完成，分配ID: {}, 科室: {}, 优先级: {}, 等待时间: {}分钟", 
                saved.getId(), saved.getAllocatedDepartment(), 
                saved.getPriorityScore(), saved.getEstimatedWaitTime());

            return saved;

        } catch (Exception e) {
            log.error("医疗资源调度失败", e);
            
            // 创建默认分配
            ResourceAllocation defaultAllocation = new ResourceAllocation();
            defaultAllocation.setTriageRecord(triageRecord);
            defaultAllocation.setDiagnosisResult(diagnosisResult);
            defaultAllocation.setAllocatedDepartment("急诊科");
            defaultAllocation.setPriorityScore(50);
            defaultAllocation.setEstimatedWaitTime(60);
            defaultAllocation.setAllocationReason("系统错误，使用默认分配");
            defaultAllocation.setStatus(ResourceAllocation.AllocationStatus.PENDING);
            
            return resourceAllocationRepository.save(defaultAllocation);
        } finally {
            // 清理规则引擎会话
            if (kieSession != null) {
                kieSession.dispose();
            }
        }
    }

    /**
     * 简单分配策略（当规则引擎不可用时使用）
     */
    private void applySimpleAllocationStrategy(ResourceAllocation allocation, 
                                              TriageRecord triageRecord, 
                                              DiagnosisResult diagnosisResult) {
        // 根据分诊等级调整优先级
        int triageLevel = triageRecord.getTriageLevel();
        if (triageLevel <= 2) {
            allocation.setPriorityScore(90);
            allocation.setEstimatedWaitTime(5);
            allocation.setAllocatedBed("抢救室-01");
        } else if (triageLevel == 3) {
            allocation.setPriorityScore(70);
            allocation.setEstimatedWaitTime(30);
            allocation.setAllocatedBed("急诊床位-A01");
        } else {
            allocation.setPriorityScore(50);
            allocation.setEstimatedWaitTime(60);
            allocation.setAllocatedBed("观察床位-C01");
        }
        
        // 根据症状设置科室
        String symptoms = triageRecord.getSymptoms().toLowerCase();
        if (symptoms.contains("胸痛") || symptoms.contains("心脏")) {
            allocation.setAllocatedDepartment("心内科");
        } else if (symptoms.contains("头痛") || symptoms.contains("晕") || symptoms.contains("神经")) {
            allocation.setAllocatedDepartment("神经科");
        } else {
            allocation.setAllocatedDepartment("急诊科");
        }
    }

    /**
     * 获取可用医生列表
     */
    private List<User> getAvailableDoctors() {
        return userRepository.findByRole(User.Role.DOCTOR)
            .stream()
            .filter(doctor -> isDocterAvailable(doctor))
            .collect(Collectors.toList());
    }

    /**
     * 检查医生是否可用
     */
    private boolean isDocterAvailable(User doctor) {
        // 检查医生当前的工作负载
        long activePatients = resourceAllocationRepository.countActiveByDoctor(doctor.getId());
        return activePatients < 5; // 假设每个医生最多同时处理5个病人
    }

    /**
     * 获取可用床位列表
     */
    private List<String> getAvailableBeds() {
        return List.of(
            "抢救室-01", "抢救室-02", "抢救室-03",
            "急诊床位-A01", "急诊床位-A02", "急诊床位-A03",
            "急诊床位-B01", "急诊床位-B02", "急诊床位-B03",
            "观察床位-C01", "观察床位-C02", "观察床位-C03"
        );
    }

    /**
     * 获取可用设备列表
     */
    private List<String> getAvailableEquipment() {
        return List.of(
            "心电监护仪", "呼吸机", "除颤仪", "氧气", 
            "心电图机", "雾化器", "输液泵", "血压计"
        );
    }

    /**
     * 分配最合适的医生
     */
    private void assignBestDoctor(ResourceAllocation allocation, List<User> availableDoctors) {
        if (availableDoctors.isEmpty()) {
            allocation.setAllocationReason(allocation.getAllocationReason() + ";当前无可用医生");
            return;
        }

        User bestDoctor = null;
        
        // 根据分诊等级和诊断结果选择最合适的医生
        if (allocation.getTriageRecord().getTriageLevel() <= 2) {
            // 危重患者分配高年资医生
            bestDoctor = availableDoctors.stream()
                .filter(doctor -> doctor.getUsername().contains("主任") || 
                               doctor.getUsername().contains("副主任"))
                .findFirst()
                .orElse(availableDoctors.get(0));
        } else {
            // 普通患者分配负载最轻的医生
            bestDoctor = availableDoctors.stream()
                .min((d1, d2) -> {
                    long load1 = resourceAllocationRepository.countActiveByDoctor(d1.getId());
                    long load2 = resourceAllocationRepository.countActiveByDoctor(d2.getId());
                    return Long.compare(load1, load2);
                })
                .orElse(availableDoctors.get(0));
        }

        allocation.setAssignedDoctor(bestDoctor);
        allocation.setAllocationReason(allocation.getAllocationReason() + 
            String.format(";分配医生：%s", bestDoctor.getUsername()));
    }

    /**
     * 获取当前资源使用情况
     */
    public ResourceUsageStats getResourceUsageStats() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        
        List<ResourceAllocation> activeAllocations = resourceAllocationRepository
            .findActiveAllocations(today);

        ResourceUsageStats stats = new ResourceUsageStats();
        stats.setTotalActiveAllocations(activeAllocations.size());
        stats.setAverageWaitTime(resourceAllocationRepository.getAverageWaitTime(today));
        
        // 按科室统计
        stats.setEmergencyDeptLoad(resourceAllocationRepository.countActiveBeDepartment("急诊科"));
        stats.setCardiologyDeptLoad(resourceAllocationRepository.countActiveBeDepartment("心内科"));
        stats.setNeurologyDeptLoad(resourceAllocationRepository.countActiveBeDepartment("神经科"));
        
        return stats;
    }

    /**
     * 资源使用统计类
     */
    public static class ResourceUsageStats {
        private int totalActiveAllocations;
        private Double averageWaitTime;
        private long emergencyDeptLoad;
        private long cardiologyDeptLoad;
        private long neurologyDeptLoad;

        // Getters and Setters
        public int getTotalActiveAllocations() { return totalActiveAllocations; }
        public void setTotalActiveAllocations(int totalActiveAllocations) { this.totalActiveAllocations = totalActiveAllocations; }
        
        public Double getAverageWaitTime() { return averageWaitTime; }
        public void setAverageWaitTime(Double averageWaitTime) { this.averageWaitTime = averageWaitTime; }
        
        public long getEmergencyDeptLoad() { return emergencyDeptLoad; }
        public void setEmergencyDeptLoad(long emergencyDeptLoad) { this.emergencyDeptLoad = emergencyDeptLoad; }
        
        public long getCardiologyDeptLoad() { return cardiologyDeptLoad; }
        public void setCardiologyDeptLoad(long cardiologyDeptLoad) { this.cardiologyDeptLoad = cardiologyDeptLoad; }
        
        public long getNeurologyDeptLoad() { return neurologyDeptLoad; }
        public void setNeurologyDeptLoad(long neurologyDeptLoad) { this.neurologyDeptLoad = neurologyDeptLoad; }
    }
}