package com.medical.listener;

import com.medical.entity.Patient;
import com.medical.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/**
 * 🔒 患者数据加密监听器
 * 在保存到数据库前自动加密敏感字段
 * 在从数据库读取后自动解密敏感字段
 */
@Component
public class PatientEncryptionListener {
    
    private static final Logger log = LoggerFactory.getLogger(PatientEncryptionListener.class);

    private static EncryptionUtil encryptionUtil;

    @Autowired
    public void setEncryptionUtil(EncryptionUtil encryptionUtil) {
        PatientEncryptionListener.encryptionUtil = encryptionUtil;
    }

    /**
     * 保存前加密敏感数据
     */
    @PrePersist
    @PreUpdate
    public void encryptSensitiveData(Patient patient) {
        if (encryptionUtil == null) {
            log.warn("加密工具未初始化，跳过加密");
            return;
        }
        try {
            // 加密身份证号
            if (patient.getIdCard() != null && !patient.getIdCard().isEmpty()) {
                if (!isEncrypted(patient.getIdCard())) {
                    patient.setIdCard(encryptionUtil.encrypt(patient.getIdCard()));
                    patient.setIdNumber(patient.getIdCard());
                }
            }
            
            // 加密手机号
            if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
                if (!isEncrypted(patient.getPhoneNumber())) {
                    patient.setPhoneNumber(encryptionUtil.encrypt(patient.getPhoneNumber()));
                }
            }
            
            log.debug("患者敏感数据已加密: {}", patient.getPatientName());
        } catch (Exception e) {
            log.error("患者数据加密失败", e);
        }
    }

    /**
     * 加载后解密敏感数据
     */
    @PostLoad
    public void decryptSensitiveData(Patient patient) {
        if (encryptionUtil == null) {
            log.warn("加密工具未初始化，跳过解密");
            return;
        }
        try {
            // 解密身份证号
            if (patient.getIdCard() != null && !patient.getIdCard().isEmpty()) {
                if (isEncrypted(patient.getIdCard())) {
                    patient.setIdCard(encryptionUtil.decrypt(patient.getIdCard()));
                    patient.setIdNumber(patient.getIdCard());
                }
            }
            
            // 解密手机号
            if (patient.getPhoneNumber() != null && !patient.getPhoneNumber().isEmpty()) {
                if (isEncrypted(patient.getPhoneNumber())) {
                    patient.setPhoneNumber(encryptionUtil.decrypt(patient.getPhoneNumber()));
                }
            }
            
            log.debug("患者敏感数据已解密: {}", patient.getPatientName());
        } catch (Exception e) {
            log.error("患者数据解密失败", e);
        }
    }

    /**
     * 判断字符串是否已加密（Base64编码）
     */
    private boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        // 简单判断：加密后的数据是Base64格式，长度通常>50
        // 且不包含中文字符和特殊符号（除了+/=）
        return value.length() > 50 && value.matches("^[A-Za-z0-9+/=]+$");
    }
}
