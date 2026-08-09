package com.medical.service;

import com.medical.entity.Notification;
import com.medical.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebSocketService webSocketService;

    public NotificationService(NotificationRepository notificationRepository,
                                WebSocketService webSocketService) {
        this.notificationRepository = notificationRepository;
        this.webSocketService = webSocketService;
    }

    /**
     * 创建并发送通知
     */
    public Notification createNotification(Long userId, String username, String type, 
                                            String title, String content, String priority) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUsername(username);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setPriority(priority);
        
        Notification saved = notificationRepository.save(notification);
        
        // 通过WebSocket实时推送
        webSocketService.notifyUser(username, type, content);
        
        return saved;
    }

    /**
     * 获取用户未读通知
     */
    public List<Notification> getUnreadNotifications(String username) {
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    /**
     * 获取未读通知数量
     */
    public Long getUnreadCount(String username) {
        return notificationRepository.countByUsernameAndIsReadFalse(username);
    }

    /**
     * 标记通知为已读
     */
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead(String username) {
        List<Notification> notifications = 
            notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
        notifications.forEach(notification -> {
            if (!notification.getIsRead()) {
                notification.setIsRead(true);
                notification.setReadAt(LocalDateTime.now());
            }
        });
        notificationRepository.saveAll(notifications);
    }
}
