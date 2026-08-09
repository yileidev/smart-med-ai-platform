package com.medical.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieRepository;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drools规则引擎配置
 * 用于医疗分诊、资源调度等决策
 */
@Configuration
public class DroolsConfig {
    
    private static final String RULES_PATH = "rules/";
    private static final KieServices kieServices = KieServices.Factory.get();
    
    /**
     * 配置KieContainer
     */
    @Bean
    public KieContainer kieContainer() {
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        
        // 加载所有规则文件
        kieFileSystem.write(ResourceFactory.newClassPathResource(
            RULES_PATH + "triage-priority.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(
            RULES_PATH + "doctor-assignment.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(
            RULES_PATH + "medical-resource-allocation.drl"));
        
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        
        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }
    
    /**
     * 创建KieSession用于执行规则
     */
    @Bean
    public KieSession kieSession() {
        return kieContainer().newKieSession();
    }
}
