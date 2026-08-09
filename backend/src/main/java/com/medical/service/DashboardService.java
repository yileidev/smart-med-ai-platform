package com.medical.service;

import com.medical.dto.DashboardOverviewDTO;
import com.medical.entity.MedicalResource;
import com.medical.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final UserService userService;
    private final MedicalResourceService resourceService;
    private final SystemLogService logService;
    
    public DashboardOverviewDTO getOverview() {
        DashboardOverviewDTO overview = new DashboardOverviewDTO();
        
        // 用户统计
        overview.setTotalUsers(userService.countByStatus(User.UserStatus.ACTIVE) + 
                              userService.countByStatus(User.UserStatus.INACTIVE) + 
                              userService.countByStatus(User.UserStatus.SUSPENDED));
        overview.setActiveUsers(userService.countByStatus(User.UserStatus.ACTIVE));
        
        // 资源统计
        overview.setTotalResources(resourceService.countByStatus(MedicalResource.ResourceStatus.AVAILABLE) +
                                  resourceService.countByStatus(MedicalResource.ResourceStatus.IN_USE) +
                                  resourceService.countByStatus(MedicalResource.ResourceStatus.MAINTENANCE) +
                                  resourceService.countByStatus(MedicalResource.ResourceStatus.OUT_OF_ORDER));
        overview.setAvailableResources(resourceService.countByStatus(MedicalResource.ResourceStatus.AVAILABLE));
        overview.setMaintenanceResources(resourceService.countByStatus(MedicalResource.ResourceStatus.MAINTENANCE));
        
        // 今日预约数量（模拟数据）
        overview.setTodayAppointments(logService.countTodayLogs());
        
        // 系统负载
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        overview.setSystemLoad(osBean.getSystemLoadAverage());
        
        // 系统状态
        overview.setSystemStatus("正常");
        
        return overview;
    }
}