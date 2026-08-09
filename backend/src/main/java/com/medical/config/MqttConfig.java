package com.medical.config;

import com.medical.service.MqttMessageHandler;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * MQTT配置类 - 用于接收边缘设备分诊数据
 */
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client.id:medical-cloud-server}")
    private String clientId;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    @Value("${mqtt.topic.triage:medical/triage/data}")
    private String triageTopic;

    @Value("${mqtt.topic.device.status:medical/device/status}")
    private String deviceStatusTopic;

    @Value("${mqtt.topic.heartbeat:medical/device/heartbeat}")
    private String heartbeatTopic;
    
    @Value("${mqtt.topic.final:medical/triage/final}")
    private String finalTriageTopic;

    @Autowired
    private MqttMessageHandler mqttMessageHandler;

    private MqttClient mqttClient;

    /**
     * 创建MQTT客户端Bean
     */
    @Bean
    public MqttClient mqttClient() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            mqttClient = new MqttClient(brokerUrl, clientId, persistence);
            return mqttClient;
        } catch (MqttException e) {
            System.err.println("⚠️ MQTT客户端创建失败(不影响核心功能): " + e.getMessage());
            System.err.println("💡 边缘设备数据同步功能将不可用，其他功能正常");
            // 返回null，让应用继续启动
            return null;
        }
    }

    /**
     * 配置MQTT连接选项
     */
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(5);  // 减少超时时间到5秒
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);
        
        if (!username.isEmpty()) {
            options.setUserName(username);
        }
        if (!password.isEmpty()) {
            options.setPassword(password.toCharArray());
        }
        
        return options;
    }

    /**
     * 初始化MQTT连接和订阅
     */
    @PostConstruct
    @DependsOn({"mqttClient", "mqttConnectOptions"})
    public void initMqttConnection() {
        try {
            if (mqttClient == null) {
                System.err.println("⚠️ MQTT客户端未初始化，跳过连接");
                return;
            }
            if (!mqttClient.isConnected()) {
                mqttClient.connect(mqttConnectOptions());
                System.out.println("✅ MQTT客户端连接成功: " + brokerUrl);
                
                // 订阅分诊数据主题
                mqttClient.subscribe(triageTopic, 1, (topic, message) -> {
                    mqttMessageHandler.handleTriageMessage(topic, new String(message.getPayload()));
                });
                System.out.println("✅ 订阅分诊数据主题: " + triageTopic);
                
                // 订阅设备状态主题
                mqttClient.subscribe(deviceStatusTopic, 1, (topic, message) -> {
                    mqttMessageHandler.handleDeviceStatusMessage(topic, new String(message.getPayload()));
                });
                System.out.println("✅ 订阅设备状态主题: " + deviceStatusTopic);
                
                // 订阅心跳主题
                mqttClient.subscribe(heartbeatTopic, 1, (topic, message) -> {
                    mqttMessageHandler.handleHeartbeatMessage(topic, new String(message.getPayload()));
                });
                System.out.println("✅ 订阅心跳主题: " + heartbeatTopic);
                
                // 订阅最终分诊结果主题（新增）
                mqttClient.subscribe(finalTriageTopic, 1, (topic, message) -> {
                    mqttMessageHandler.handleFinalTriageMessage(topic, new String(message.getPayload()));
                });
                System.out.println("✅ 订阅最终分诊结果主题: " + finalTriageTopic);
                
                // 设置连接丢失回调
                mqttClient.setCallback(mqttMessageHandler);
            }
        } catch (MqttException e) {
            System.err.println("⚠️ MQTT连接失败(不影响核心功能): " + e.getMessage());
            System.err.println("💡 如需边缘设备数据同步功能，请启动MQTT Broker");
            // 不抛出异常，允许应用继续启动
        }
    }

    /**
     * 销毁时断开MQTT连接
     */
    @PreDestroy
    public void destroyMqttConnection() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                System.out.println("MQTT客户端已断开连接");
            }
        } catch (MqttException e) {
            System.err.println("MQTT断开连接失败: " + e.getMessage());
        }
    }

    // Getters for topics (used by other services)
    public String getTriageTopic() { return triageTopic; }
    public String getDeviceStatusTopic() { return deviceStatusTopic; }
    public String getHeartbeatTopic() { return heartbeatTopic; }
}