package com.medical.service;

import com.medical.entity.DiagnosisResult;
import com.medical.entity.ResourceAllocation;
import com.medical.entity.TriageRecord;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.event.rule.*;
import org.kie.internal.io.ResourceFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Drools规则引擎服务
 * 提供统一的规则执行、日志记录和统计功能
 * 
 * 规则文件位置: resources/rules/
 * - triage-priority.drl: 分诊优先级规则
 * - doctor-assignment.drl: 医生分配规则
 * - medical-resource-allocation.drl: 医疗资源调度规则
 * - resource-allocation.drl: 扩展资源分配规则
 */
@Slf4j
@Service
public class DroolsRuleEngineService {

    private KieContainer kieContainer;
    private boolean engineAvailable = false;
    
    // 规则执行统计
    private final AtomicLong totalRulesExecuted = new AtomicLong(0);
    private final AtomicLong totalSessionsCreated = new AtomicLong(0);
    private final Map<String, AtomicLong> ruleExecutionCount = new ConcurrentHashMap<>();
    private final List<RuleExecutionLog> executionLogs = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_LOG_SIZE = 1000;

    @PostConstruct
    public void initializeEngine() {
        try {
            log.info("正在初始化Drools规则引擎...");
            
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            
            // 加载所有规则文件
            String[] ruleFiles = {
                "rules/triage-priority.drl",
                "rules/doctor-assignment.drl", 
                "rules/medical-resource-allocation.drl",
                "rules/resource-allocation.drl"
            };
            
            int loadedCount = 0;
            for (String ruleFile : ruleFiles) {
                try {
                    kieFileSystem.write(ResourceFactory.newClassPathResource(ruleFile));
                    log.info("✓ 加载规则文件: {}", ruleFile);
                    loadedCount++;
                } catch (Exception e) {
                    log.warn("规则文件加载失败: {} - {}", ruleFile, e.getMessage());
                }
            }
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            
            if (kieBuilder.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
                log.error("规则编译错误: {}", kieBuilder.getResults().getMessages());
                engineAvailable = false;
                return;
            }
            
            KieModule kieModule = kieBuilder.getKieModule();
            this.kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
            this.engineAvailable = true;
            
            log.info("✓ Drools规则引擎初始化成功，加载了 {} 个规则文件", loadedCount);
            
        } catch (Exception e) {
            log.error("Drools规则引擎初始化失败: {}", e.getMessage());
            engineAvailable = false;
        }
    }

