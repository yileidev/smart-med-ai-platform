package com.medical.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

/**
 * 百川医疗AI诊断结果模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaichuanMedicalAIDiagnosis {

    private Long id;
    private String patientId;
    private String diagnosisContent;
    private Double confidence;
    private String modelVersion;
    private Date createdAt;
    private Date updatedAt;
    private String requestId;
    private Integer processingTime;
    private Boolean success;
    private String errorMessage;

}