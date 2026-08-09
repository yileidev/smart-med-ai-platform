package com.medical.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medical_resources")
public class MedicalResource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ResourceType type;
    
    @Enumerated(EnumType.STRING)
    private ResourceStatus status = ResourceStatus.AVAILABLE;
    
    private String location;
    
    @Column(name = "total_quantity")
    private Integer totalQuantity;
    
    @Column(name = "available_quantity")
    private Integer availableQuantity;
    
    @Column(name = "unit_price")
    private Double unitPrice;
    
    @Column(name = "maintenance_date")
    private LocalDateTime maintenanceDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum ResourceType {
        EQUIPMENT, MEDICINE, ROOM, BED, VEHICLE
    }
    
    public enum ResourceStatus {
        AVAILABLE, IN_USE, MAINTENANCE, OUT_OF_ORDER
    }
    
    // 手动添加Setter方法（解决IDE Lombok识别问题）
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setType(ResourceType type) { this.type = type; }
    public void setStatus(ResourceStatus status) { this.status = status; }
    public void setLocation(String location) { this.location = location; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
    public void setMaintenanceDate(LocalDateTime maintenanceDate) { this.maintenanceDate = maintenanceDate; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // 手动添加Getter方法
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ResourceType getType() { return type; }
    public ResourceStatus getStatus() { return status; }
    public String getLocation() { return location; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public Integer getAvailableQuantity() { return availableQuantity; }
    public Double getUnitPrice() { return unitPrice; }
    public LocalDateTime getMaintenanceDate() { return maintenanceDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}