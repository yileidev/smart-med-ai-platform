package com.medical.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.retriever.Retriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LangChain4j完整配置 - RAG医疗AI
 */
@Configuration
public class LangChain4jConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jConfig.class);
    
    @Value("${medical.ai.model.api-key:demo-key}")
    private String baichuanApiKey;
    
    @Value("${medical.ai.model.base-url:https://api.baichuan-ai.com/v1}")
    private String baichuanApiBaseUrl;
    
    @Value("${medical.ai.model.name:Baichuan2-Turbo-192k}")
    private String modelName;
    
    @Value("${medical.ai.model.temperature:0.2}")
    private Double temperature;
    
    @Value("${medical.ai.model.max-tokens:512}")
    private Integer maxTokens;

    @Value("${medical.vector.chroma.url:http://localhost:8000}")
    private String chromaBaseUrl;

    @Value("${medical.vector.chroma.collection:medical-knowledge}")
    private String chromaCollection;

    @Value("${medical.vector.chroma.enable:false}")
    private boolean chromaEnabled;
    
    /**
     * 嵌入模型 - AllMiniLmL6V2
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        try {
            EmbeddingModel model = new dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel();
            log.info("✓ 嵌入模型加载成功: AllMiniLmL6V2");
            return model;
        } catch (Exception e) {
            log.debug("嵌入模型加载失败，RAG功能降级: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 向量存储 - 使用内存存储（可替换为Chroma DB）
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        if (chromaEnabled) {
            EmbeddingStore<TextSegment> chromaStore = tryCreateChromaStore();
            if (chromaStore != null) {
                return chromaStore;
            }
        }
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 通过反射方式创建Chroma向量存储，避免在未安装依赖时编译失败
     */
    @SuppressWarnings("unchecked")
    private EmbeddingStore<TextSegment> tryCreateChromaStore() {
        try {
            Class<?> chromaClass = Class.forName("dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore");
            Object builder = chromaClass.getMethod("builder").invoke(null);
            builder.getClass().getMethod("baseUrl", String.class).invoke(builder, chromaBaseUrl);
            builder.getClass().getMethod("collectionName", String.class).invoke(builder, chromaCollection);
            EmbeddingStore<TextSegment> store = (EmbeddingStore<TextSegment>) builder.getClass()
                .getMethod("build")
                .invoke(builder);
            log.info("Chroma向量库连接成功 -> {} ({})", chromaCollection, chromaBaseUrl);
            return store;
        } catch (ClassNotFoundException e) {
            log.warn("未找到Chroma依赖包，已自动降级为内存向量库");
        } catch (Exception e) {
            log.warn("连接Chroma失败，将降级为内存向量库: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 聊天语言模型 - 百川AI
     * 如果没有配置API Key，使用模拟模式
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // 检查API Key是否配置（排除demo-key和空值）
        if (baichuanApiKey != null && 
            !baichuanApiKey.equals("demo-key") && 
            !baichuanApiKey.equals("your-api-key-here") &&
            !baichuanApiKey.trim().isEmpty()) {
            // 真实的百川API调用
            log.info("正在启用百川智能AI模型: {}", modelName);
            return OpenAiChatModel.builder()
                .apiKey(baichuanApiKey)
                .baseUrl(baichuanApiBaseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
        } else {
            // 模拟模式 - 返回固定响应（演示用）
            log.warn("未配置API Key，使用模拟模式。请在application.yml中配置 medical.ai.model.api-key");
            return new MockChatLanguageModel();
        }
    }
    
    /**
     * 检索器配置
     */
    @Bean
    public Retriever<TextSegment> retriever(EmbeddingStore<TextSegment> embeddingStore, 
                                           @org.springframework.beans.factory.annotation.Autowired(required = false) EmbeddingModel embeddingModel) {
        if (embeddingModel == null) {
            // 返回空检索器
            return query -> java.util.Collections.emptyList();
        }
        return EmbeddingStoreRetriever.from(embeddingStore, embeddingModel, 3, 0.6);
    }
    
    /**
     * 医疗诊断AI接口 - 使用AiServices链式调用
     */
    public interface MedicalDiagnosisAI {
        
        /**
         * 综合诊断（一次调用完成所有分析）- 性能优化版
         * 将症状分析、分诊评估、科室推荐、治疗方案合并为一次调用
         */
        String performComprehensiveDiagnosis(String comprehensivePrompt);
        
        /**
         * RAG增强的症状分析
         */
        String analyzeSymptomsWithRAG(String patientInfo, String symptoms, 
                                     String vitalSigns, String medicalHistory);
        
        /**
         * 评估分诊等级
         */
        String assessTriageLevel(String symptoms, String vitalSigns, String patientAge);
        
        /**
         * 推荐科室和设备
         */
        String recommendDepartmentAndEquipment(String symptoms, String diagnosisResult);
        
        /**
         * 生成治疗方案
         */
        String generateTreatmentPlan(String diagnosisResult, String patientInfo);
    }
    
    /**
     * 创建医疗诊断AI实例 - 使用LangChain4j的AiServices
     */
    @Bean
    public MedicalDiagnosisAI medicalDiagnosisAI(ChatLanguageModel chatModel,
                                                 Retriever<TextSegment> retriever) {
        return AiServices.builder(MedicalDiagnosisAI.class)
            .chatLanguageModel(chatModel)
            .retriever(retriever)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .build();
    }
    
    /**
     * 模拟聊天模型 - 用于演示和开发
     */
    private static class MockChatLanguageModel implements ChatLanguageModel {
        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
            java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
            
            String lastMessage = messages.isEmpty() ? "" : 
                messages.get(messages.size() - 1).text();
            
            String response;
            if (lastMessage.contains("症状") || lastMessage.contains("分析")) {
                response = """
                        基于患者症状分析：
                        1. 初步诊断：需要进一步检查
                        2. 建议检查项目：血常规、心电图
                        3. 注意事项：密切观察病情变化
                        【注意：这是演示模式，请配置百川API Key获得真实AI诊断】
                        """;
            } else if (lastMessage.contains("分诊")) {
                response = """
                        分诊评估结果：
                        - 分诊等级：3级（急症）
                        - 优先级：较高
                        - 建议处理时限：30分钟内
                        【注意：这是演示模式】
                        """;
            } else if (lastMessage.contains("科室") || lastMessage.contains("设备")) {
                response = """
                        推荐科室及设备：
                        - 建议科室：急诊科 / 内科
                        - 所需设备：心电图机、血压监测仪、血氧仪
                        - 备用科室：根据检查结果转诊
                        【注意：这是演示模式】
                        """;
            } else if (lastMessage.contains("治疗")) {
                response = """
                        治疗方案建议：
                        1. 急诊处理：生命体征监测
                        2. 检查项目：相关辅助检查
                        3. 用药建议：根据检查结果制定
                        4. 观察要点：密切监测病情
                        【注意：这是演示模式】
                        """;
            } else {
                response = """
                        AI诊断系统就绪。请提供患者信息进行分析。
                        【演示模式】配置application.yml中的baichuan.api.key以启用真实AI功能
                        """;
            }
            
            return new dev.langchain4j.model.output.Response<>(
                dev.langchain4j.data.message.AiMessage.from(response)
            );
        }
        
        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
            java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
            dev.langchain4j.agent.tool.ToolSpecification toolSpecification) {
            return generate(messages);
        }
        
        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
            java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
            java.util.List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecifications) {
            return generate(messages);
        }
    }
}