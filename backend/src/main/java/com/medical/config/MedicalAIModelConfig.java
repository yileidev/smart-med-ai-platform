package com.medical.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 医疗AI模型配置类
 */
@Configuration
public class MedicalAIModelConfig {

    @Value("${medical.ai.model.api-key:}")
    private String baichuanApiKey;

    @Value("${medical.ai.model.name:Baichuan2-Turbo-192k}")
    private String baichuanModelId;

    @Value("${medical.ai.model.base-url:https://api.baichuan-ai.com/v1}")
    private String baichuanUrl;

    @Value("${medical.ai.timeout:30000}")
    private int timeout;

    @Bean
    public BaichuanAIConfig baichuanAIConfig() {
        BaichuanAIConfig config = new BaichuanAIConfig();
        config.setApiKey(baichuanApiKey);
        config.setModelId(baichuanModelId);
        config.setUrl(baichuanUrl);
        config.setTimeout(timeout);
        return config;
    }

    /**
     * 百川AI配置类
     */
    public static class BaichuanAIConfig {
        private String apiKey;
        private String modelId;
        private String url;
        private int timeout;

        // Getters and Setters
        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
}