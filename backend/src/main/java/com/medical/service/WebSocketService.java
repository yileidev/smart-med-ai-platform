package com.medical.service;

import com.medical.entity.TriageRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket消息推送服务
 */
@Slf4j
@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 推送新患者通知到所有护士
     */
    public void notifyNewPatient(TriageRecord record) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NEW_PATIENT");
            message.put("data", record);
            message.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/nurse/new-patient", message);
            log.info("推送新患者通知: {}", record.getId());
        } catch (Exception e) {
            log.error("推送新患者通知失败", e);
        }
    }

    /**
     * 推送分诊状态更新到医生
     */
    public void notifyTriageUpdate(TriageRecord record) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "TRIAGE_UPDATE");
            message.put("data", record);
            message.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/doctor/triage-update", message);
            log.info("推送分诊更新通知: {}", record.getId());
        } catch (Exception e) {
            log.error("推送分诊更新通知失败", e);
        }
    }

    /**
     * 推送诊断完成通知到护士
     */
    public void notifyDiagnosisComplete(Long recordId, String diagnosis) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "DIAGNOSIS_COMPLETE");
            message.put("recordId", recordId);
            message.put("diagnosis", diagnosis);
            message.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/nurse/diagnosis-complete", message);
            log.info("推送诊断完成通知: {}", recordId);
        } catch (Exception e) {
            log.error("推送诊断完成通知失败", e);
        }
    }

    /**
     * 推送系统通知到指定用户
     */
    public void notifyUser(String username, String type, String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
            log.info("推送通知给用户 {}: {}", username, message);
        } catch (Exception e) {
            log.error("推送用户通知失败", e);
        }
    }

    /**
     * 推送会诊请求通知
     */
    public void notifyConsultationRequest(Long consultationId, String doctorUsername) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "CONSULTATION_REQUEST");
            message.put("consultationId", consultationId);
            message.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(doctorUsername, "/queue/consultation", message);
            log.info("推送会诊请求给医生: {}", doctorUsername);
        } catch (Exception e) {
            log.error("推送会诊请求失败", e);
        }
    }

    /**
     * 广播系统公告
     */
    public void broadcastAnnouncement(String announcement) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ANNOUNCEMENT");
            message.put("content", announcement);
            message.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/announcements", message);
            log.info("广播系统公告");
        } catch (Exception e) {
            log.error("广播系统公告失败", e);
        }
    }

    /**
     * 推送边缘设备状态更新
     */
    public void notifyDeviceStatusUpdate(String deviceId, String status) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "DEVICE_STATUS_UPDATE");
            message.put("deviceId", deviceId);
            message.put("status", status);
            message.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/device/status", message);
            log.info("推送设备状态更新: {} - {}", deviceId, status);
        } catch (Exception e) {
            log.error("推送设备状态更新失败", e);
        }
    }
}
