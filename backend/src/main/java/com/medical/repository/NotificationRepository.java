package com.medical.repository;

import com.medical.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * 根据用户ID查询未读通知
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据用户ID查询所有通知
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据用户名查询未读通知数量
     */
    Long countByUsernameAndIsReadFalse(String username);
    
    /**
     * 根据用户名查询通知
     */
    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);
}