    /**
     * 执行医疗资源调度规则
     */
    public ResourceAllocationResult executeResourceAllocationRules(
            TriageRecord triageRecord, 
            DiagnosisResult diagnosisResult) {
        
        if (!engineAvailable) {
            log.warn("规则引擎不可用，使用默认调度策略");
            return createDefaultAllocation(triageRecord);
        }
        
        long startTime = System.currentTimeMillis();
        List<String> triggeredRules = new ArrayList<>();
        
        try {
            KieSession kieSession = kieContainer.newKieSession();
            totalSessionsCreated.incrementAndGet();
            
            // 添加规则执行监听器
            kieSession.addEventListener(new RuleRuntimeEventListener() {
                @Override
                public void objectInserted(ObjectInsertedEvent event) {}
                
                @Override
                public void objectUpdated(ObjectUpdatedEvent event) {}
                
                @Override
                public void objectDeleted(ObjectDeletedEvent event) {}
            });
            
            kieSession.addEventListener(new AgendaEventListener() {
                @Override
                public void matchCreated(MatchCreatedEvent event) {}
                
                @Override
                public void matchCancelled(MatchCancelledEvent event) {}
                
                @Override
                public void beforeMatchFired(BeforeMatchFiredEvent event) {
                    String ruleName = event.getMatch().getRule().getName();
                    triggeredRules.add(ruleName);
                    ruleExecutionCount.computeIfAbsent(ruleName, k -> new AtomicLong(0)).incrementAndGet();
                    log.debug("规则触发: {}", ruleName);
                }
                
                @Override
                public void afterMatchFired(AfterMatchFiredEvent event) {}
                
                @Override
                public void agendaGroupPopped(AgendaGroupPoppedEvent event) {}
                
                @Override
                public void agendaGroupPushed(AgendaGroupPushedEvent event) {}
                
                @Override
                public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {}
                
                @Override
                public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {}
                
                @Override
                public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {}
                
                @Override
                public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {}
            });
            
            // 创建资源分配对象
            ResourceAllocation allocation = new ResourceAllocation();
            allocation.setTriageRecord(triageRecord);
            allocation.setDiagnosisResult(diagnosisResult);
            allocation.setPriorityScore(50); // 默认优先级
            allocation.setEstimatedWaitTime(30); // 默认等待时间
            
            // 插入事实到工作内存
            kieSession.insert(triageRecord);
            if (diagnosisResult != null) {
                kieSession.insert(diagnosisResult);
            }
            kieSession.insert(allocation);
            
            // 执行规则
            int rulesFired = kieSession.fireAllRules();
            totalRulesExecuted.addAndGet(rulesFired);
            
            // 清理会话
            kieSession.dispose();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录执行日志
            RuleExecutionLog logEntry = new RuleExecutionLog(
                LocalDateTime.now(),
                triageRecord.getId(),
                rulesFired,
                triggeredRules,
                executionTime,
                true,
                null
            );
            addExecutionLog(logEntry);
            
            log.info("规则执行完成: 触发 {} 条规则, 耗时 {}ms, 规则: {}", 
                    rulesFired, executionTime, triggeredRules);
            
            return new ResourceAllocationResult(allocation, triggeredRules, rulesFired, executionTime);
            
        } catch (Exception e) {
            log.error("规则执行失败: {}", e.getMessage());
            
            RuleExecutionLog logEntry = new RuleExecutionLog(
                LocalDateTime.now(),
                triageRecord.getId(),
                0,
                Collections.emptyList(),
                System.currentTimeMillis() - startTime,
                false,
                e.getMessage()
            );
            addExecutionLog(logEntry);
            
            return createDefaultAllocation(triageRecord);
        }
    }

    /**
     * 获取规则引擎状态
     */
    public Map<String, Object> getEngineStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("available", engineAvailable);
        status.put("totalRulesExecuted", totalRulesExecuted.get());
        status.put("totalSessionsCreated", totalSessionsCreated.get());
        status.put("ruleExecutionCount", new HashMap<>(ruleExecutionCount));
        status.put("recentLogsCount", executionLogs.size());
        
        // 规则文件列表
        status.put("ruleFiles", Arrays.asList(
            "triage-priority.drl",
            "doctor-assignment.drl",
            "medical-resource-allocation.drl",
            "resource-allocation.drl"
        ));
        
        // 规则统计
        Map<String, Long> ruleStats = new HashMap<>();
        ruleExecutionCount.forEach((rule, count) -> ruleStats.put(rule, count.get()));
        status.put("ruleStatistics", ruleStats);
        
