package com.medical.service;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Collections;

/**
 * 基于Drools的医疗资源智能调度服务
 */
@Slf4j
@Service
@SuppressWarnings("unused")
public class ResourceSchedulingService {

    private static final Logger log = LoggerFactory.getLogger(ResourceSchedulingService.class);
    private KieContainer kieContainer;
    private boolean droolsAvailable = false;

    public ResourceSchedulingService() {
        try {
            this.kieContainer = initializeRulesEngine();
            this.droolsAvailable = true;
            log.info("规则引擎初始化成功");
        } catch (Exception e) {
            log.warn("规则引擎初始化失败，使用简化调度逻辑: {}", e.getMessage());
            this.droolsAvailable = false;
        }
    }

    /**
     * 初始化规则引擎
     */
    private KieContainer initializeRulesEngine() {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        
        // 添加资源调度规则文件
        String rulesContent = buildResourceSchedulingRules();
        kieFileSystem.write("src/main/resources/rules/resource-scheduling.drl", rulesContent);
        
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        
        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

    /**
     * 智能资源调度
     */
    public SchedulingResult scheduleResources(SchedulingRequest request) {
        // 如果Drools不可用，使用简化调度逻辑
        if (!droolsAvailable || kieContainer == null) {
            return simplifiedSchedule(request);
        }
        
        try {
            KieSession kieSession = kieContainer.newKieSession();
            
            try {
                SchedulingResult result = new SchedulingResult();
                
                // 设置调度上下文
                kieSession.setGlobal("result", result);
                kieSession.setGlobal("logger", log);
                
                // 插入调度请求事实
                kieSession.insert(request);
                kieSession.insert(getCurrentResourceStatus());
                
                // 执行规则
                int rulesFired = kieSession.fireAllRules();
                log.info("执行了 {} 条资源调度规则", rulesFired);
                
                return result;
                
            } finally {
                kieSession.dispose();
            }
        } catch (Exception e) {
            log.warn("规则引擎执行失败，使用简化调度: {}", e.getMessage());
            return simplifiedSchedule(request);
        }
    }
    
    /**
     * 简化调度逻辑（当Drools不可用时使用）
     */
    private SchedulingResult simplifiedSchedule(SchedulingRequest request) {
        SchedulingResult result = new SchedulingResult();
        ResourceStatus status = getCurrentResourceStatus();
        
        int triageLevel = request.getTriageLevel();
        String department = request.getRecommendedDepartment();
        if (department == null || department.isEmpty()) {
            department = "急诊科";
        }
        
        result.setDepartment(department);
        
        switch (triageLevel) {
            case 1: // 濒危
                result.setBedNumber(status.getBestAvailableBed(department));
                result.setDoctorId(status.getTopDoctorId(department));
                result.setEquipment(status.getCriticalEquipment(department));
                result.setPriority("最高");
                result.setEstimatedWaitTime(0);
                break;
            case 2: // 危急
                result.setBedNumber(status.getUrgentBed(department));
                result.setDoctorId(status.getAvailableDoctorId(department));
                result.setEquipment(status.getUrgentEquipment(department));
                result.setPriority("高");
                result.setEstimatedWaitTime(5);
                break;
            case 3: // 急症
                result.setBedNumber(status.getStandardBed(department));
                result.setDoctorId(status.getAvailableDoctorId(department));
                result.setEquipment(status.getStandardEquipment(department));
                result.setPriority("中");
                result.setEstimatedWaitTime(15);
                break;
            case 4: // 次急症
                result.setBedNumber(status.getStandardBed(department));
                result.setDoctorId(status.getAvailableDoctorId(department));
                result.setPriority("低");
                result.setEstimatedWaitTime(30);
                break;
            default: // 非急症
                result.setBedNumber(status.getStandardBed(department));
                result.setDoctorId(status.getAvailableDoctorId(department));
                result.setPriority("标准");
                result.setEstimatedWaitTime(60);
                break;
        }
        
        log.info("使用简化调度逻辑完成资源分配");
        return result;
    }

    /**
     * 构建资源调度规则
     */
    private String buildResourceSchedulingRules() {
        return """
            package com.medical.rules;
            
            import com.medical.service.ResourceSchedulingService.SchedulingRequest;
            import com.medical.service.ResourceSchedulingService.SchedulingResult;
            import com.medical.service.ResourceSchedulingService.ResourceStatus;
            
            global SchedulingResult result;
            global org.slf4j.Logger logger;
            
            // 规则1：濒危患者优先分配最好的资源
            rule "Critical Patient Priority"
                when
                    $request: SchedulingRequest(triageLevel == 1)
                    $status: ResourceStatus()
                then
                    result.setDepartment($request.getRecommendedDepartment());
                    result.setBedNumber($status.getBestAvailableBed($request.getDepartment()));
                    result.setDoctorId($status.getTopDoctorId($request.getDepartment()));
                    result.setEquipment($status.getCriticalEquipment($request.getDepartment()));
                    result.setPriority("最高");
                    result.setEstimatedWaitTime(0);
                    logger.info("濒危患者资源调度：立即处理");
            end
            
            // 规则2：危急患者快速通道
            rule "Urgent Patient Fast Track"
                when
                    $request: SchedulingRequest(triageLevel == 2)
                    $status: ResourceStatus()
                then
                    result.setDepartment($request.getRecommendedDepartment());
                    result.setBedNumber($status.getUrgentBed($request.getDepartment()));
                    result.setDoctorId($status.getAvailableDoctorId($request.getDepartment()));
                    result.setEquipment($status.getUrgentEquipment($request.getDepartment()));
                    result.setPriority("高");
                    result.setEstimatedWaitTime(5);
                    logger.info("危急患者资源调度：5分钟内处理");
            end
            
            // 规则3：急症患者标准流程
            rule "Semi-Urgent Patient Standard"
                when
                    $request: SchedulingRequest(triageLevel == 3)
                    $status: ResourceStatus(getDepartmentLoad($request.getRecommendedDepartment()) < 80)
                then
                    result.setDepartment($request.getRecommendedDepartment());
                    result.setBedNumber($status.getStandardBed($request.getDepartment()));
                    result.setDoctorId($status.getAvailableDoctorId($request.getDepartment()));
                    result.setEquipment($status.getStandardEquipment($request.getDepartment()));
                    result.setPriority("中");
                    result.setEstimatedWaitTime(15);
                    logger.info("急症患者资源调度：15分钟内处理");
            end
            
            // 规则4：科室负荷过高时转移患者
            rule "Department Overload Redirect"
                when
                    $request: SchedulingRequest(triageLevel >= 3)
                    $status: ResourceStatus(getDepartmentLoad($request.getRecommendedDepartment()) >= 80)
                then
                    String alternateDept = $status.getAlternateDepartment($request.getRecommendedDepartment());
                    result.setDepartment(alternateDept);
                    result.setBedNumber($status.getStandardBed(alternateDept));
                    result.setDoctorId($status.getAvailableDoctorId(alternateDept));
                    result.setPriority("中");
                    result.setEstimatedWaitTime(30);
                    result.addNote("原科室负荷过高，转至" + alternateDept);
                    logger.info("科室负荷过高，患者转移至：{}", alternateDept);
            end
            
            // 规则5：夜间和节假日资源调整
            rule "Off-Hours Resource Adjustment"
                when
                    $request: SchedulingRequest()
                    $status: ResourceStatus(isOffHours() == true)
                then
                    result.setDepartment("急诊科");
                    result.setBedNumber($status.getEmergencyBed());
                    result.setDoctorId($status.getOnDutyDoctorId());
                    result.setPriority("标准");
                    result.setEstimatedWaitTime(45);
                    result.addNote("非工作时间，统一由急诊科接诊");
                    logger.info("非工作时间资源调度");
            end
            
            // 规则6：特殊设备需求处理
            rule "Special Equipment Required"
                when
                    $request: SchedulingRequest(requiredEquipment != null)
                    $status: ResourceStatus()
                then
                    String deptWithEquipment = $status.getDepartmentWithEquipment($request.getRequiredEquipment());
                    if (deptWithEquipment != null) {
                        result.setDepartment(deptWithEquipment);
                        result.setBedNumber($status.getSpecialEquipmentBed(deptWithEquipment));
                        result.setEquipment($status.getSpecialEquipment($request.getRequiredEquipment()));
                        result.addNote("根据设备需求调整科室");
                    }
                    logger.info("特殊设备需求处理：{}", $request.getRequiredEquipment());
            end
            
            // 规则7：医生专业匹配
            rule "Doctor Specialty Matching"
                when
                    $request: SchedulingRequest(symptoms != null)
                    $status: ResourceStatus()
                then
                    String specialistId = $status.getSpecialistDoctorId($request.getSymptoms());
                    if (specialistId != null && result.getDoctorId() == null) {
                        result.setDoctorId(specialistId);
                        result.addNote("匹配专科医生");
                    }
                    logger.info("医生专业匹配完成");
            end
            """;
    }

    /**
     * 获取当前资源状态
     */
    private ResourceStatus getCurrentResourceStatus() {
        // 模拟获取实时资源状态
        ResourceStatus status = new ResourceStatus();
        
        // 科室负荷情况（百分比）
        status.setDepartmentLoad("心内科", 65);
        status.setDepartmentLoad("呼吸科", 45);
        status.setDepartmentLoad("消化科", 85); // 负荷过高
        status.setDepartmentLoad("神经科", 55);
        status.setDepartmentLoad("急诊科", 40);
        
        // 可用床位
        status.addAvailableBed("心内科", "C101");
        status.addAvailableBed("心内科", "C102");
        status.addAvailableBed("呼吸科", "R201");
        status.addAvailableBed("急诊科", "E001");
        status.addAvailableBed("急诊科", "E002");
        
        // 在岗医生
        status.addAvailableDoctor("心内科", "DR001");
        status.addAvailableDoctor("呼吸科", "DR002");
        status.addAvailableDoctor("急诊科", "DR003");
        
        return status;
    }

    // 内部类定义
    public static class SchedulingRequest {
        private int triageLevel;
        private String recommendedDepartment;
        private String symptoms;
        private String requiredEquipment;
        private String patientId;
        
        // getters and setters
        public int getTriageLevel() { return triageLevel; }
        public void setTriageLevel(int triageLevel) { this.triageLevel = triageLevel; }
        public String getRecommendedDepartment() { return recommendedDepartment; }
        public void setRecommendedDepartment(String recommendedDepartment) { this.recommendedDepartment = recommendedDepartment; }
        public String getSymptoms() { return symptoms; }
        public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
        public String getRequiredEquipment() { return requiredEquipment; }
        public void setRequiredEquipment(String requiredEquipment) { this.requiredEquipment = requiredEquipment; }
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        public String getDepartment() { return recommendedDepartment; }
    }

    public static class SchedulingResult {
        private String department;
        private String bedNumber;
        private String doctorId;
        private List<String> equipment = new ArrayList<>();
        private String priority;
        private int estimatedWaitTime;
        private final List<String> notes = new ArrayList<>();
        
        // getters and setters
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getBedNumber() { return bedNumber; }
        public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }
        public String getDoctorId() { return doctorId; }
        public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
        public List<String> getEquipment() { return equipment; }
        public void setEquipment(List<String> equipment) { this.equipment = equipment; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public int getEstimatedWaitTime() { return estimatedWaitTime; }
        public void setEstimatedWaitTime(int estimatedWaitTime) { this.estimatedWaitTime = estimatedWaitTime; }
        public List<String> getNotes() { return notes; }
        public void addNote(String note) { this.notes.add(note); }
    }

    public static class ResourceStatus {
        private final Map<String, Integer> departmentLoads = new HashMap<>();
        private final Map<String, List<String>> availableBeds = new HashMap<>();
        private final Map<String, List<String>> availableDoctors = new HashMap<>();
        
        public void setDepartmentLoad(String department, int load) {
            departmentLoads.put(department, load);
        }
        
        public int getDepartmentLoad(String department) {
            return departmentLoads.getOrDefault(department, 50);
        }
        
        public void addAvailableBed(String department, String bedNumber) {
            availableBeds.computeIfAbsent(department, k -> new ArrayList<>()).add(bedNumber);
        }
        
        public void addAvailableDoctor(String department, String doctorId) {
            availableDoctors.computeIfAbsent(department, k -> new ArrayList<>()).add(doctorId);
        }
        
        public String getBestAvailableBed(String department) {
            List<String> beds = availableBeds.get(department);
            return beds != null && !beds.isEmpty() ? beds.get(0) : "ICU001";
        }
        
        public String getUrgentBed(String department) {
            return getBestAvailableBed(department);
        }
        
        public String getStandardBed(String department) {
            List<String> beds = availableBeds.get(department);
            return beds != null && !beds.isEmpty() ? beds.get(beds.size() - 1) : "GENERAL001";
        }
        
        public String getTopDoctorId(String department) {
            List<String> doctors = availableDoctors.get(department);
            return doctors != null && !doctors.isEmpty() ? doctors.get(0) : "DR001";
        }
        
        public String getAvailableDoctorId(String department) {
            return getTopDoctorId(department);
        }
        
        public List<String> getCriticalEquipment(String department) {
            return Arrays.asList("监护设备", "除颤器", "呼吸机");
        }
        
        public List<String> getUrgentEquipment(String department) {
            return Arrays.asList("监护设备", "氧气");
        }
        
        public List<String> getStandardEquipment(String department) {
            return Collections.singletonList("基础检查设备");
        }
        
        public String getAlternateDepartment(String department) {
            // 简化的科室转移逻辑，所有科室负荷过高时都转至急诊科
            return "急诊科";
        }
        
        public boolean isOffHours() {
            // 简化的非工作时间判断
            int hour = java.time.LocalTime.now().getHour();
            return hour < 8 || hour > 18;
        }
        
        public String getEmergencyBed() {
            return "EMERGENCY001";
        }
        
        public String getOnDutyDoctorId() {
            return "DR999"; // 值班医生
        }
        
        public String getDepartmentWithEquipment(String equipment) {
            // 根据设备类型返回对应科室
            if (equipment.contains("心电")) return "心内科";
            if (equipment.contains("呼吸")) return "呼吸科";
            return "急诊科";
        }
        
        public String getSpecialEquipmentBed(String department) {
            return getBestAvailableBed(department);
        }
        
        public List<String> getSpecialEquipment(String equipmentType) {
            return Collections.singletonList(equipmentType);
        }
        
        public String getSpecialistDoctorId(String symptoms) {
            // 根据症状匹配专科医生
            if (symptoms.contains("胸痛")) return "DR_CARDIO_001";
            if (symptoms.contains("呼吸")) return "DR_PULMONARY_001";
            return null;
        }
    }
}