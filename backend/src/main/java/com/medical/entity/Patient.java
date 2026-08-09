package com.medical.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "patients")
@EqualsAndHashCode(callSuper = false)
@EntityListeners(com.medical.listener.PatientEncryptionListener.class)  // 🔒 启用加密监听器
public class Patient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "patient_name", nullable = false)
    private String patientName;
    
    // 🔒 加密存储的身份证号
    @Column(name = "id_number", length = 500)
    private String idNumber;
    
    @Column(name = "id_card", length = 500)
    private String idCard;  // 加密存储
    
    // 🔒 加密存储的手机号
    @Column(name = "phone_number", length = 500)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    private Gender gender = Gender.OTHER;
    
    private Integer age;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;  // 添加出生日期字段
    
    private String address;
    
    @Column(name = "emergency_contact")
    private String emergencyContact;
    
    @Column(name = "emergency_phone")
    private String emergencyPhone;
    
    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;
    
    @Column(columnDefinition = "TEXT")
    private String allergies;
    
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
    
    /**
     * 设置性别（支持字符串参数）
     */
    public void setGenderFromString(String genderStr) {
        if (genderStr == null || genderStr.trim().isEmpty()) {
            this.gender = Gender.OTHER;
            return;
        }
        
        String normalized = genderStr.trim().toUpperCase();
        switch (normalized) {
            case "MALE":
            case "男":
            case "M":
                this.gender = Gender.MALE;
                break;
            case "FEMALE":
            case "女":
            case "F":
                this.gender = Gender.FEMALE;
                break;
            default:
                this.gender = Gender.OTHER;
        }
    }
    
    /**
     * 获取身份证号（兼容 idCard 和 idNumber）
     */
    public String getIdCard() {
        return idCard != null ? idCard : idNumber;
    }
    
    /**
     * 设置身份证号（同时设置 idCard 和 idNumber）
     */
    public void setIdCard(String idCard) {
        this.idCard = idCard;
        this.idNumber = idCard;
    }
    
    /**
     * 获取出生日期
     */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    
    /**
     * 设置出生日期
     */
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    
    public enum Gender {
        MALE, FEMALE, OTHER
    }
    
    // 手动添加getter方法（解决IDE Lombok识别问题）
    public Long getId() { return id; }
    public String getPatientName() { return patientName; }
    public Integer getAge() { return age; }
    public Gender getGender() { return gender; }
    public String getIdNumber() { return idNumber; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public String getMedicalHistory() { return medicalHistory; }
    public String getAllergies() { return allergies; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    // 手动添加setter方法
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setAge(Integer age) { this.age = age; }
    public void setGender(Gender gender) { this.gender = gender; }
}