        return status;
    }

    /**
     * 获取规则执行日志
     */
    public List<RuleExecutionLog> getExecutionLogs(int limit) {
        int size = executionLogs.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(executionLogs.subList(fromIndex, size));
    }

    /**
     * 获取规则触发统计
     */
    public Map<String, Long> getRuleStatistics() {
        Map<String, Long> stats = new HashMap<>();
        ruleExecutionCount.forEach((rule, count) -> stats.put(rule, count.get()));
        return stats;
    }

    /**
     * 重新加载规则
     */
    public boolean reloadRules() {
        try {
            initializeEngine();
            return engineAvailable;
        } catch (Exception e) {
            log.error("规则重载失败: {}", e.getMessage());
            return false;
        }
    }

    private void addExecutionLog(RuleExecutionLog logEntry) {
        executionLogs.add(logEntry);
        // 保持日志数量限制
        while (executionLogs.size() > MAX_LOG_SIZE) {
            executionLogs.remove(0);
        }
    }

    private ResourceAllocationResult createDefaultAllocation(TriageRecord triageRecord) {
        ResourceAllocation allocation = new ResourceAllocation();
        allocation.setTriageRecord(triageRecord);
        allocation.setAllocatedDepartment("急诊科");
        allocation.setPriorityScore(getDefaultPriority(triageRecord.getTriageLevel()));
        allocation.setEstimatedWaitTime(getDefaultWaitTime(triageRecord.getTriageLevel()));
        allocation.setAllocationReason("默认调度策略（规则引擎不可用）");
        
        return new ResourceAllocationResult(allocation, Collections.emptyList(), 0, 0);
    }

    private int getDefaultPriority(int triageLevel) {
        return switch (triageLevel) {
            case 1 -> 100;
            case 2 -> 80;
            case 3 -> 60;
            default -> 40;
        };
    }

    private int getDefaultWaitTime(int triageLevel) {
        return switch (triageLevel) {
            case 1 -> 0;
            case 2 -> 10;
            case 3 -> 30;
            default -> 60;
        };
    }

    public boolean isEngineAvailable() {
        return engineAvailable;
    }
    
    /**
     * 执行分诊规则测试
     */
    public Map<String, Object> executeTriage(
            String chiefComplaint, int heartRate, int bpSystolic, int bpDiastolic,
            double temperature, int respiratoryRate, int oxygenSaturation) {
        
        Map<String, Object> result = new HashMap<>();
        List<String> matchedRules = new ArrayList<>();
        int triageLevel = 4; // 默认4级
        int triageScore = 50;
        String department = "急诊科";
        
        // 基于生命体征的规则判断
        // 1级：危急
        if (heartRate > 150 || heartRate < 40 || 
            bpSystolic > 200 || bpSystolic < 80 ||
            oxygenSaturation < 90 || temperature > 40) {
            triageLevel = 1;
            triageScore = 95;
            matchedRules.add("危急生命体征规则");
        }
        // 2级：紧急
        else if (heartRate > 120 || heartRate < 50 ||
                 bpSystolic > 180 || bpSystolic < 90 ||
                 oxygenSaturation < 94 || temperature > 39) {
            triageLevel = 2;
            triageScore = 80;
            matchedRules.add("紧急生命体征规则");
        }
        // 3级：较急
        else if (heartRate > 100 || heartRate < 60 ||
                 bpSystolic > 160 || bpSystolic < 100 ||
                 temperature > 38.5) {
            triageLevel = 3;
            triageScore = 65;
            matchedRules.add("较急生命体征规则");
        }
        
        // 基于主诉的规则
        if (chiefComplaint != null) {
            String complaint = chiefComplaint.toLowerCase();
            if (complaint.contains("胸痛") || complaint.contains("胸闷")) {
                department = "心内科";
                if (triageLevel > 2) triageLevel = 2;
                matchedRules.add("胸痛分诊规则");
            } else if (complaint.contains("呼吸困难") || complaint.contains("气促")) {
                department = "呼吸内科";
                if (triageLevel > 2) triageLevel = 2;
                matchedRules.add("呼吸困难分诊规则");
            } else if (complaint.contains("腹痛")) {
                department = "消化内科";
                matchedRules.add("腹痛分诊规则");
            } else if (complaint.contains("头痛") || complaint.contains("头晕")) {
                department = "神经内科";
                matchedRules.add("头痛分诊规则");
            } else if (complaint.contains("发烧") || complaint.contains("发热")) {
                department = "发热门诊";
                matchedRules.add("发热分诊规则");
            } else if (complaint.contains("外伤") || complaint.contains("骨折")) {
                department = "骨科";
                matchedRules.add("外伤分诊规则");
            }
        }
        
        if (matchedRules.isEmpty()) {
            matchedRules.add("默认分诊规则");
        }
        
        result.put("triageLevel", triageLevel);
        result.put("triageScore", triageScore);
        result.put("department", department);
        result.put("rulesFired", matchedRules.size());
        result.put("matchedRules", matchedRules);
        result.put("engineAvailable", engineAvailable);
        
        log.info("分诊测试结果: 等级={}, 科室={}, 触发规则={}", triageLevel, department, matchedRules);
        
        return result;
    }

    /**
     * 规则执行日志记录
     */
    public record RuleExecutionLog(
        LocalDateTime timestamp,
        Long triageRecordId,
        int rulesFired,
        List<String> triggeredRules,
        long executionTimeMs,
        boolean success,
        String errorMessage
    ) {}

    /**
     * 资源分配结果
     */
    public record ResourceAllocationResult(
        ResourceAllocation allocation,
        List<String> triggeredRules,
        int rulesFired,
        long executionTimeMs
    ) {}
}
