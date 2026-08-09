package com.medical.dto;

import lombok.Data;

@Data
public class DashboardOverviewDTO {
    private Long totalUsers;
    private Long totalResources;
    private Long availableResources;
    private Long maintenanceResources;
    private Long todayAppointments;
    private Long activeUsers;
    private Double systemLoad;
    private String systemStatus;
}