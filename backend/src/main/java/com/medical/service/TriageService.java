package com.medical.service;

import com.medical.entity.Patient;
import com.medical.entity.TriageRecord;
import com.medical.entity.User;
import com.medical.repository.TriageRecordRepository;
import com.medical.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TriageService {
    
    private final TriageRecordRepository triageRecordRepository;
    private final UserRepository userRepository;
    private final PatientService patientService;
    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;
    private final BaichuanAIService baichuanAIService;
    private final ChromaVectorService chromaVectorService;
    private final ResourceSchedulingService resourceSchedulingService;
    private final HL7IntegrationService hl7IntegrationService;

    public TriageService(TriageRecordRepository triageRecordRepository,
                         UserRepository userRepository,
                         PatientService patientService,
                         SystemLogService systemLogService,
                         ObjectMapper objectMapper,
                         BaichuanAIService baichuanAIService,
                         ChromaVectorService chromaVectorService,
                         ResourceSchedulingService resourceSchedulingService,
                         HL7IntegrationService hl7IntegrationService) {
        this.triageRecordRepository = triageRecordRepository;
        this.userRepository = userRepository;
        this.patientService = patientService;
        this.systemLogService = systemLogService;
        this.objectMapper = objectMapper;
        this.baichuanAIService = baichuanAIService;
        this.chromaVectorService = chromaVectorService;
        this.resourceSchedulingService = resourceSchedulingService;
        this.hl7IntegrationService = hl7IntegrationService;
    }
    
    public Page<TriageRecord> findByStatus(TriageRecord.TriageStatus status, Pageable pageable) {
        return triageRecordRepository.findByStatusOrderByTriageLevelAscCreatedAtAsc(status, pageable);
    }
    
    public Page<TriageRecord> findByDoctorAndStatus(User doctor, TriageRecord.TriageStatus status, Pageable pageable) {
        return triageRecordRepository.findByAssignedDoctorAndStatusOrderByTriageLevelAscCreatedAtAsc(doctor, status, pageable);
    }
    
    public Page<TriageRecord> findByFilters(String department, TriageRecord.TriageStatus status, 
                                          Integer triageLevel, Pageable pageable) {
        return triageRecordRepository.findByFilters(department, status, triageLevel, pageable);
    }
    
    /**
     * 根据时间范围获取分诊记录
     */
    public List<TriageRecord> getTriageRecordsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return triageRecordRepository.findByArrivalTimeBetween(startTime, endTime);
    }
    
    public TriageRecord findById(Long id) {
        return triageRecordRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("分诊记录不存在"));
    }
    
    @Transactional
    @CacheEvict(value = {"triageStats", "patientList"}, allEntries = true)
    public TriageRecord saveTriageRecord(TriageRecord triageRecord) {
        // 如果没有设置到院时间，设置为当前时间
        if (triageRecord.getArrivalTime() == null) {
            triageRecord.setArrivalTime(LocalDateTime.now());
        }
        
        // 调用AI进行初步分诊
        performAITriage(triageRecord);
        
        TriageRecord saved = triageRecordRepository.save(triageRecord);
        
        // 记录日志
        systemLogService.logUserAction(
            triageRecord.getAssignedNurse() != null ? triageRecord.getAssignedNurse().getId() : null,
            triageRecord.getAssignedNurse() != null ? triageRecord.getAssignedNurse().getUsername() : "system",
            "CREATE_TRIAGE", "TRIAGE", saved.getId().toString(),
            "创建分诊记录，患者：" + triageRecord.getPatient().getPatientName()
        );
        
        log.info("创建分诊记录：患者 {}, 分诊等级 {}", 
            triageRecord.getPatient().getPatientName(), triageRecord.getTriageLevel());
        
        return saved;
    }
    

    
    /**
     * 执行人工分诊（重新AI分诊）
     */
    @Transactional
    public void performManualTriage(TriageRecord triageRecord) {
        performAITriage(triageRecord);
        triageRecordRepository.save(triageRecord);
    }
    
    /**
     * 获取待确认的分诊记录
     */
    public Map<String, Object> getPendingTriageRecords(int page, int size) {
        // 实现分页查询待确认的分诊记录
        Map<String, Object> result = new HashMap<>();
        // 这里应该实现具体的查询逻辑
        result.put("records", List.of());
        result.put("total", 0);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
    
    /**
     * 获取分诊记录详情（包含边缘数据）
     */
    public Map<String, Object> getTriageRecordWithEdgeData(Long triageRecordId) {
        TriageRecord triageRecord = findById(triageRecordId);
        Map<String, Object> details = new HashMap<>();
        details.put("triageRecord", triageRecord);
        // 这里可以添加关联的边缘设备数据查询
        return details;
    }
    
    /**
     * 保存来自边缘设备的分诊记录（不进行云端AI分诊）
     */
    @Transactional
    public TriageRecord saveTriageRecordFromEdge(TriageRecord triageRecord) {
        // 如果没有设置到院时间，设置为当前时间
        if (triageRecord.getArrivalTime() == null) {
            triageRecord.setArrivalTime(LocalDateTime.now());
        }
        
        // 先保存患者信息（如果是新患者）
        if (triageRecord.getPatient().getId() == null) {
            Patient savedPatient = patientService.savePatient(triageRecord.getPatient());
            triageRecord.setPatient(savedPatient);
        }
        
        // 直接保存分诊记录，使用边缘端的AI分诊结果
        TriageRecord saved = triageRecordRepository.save(triageRecord);
        
        // 记录日志
        systemLogService.logUserAction(
            null, "edge-device",
            "CREATE_EDGE_TRIAGE", "TRIAGE", saved.getId().toString(),
            "边缘设备创建分诊记录，患者：" + triageRecord.getPatient().getPatientName() + 
            "，分诊等级：" + triageRecord.getTriageLevel()
        );
        
        log.info("边缘设备创建分诊记录：患者 {}, 分诊等级 {}, AI置信度 {}", 
            triageRecord.getPatient().getPatientName(), 
            triageRecord.getTriageLevel(), 
            triageRecord.getAiConfidence());
        
        return saved;
    }
    
    @Transactional
    @CacheEvict(value = {"triageStats", "patientList"}, allEntries = true)
    public TriageRecord updateTriageRecord(TriageRecord triageRecord) {
        TriageRecord existing = findById(triageRecord.getId());
        
        // 更新字段
        existing.setChiefComplaint(triageRecord.getChiefComplaint());
        existing.setVitalSigns(triageRecord.getVitalSigns());
        existing.setTriageLevel(triageRecord.getTriageLevel());
        existing.setAssignedDepartment(triageRecord.getAssignedDepartment());
        existing.setStatus(triageRecord.getStatus());
        
        // 如果生命体征有变化，重新进行AI评估
        if (!existing.getVitalSigns().equals(triageRecord.getVitalSigns())) {
            performAITriage(existing);
        }
        
        TriageRecord updated = triageRecordRepository.save(existing);
        
        systemLogService.logUserAction(null, "system", "UPDATE_TRIAGE", "TRIAGE", 
            updated.getId().toString(), "更新分诊记录");
        
        return updated;
    }
    
    @Transactional
    public TriageRecord updateVitalSigns(Long patientId, Map<String, Object> vitalSigns) {
        // 找到患者最新的分诊记录
        TriageRecord triageRecord = triageRecordRepository.findByPatient_IdAndStatus(
            patientId, TriageRecord.TriageStatus.WAITING)
            .orElseThrow(() -> new RuntimeException("未找到患者的待处理分诊记录"));
        
        try {
            String vitalSignsJson = objectMapper.writeValueAsString(vitalSigns);
            triageRecord.setVitalSigns(vitalSignsJson);
            
            // 重新进行AI评估
            performAITriage(triageRecord);
            
            return triageRecordRepository.save(triageRecord);
        } catch (Exception e) {
            log.error("更新生命体征失败", e);
            throw new RuntimeException("更新生命体征失败");
        }
    }
    
    @Transactional
    public TriageRecord assignDoctor(Long triageId, Long doctorId) {
        TriageRecord triageRecord = findById(triageId);
        User doctor = userRepository.findById(doctorId)
            .orElseThrow(() -> new RuntimeException("医生不存在"));
        
        if (doctor.getRole() != User.Role.DOCTOR) {
            throw new RuntimeException("指定用户不是医生");
        }
        
        triageRecord.setAssignedDoctor(doctor);
        triageRecord.setStatus(TriageRecord.TriageStatus.IN_PROGRESS);
        
        TriageRecord updated = triageRecordRepository.save(triageRecord);
        
        systemLogService.logUserAction(null, "system", "ASSIGN_DOCTOR", "TRIAGE",
            triageId.toString(), "分配医生：" + doctor.getFullName());
        
        return updated;
    }
    
    public Long countByStatus(TriageRecord.TriageStatus status) {
        return triageRecordRepository.countByStatus(status);
    }
    
    public Long countByTriageLevelAndStatus(Integer triageLevel, TriageRecord.TriageStatus status) {
        if (triageLevel == null) {
            return countByStatus(status);
        }
        return triageRecordRepository.countByTriageLevelAndStatus(triageLevel, status);
    }
    
    public Long countByDoctorAndStatus(User doctor, TriageRecord.TriageStatus status) {
        return triageRecordRepository.countByAssignedDoctorAndStatus(doctor, status);
    }
    
    public Long countTodayCompleted() {
        // 这里应该查询今天完成的分诊记录数量
        // 简化实现，返回固定值
        return 25L;
    }
    
    public Map<String, Object> getAIDetailedAnalysis(Long triageId) {
        TriageRecord triageRecord = findById(triageId);
        
        try {
            Map<String, Object> vitalSigns = objectMapper.readValue(
                triageRecord.getVitalSigns(), Map.class);
            
            Map<String, Object> analysis = new HashMap<>();
            
            // 生命体征分析
            StringBuilder vitalAnalysis = new StringBuilder();
            vitalAnalysis.append("生命体征评估：");
            
            double temp = ((Number) vitalSigns.getOrDefault("temperature", 36.5)).doubleValue();
            int sbp = ((Number) vitalSigns.getOrDefault("systolicBP", 120)).intValue();
            int dbp = ((Number) vitalSigns.getOrDefault("diastolicBP", 80)).intValue();
            int hr = ((Number) vitalSigns.getOrDefault("heartRate", 80)).intValue();
            int rr = ((Number) vitalSigns.getOrDefault("respiratoryRate", 18)).intValue();
            int spo2 = ((Number) vitalSigns.getOrDefault("bloodOxygen", 98)).intValue();
            
            vitalAnalysis.append("体温").append(temp).append("°C");
            if (temp < 36.0) vitalAnalysis.append("(体温过低)");
            else if (temp > 38.0) vitalAnalysis.append("(发热)");
            else if (temp > 37.5) vitalAnalysis.append("(低热)");
            else vitalAnalysis.append("(正常)");
            
            vitalAnalysis.append("，血压").append(sbp).append("/").append(dbp).append("mmHg");
            if (sbp > 180 || dbp > 110) vitalAnalysis.append("(高血压危象)");
            else if (sbp > 160 || dbp > 100) vitalAnalysis.append("(高血压)");
            else if (sbp < 90) vitalAnalysis.append("(低血压)");
            else vitalAnalysis.append("(正常)");
            
            vitalAnalysis.append("，心率").append(hr).append("次/分");
            if (hr > 120) vitalAnalysis.append("(心动过速)");
            else if (hr < 50) vitalAnalysis.append("(心动过缓)");
            else vitalAnalysis.append("(正常)");
            
            vitalAnalysis.append("，呼吸").append(rr).append("次/分");
            if (rr > 25) vitalAnalysis.append("(呼吸急促)");
            else if (rr < 10) vitalAnalysis.append("(呼吸抑制)");
            else vitalAnalysis.append("(正常)");
            
            vitalAnalysis.append("，血氧饱和度").append(spo2).append("%");
            if (spo2 < 90) vitalAnalysis.append("(严重缺氧)");
            else if (spo2 < 95) vitalAnalysis.append("(轻度缺氧)");
            else vitalAnalysis.append("(正常)");
            
            analysis.put("vitalSignsAnalysis", vitalAnalysis.toString());
            
            // 症状分析
            String complaint = triageRecord.getChiefComplaint();
            analysis.put("symptomAnalysis", "主诉：" + complaint + "。" + getSymptomAnalysis(complaint));
            
            // 分诊等级解释
            String triageExplanation = getTriageLevelExplanation(triageRecord.getTriageLevel());
            analysis.put("triageExplanation", triageExplanation);
            
            // 可能诊断（基于症状和生命体征）
            String[] possibleDiagnoses = getPossibleDiagnoses(complaint, temp, sbp, hr, spo2);
            analysis.put("possibleDiagnoses", possibleDiagnoses);
            
            // 建议检查
            String[] recommendedExams = getRecommendedExams(complaint, triageRecord.getTriageLevel());
            analysis.put("recommendedExams", recommendedExams);
            
            // 处理建议
            String[] treatmentSuggestions = getTreatmentSuggestions(triageRecord.getTriageLevel(), complaint);
            analysis.put("treatmentSuggestions", treatmentSuggestions);
            
            return analysis;
            
        } catch (Exception e) {
            log.error("AI详细分析失败", e);
            Map<String, Object> errorAnalysis = new HashMap<>();
            errorAnalysis.put("error", "分析系统暂时不可用，请进行人工评估");
            return errorAnalysis;
        }
    }
    
    private String getSymptomAnalysis(String complaint) {
        if (complaint.contains("胸痛")) {
            return "胸痛是急诊科常见症状，需要排除急性心肌梗死、肺栓塞、主动脉夹层等危险疾病。";
        } else if (complaint.contains("呼吸困难")) {
            return "呼吸困难可能提示心肺功能异常，需要紧急评估气道、呼吸、循环状态。";
        } else if (complaint.contains("腹痛")) {
            return "急性腹痛需要考虑急腹症可能，如阑尾炎、肠梗阻、胆囊炎等。";
        } else if (complaint.contains("头痛")) {
            return "头痛需要排除颅内压增高、脑血管意外等严重疾病。";
        } else if (complaint.contains("发热")) {
            return "发热是机体对感染或其他疾病的反应，需要查找感染源。";
        }
        return "根据症状描述进行综合评估。";
    }
    
    private String getTriageLevelExplanation(int level) {
        switch (level) {
            case 1: return "I级（红色）- 濒危：生命体征极不稳定，需要立即抢救，无等待时间。";
            case 2: return "II级（橙色）- 危急：病情危急，需要10分钟内开始处理。";
            case 3: return "III级（黄色）- 急症：病情较急，需要30分钟内开始处理。";
            case 4: return "IV级（绿色）- 次急症：病情相对稳定，可等待60分钟内处理。";
            case 5: return "V级（蓝色）- 非急症：病情稳定，可安排预约或等待120分钟内处理。";
            default: return "分诊等级待定，需要人工评估。";
        }
    }
    
    private String[] getPossibleDiagnoses(String complaint, double temp, int sbp, int hr, int spo2) {
        if (complaint.contains("胸痛")) {
            if (hr > 100 || sbp > 160) {
                return new String[]{"急性心肌梗死 (高风险)", "不稳定性心绞痛 (中风险)", "肺栓塞 (中风险)"};
            } else {
                return new String[]{"稳定性心绞痛 (中风险)", "肌肉骨骼疼痛 (低风险)", "胃食管反流 (低风险)"};
            }
        } else if (complaint.contains("呼吸困难")) {
            if (spo2 < 90) {
                return new String[]{"急性肺水肿 (高风险)", "肺栓塞 (高风险)", "急性哮喘发作 (高风险)"};
            } else {
                return new String[]{"慢性阻塞性肺病急性发作 (中风险)", "支气管炎 (低风险)", "焦虑症 (低风险)"};
            }
        } else if (complaint.contains("发热") && temp > 38.5) {
            return new String[]{"细菌性感染 (中风险)", "病毒性感染 (中风险)", "其他感染性疾病 (中风险)"};
        }
        return new String[]{"需要进一步检查确定诊断", "建议详细病史采集", "考虑多系统疾病可能"};
    }
    
    private String[] getRecommendedExams(String complaint, int triageLevel) {
        if (triageLevel <= 2) {
            if (complaint.contains("胸痛")) {
                return new String[]{"心电图（紧急）", "心肌酶谱", "胸部CT", "D-二聚体"};
            } else if (complaint.contains("呼吸困难")) {
                return new String[]{"胸部X线（紧急）", "动脉血气分析", "心电图", "BNP或NT-proBNP"};
            } else if (complaint.contains("腹痛")) {
                return new String[]{"腹部CT（紧急）", "血常规", "生化全套", "尿常规"};
            }
            return new String[]{"血常规（紧急）", "生化全套", "凝血功能", "相关影像学检查"};
        } else if (triageLevel == 3) {
            return new String[]{"血常规", "生化全套", "相关影像学检查", "心电图"};
        } else {
            return new String[]{"血常规", "尿常规", "如需要可行相关检查"};
        }
    }
    
    private String[] getTreatmentSuggestions(int triageLevel, String complaint) {
        switch (triageLevel) {
            case 1:
                return new String[]{"立即启动抢救流程", "建立静脉通路", "持续生命体征监测", "必要时气管插管"};
            case 2:
                return new String[]{"优先处理", "10分钟内医生接诊", "建立静脉通路", "持续监测生命体征"};
            case 3:
                return new String[]{"30分钟内安排医生接诊", "症状缓解处理", "必要时建立静脉通路"};
            case 4:
                return new String[]{"60分钟内安排医生接诊", "对症处理", "观察病情变化"};
            case 5:
                return new String[]{"可安排预约就诊", "健康宣教", "必要时给予对症处理"};
            default:
                return new String[]{"需要人工评估", "建议详细检查"};
        }
    }
    
    @Transactional
    public void requestConsultation(Long triageId, String reason, String requesterUsername) {
        TriageRecord triageRecord = findById(triageId);
        
        systemLogService.logUserAction(
            triageRecord.getAssignedDoctor() != null ? triageRecord.getAssignedDoctor().getId() : null,
            requesterUsername, "REQUEST_CONSULTATION", "TRIAGE", triageId.toString(),
            "申请会诊，原因：" + reason
        );
        
        log.info("用户 {} 为患者 {} 申请会诊，原因：{}", 
            requesterUsername, triageRecord.getPatient().getPatientName(), reason);
    }
    
    @Transactional
    public void emergencyCall(Long triageId, String reason) {
        TriageRecord triageRecord = findById(triageId);
        
        // 将分诊等级提升到最高
        triageRecord.setTriageLevel(1);
        triageRecordRepository.save(triageRecord);
        
        systemLogService.logUserAction(null, "system", "EMERGENCY_CALL", "TRIAGE",
            triageId.toString(), "紧急呼叫，原因：" + reason);
        
        log.warn("紧急呼叫：患者 {}, 原因：{}", 
            triageRecord.getPatient().getPatientName(), reason);
    }
    
    private void performAITriage(TriageRecord triageRecord) {
        // 基于急诊预检分诊标准的AI分诊逻辑（仅输出分诊等级与轻重缓急）
        try {
            Map<String, Object> vitalSigns = objectMapper.readValue(
                triageRecord.getVitalSigns(), Map.class);
            
            // 获取生命体征数据
            double temperature = ((Number) vitalSigns.getOrDefault("temperature", 36.5)).doubleValue();
            int systolicBP = ((Number) vitalSigns.getOrDefault("systolicBP", 120)).intValue();
            int diastolicBP = ((Number) vitalSigns.getOrDefault("diastolicBP", 80)).intValue();
            int heartRate = ((Number) vitalSigns.getOrDefault("heartRate", 80)).intValue();
            int respiratoryRate = ((Number) vitalSigns.getOrDefault("respiratoryRate", 18)).intValue();
            int bloodOxygen = ((Number) vitalSigns.getOrDefault("bloodOxygen", 98)).intValue();
            String consciousness = (String) vitalSigns.getOrDefault("consciousness", "清醒");
            
            String chiefComplaint = triageRecord.getChiefComplaint().toLowerCase();
            
            int triageLevel = 5; // 默认非急症
            double confidence = 0.7;
            String priority = "非急症";
            String waitTime = "120分钟或预约";
            String color = "蓝色";
            
            // I级分诊 - 濒危（红色）
            if (isLevel1Criteria(temperature, systolicBP, heartRate, respiratoryRate, bloodOxygen, consciousness, chiefComplaint)) {
                triageLevel = 1;
                confidence = 0.95;
                priority = "濒危";
                waitTime = "立即处理";
                color = "红色";
            }
            // II级分诊 - 危急（橙色）
            else if (isLevel2Criteria(temperature, systolicBP, diastolicBP, heartRate, respiratoryRate, bloodOxygen, consciousness, chiefComplaint)) {
                triageLevel = 2;
                confidence = 0.9;
                priority = "危急";
                waitTime = "10分钟内";
                color = "橙色";
            }
            // III级分诊 - 急症（黄色）
            else if (isLevel3Criteria(temperature, systolicBP, diastolicBP, heartRate, respiratoryRate, bloodOxygen, chiefComplaint)) {
                triageLevel = 3;
                confidence = 0.85;
                priority = "急症";
                waitTime = "30分钟内";
                color = "黄色";
            }
            // IV级分诊 - 次急症（绿色）
            else if (isLevel4Criteria(temperature, systolicBP, diastolicBP, heartRate, respiratoryRate, chiefComplaint)) {
                triageLevel = 4;
                confidence = 0.8;
                priority = "次急症";
                waitTime = "60分钟内";
                color = "绿色";
            }
            // V级分诊 - 非急症（蓝色）
            else {
                triageLevel = 5;
                confidence = 0.75;
                priority = "非急症";
                waitTime = "120分钟或预约";
                color = "蓝色";
            }
            
            triageRecord.setTriageLevel(triageLevel);
            triageRecord.setTriagePriority(priority);
            triageRecord.setTriageColor(color);
            triageRecord.setWaitTime(waitTime);
            triageRecord.setTriageScore(confidence);
            triageRecord.setAiConfidence(confidence);
            
        } catch (Exception e) {
            log.error("AI分诊处理失败", e);
            // 设置默认值
            triageRecord.setTriageLevel(4);
            triageRecord.setTriagePriority("次急症");
            triageRecord.setTriageColor("绿色");
            triageRecord.setWaitTime("60分钟内");
            triageRecord.setTriageScore(0.5);
            triageRecord.setAiConfidence(0.5);
        }
    }
    
    /**
     * I级分诊标准判断 - 濒危（红色）
     */
    private boolean isLevel1Criteria(double temp, int sbp, int hr, int rr, int spo2, String consciousness, String complaint) {
        // 生命体征危险指标
        if (temp < 35.0 || temp > 40.0) return true;  // 体温<35°C或>40°C
        if (sbp < 70 || sbp > 200) return true;       // 收缩压<70mmHg或>200mmHg
        if (hr < 40 || hr > 150) return true;         // 心率<40次/分或>150次/分
        if (rr < 8 || rr > 35) return true;           // 呼吸<8次/分或>35次/分
        if (spo2 < 85) return true;                   // 血氧饱和度<85%
        
        // 意识状态
        if (consciousness.contains("昏迷") || consciousness.contains("休克")) return true;
        
        // 危险症状
        if (complaint.contains("心脏骤停") || complaint.contains("呼吸骤停") || 
            complaint.contains("大出血") || complaint.contains("严重外伤") ||
            complaint.contains("中毒") || complaint.contains("窒息")) return true;
            
        return false;
    }
    
    /**
     * II级分诊标准判断 - 危急（橙色）
     */
    private boolean isLevel2Criteria(double temp, int sbp, int dbp, int hr, int rr, int spo2, String consciousness, String complaint) {
        // 生命体征异常
        if (temp >= 38.5 && temp < 40.0) return true;  // 体温38.5-40°C
        if (sbp >= 180 || sbp <= 90) return true;       // 收缩压≥180mmHg或≤90mmHg
        if (dbp >= 110) return true;                    // 舒张压≥110mmHg
        if (hr >= 120 || hr <= 50) return true;        // 心率≥120次/分或≤50次/分
        if (rr >= 25 || rr <= 10) return true;          // 呼吸≥25次/分或≤10次/分
        if (spo2 >= 85 && spo2 < 90) return true;       // 血氧饱和度85-90%
        
        // 意识改变
        if (consciousness.contains("嗜睡") || consciousness.contains("烦躁")) return true;
        
        // 危急症状
        if (complaint.contains("胸痛") || complaint.contains("呼吸困难") ||
            complaint.contains("腹痛") || complaint.contains("头痛") ||
            complaint.contains("意识障碍") || complaint.contains("抽搐")) return true;
            
        return false;
    }
    
    /**
     * III级分诊标准判断 - 急症（黄色）
     */
    private boolean isLevel3Criteria(double temp, int sbp, int dbp, int hr, int rr, int spo2, String complaint) {
        // 生命体征轻度异常
        if (temp >= 37.5 && temp < 38.5) return true;   // 体温37.5-38.5°C
        if (sbp >= 160 && sbp < 180) return true;        // 收缩压160-180mmHg
        if (dbp >= 100 && dbp < 110) return true;        // 舒张压100-110mmHg
        if (hr >= 100 && hr < 120) return true;          // 心率100-120次/分
        if (rr >= 20 && rr < 25) return true;            // 呼吸20-25次/分
        if (spo2 >= 90 && spo2 < 95) return true;        // 血氧饱和度90-95%
        
        // 急症症状
        if (complaint.contains("发热") || complaint.contains("呕吐") ||
            complaint.contains("腹泻") || complaint.contains("外伤") ||
            complaint.contains("过敏") || complaint.contains("皮疹")) return true;
            
        return false;
    }
    
    /**
     * IV级分诊标准判断 - 次急症（绿色）
     */
    private boolean isLevel4Criteria(double temp, int sbp, int dbp, int hr, int rr, String complaint) {
        // 轻微异常
        if (temp >= 37.0 && temp < 37.5) return true;   // 体温37.0-37.5°C
        if (sbp >= 140 && sbp < 160) return true;        // 收缩压140-160mmHg
        if (dbp >= 90 && dbp < 100) return true;         // 舒张压90-100mmHg
        
        // 常见症状
        if (complaint.contains("咳嗽") || complaint.contains("感冒") ||
            complaint.contains("头晕") || complaint.contains("乏力") ||
            complaint.contains("关节痛") || complaint.contains("轻微外伤")) return true;
            
        return false;
    }
    
    // ===== 以下是TriageController需要的方法 =====
    
    /**
     * 异步处理边缘设备数据
     */
    public void processTriageAsync(com.medical.entity.EdgeDeviceData edgeData) {
        // 此方法应该由EdgeDataService处理，这里作为代理方法
        log.info("接收边缘设备数据: {}", edgeData.getDeviceId());
    }
    
    /**
     * 获取待处理的患者列表
     */
    public List<Patient> getPendingPatients() {
        List<TriageRecord> records = triageRecordRepository.findByStatus(
            TriageRecord.TriageStatus.WAITING);
        return records.stream()
            .map(TriageRecord::getPatient)
            .distinct()
            .toList();
    }
    
    /**
     * 确认分诊结果
     */
    @Transactional
    public void confirmTriage(Long patientId, Integer confirmedLevel, String nurseNotes) {
        TriageRecord record = triageRecordRepository.findByPatient_IdAndStatus(
            patientId, TriageRecord.TriageStatus.WAITING)
            .orElseThrow(() -> new RuntimeException("未找到待确认的分诊记录"));
        
        record.setTriageLevel(confirmedLevel);
        record.setNurseNotes(nurseNotes);
        record.setStatus(TriageRecord.TriageStatus.CONFIRMED);
        record.setConfirmedTime(LocalDateTime.now());
        
        triageRecordRepository.save(record);
        log.info("分诊已确认: 患者ID={}, 等级={}", patientId, confirmedLevel);
    }
    
    /**
     * 获取待诊断队列
     */
    public List<Patient> getDiagnosisQueue(String department) {
        List<TriageRecord> records;
        if (department != null && !department.isEmpty()) {
            records = triageRecordRepository.findByAssignedDepartmentAndStatus(
                department, TriageRecord.TriageStatus.CONFIRMED);
        } else {
            records = triageRecordRepository.findByStatus(TriageRecord.TriageStatus.CONFIRMED);
        }
        return records.stream()
            .map(TriageRecord::getPatient)
            .distinct()
            .toList();
    }
    
    /**
     * 提交诊断结果
     */
    @Transactional
    public void submitDiagnosis(Long patientId, String diagnosis, String treatment, Long doctorId) {
        TriageRecord record = triageRecordRepository.findByPatient_IdAndStatus(
            patientId, TriageRecord.TriageStatus.CONFIRMED)
            .orElseThrow(() -> new RuntimeException("未找到确认的分诊记录"));
        
        User doctor = userRepository.findById(doctorId)
            .orElseThrow(() -> new RuntimeException("医生不存在"));
        
        record.setAssignedDoctor(doctor);
        record.setStatus(TriageRecord.TriageStatus.COMPLETED);
        // 这里可以创建诊断记录
        
        triageRecordRepository.save(record);
        log.info("诊断已提交: 患者ID={}, 医生={}", patientId, doctor.getFullName());
    }
    
    /**
     * 获取AI诊断结果
     */
    public Map<String, Object> getAIDiagnosis(Long patientId) {
        TriageRecord record = triageRecordRepository.findByPatient_Id(patientId)
            .stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("未找到分诊记录"));
        
        Map<String, Object> result = new HashMap<>();
        result.put("aiDiagnosis", record.getAiDiagnosis());
        result.put("confidence", record.getAiConfidence());
        result.put("triageLevel", record.getTriageLevel());
        result.put("triagePriority", record.getTriagePriority());
        
        return result;
    }
    
    /**
     * 获取资源分配信息
     */
    public Map<String, Object> getResourceAllocation(Long patientId) {
        TriageRecord record = triageRecordRepository.findByPatient_Id(patientId)
            .stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("未找到分诊记录"));
        
        Map<String, Object> allocation = new HashMap<>();
        allocation.put("department", record.getAssignedDepartment());
        allocation.put("doctor", record.getAssignedDoctor() != null ? 
            record.getAssignedDoctor().getFullName() : null);
        allocation.put("triageLevel", record.getTriageLevel());
        allocation.put("waitTime", record.getWaitTime());
        
        return allocation;
    }
    
    /**
     * 获取分诊统计信息
     */
    public Map<String, Object> getTriageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("waiting", countByStatus(TriageRecord.TriageStatus.WAITING));
        stats.put("confirmed", countByStatus(TriageRecord.TriageStatus.CONFIRMED));
        stats.put("inProgress", countByStatus(TriageRecord.TriageStatus.IN_PROGRESS));
        stats.put("completed", countByStatus(TriageRecord.TriageStatus.COMPLETED));
        stats.put("todayTotal", countTodayCompleted());
        
        return stats;
    }
    
    /**
     * 根据ID获取患者信息
     */
    public Patient getPatientById(Long patientId) {
        return patientService.findById(patientId);
    }
    
    /**
     * 更新患者状态
     */
    @Transactional
    public void updatePatientStatus(Long patientId, String status) {
        TriageRecord record = triageRecordRepository.findByPatient_Id(patientId)
            .stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("未找到分诊记录"));
        
        record.setStatus(TriageRecord.TriageStatus.valueOf(status));
        triageRecordRepository.save(record);
        log.info("患者状态已更新: ID={}, 状态={}", patientId, status);
    }
    
    /**
     * 获取待复核分诊记录列表
     */
    public List<TriageRecord> findPendingTriageRecords() {
        // 获取待确认状态的分诊记录
        return triageRecordRepository.findByStatus(
            TriageRecord.TriageStatus.WAITING
        );
    }
    
    // ========== 统计方法 ==========
    
    /**
     * 按分诊等级统计
     */
    @Cacheable(value = "triageStats", key = "'level:' + #level")
    public Long countByTriageLevel(Integer level) {
        return triageRecordRepository.countByTriageLevel(level);
    }
    
    /**
     * 按日期统计
     */
    @Cacheable(value = "triageStats", key = "'date:' + #date")
    public Long countByDate(java.time.LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        return triageRecordRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    }
    
    /**
     * 按科室统计
     */
    @Cacheable(value = "triageStats", key = "'dept:' + #department")
    public Long countByDepartment(String department) {
        return triageRecordRepository.countByAssignedDepartment(department);
    }
    
    /**
     * 统计AI诊断使用数量
     */
    @Cacheable(value = "triageStats", key = "'aiUsed'")
    public Long countAIDiagnosisUsed() {
        // 统计AI置信度 > 0的记录
        return triageRecordRepository.countByAiConfidenceGreaterThan(0.0);
    }
}