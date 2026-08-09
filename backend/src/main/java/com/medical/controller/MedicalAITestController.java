package com.medical.controller;

import com.medical.service.BaichuanMedicalAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 医疗AI模型测试控制器
 * 用于测试百川智能医疗大模型的集成和功能
 */
@SuppressWarnings("unused") // REST API端点
@RestController
@RequestMapping("/medical-ai")
@Tag(name = "医疗AI测试", description = "百川智能医疗大模型测试接口")
public class MedicalAITestController {

    private final BaichuanMedicalAIService baichuanMedicalAIService;
    
    public MedicalAITestController(BaichuanMedicalAIService baichuanMedicalAIService) {
        this.baichuanMedicalAIService = baichuanMedicalAIService;
    }

    @GetMapping("/status")
    @Operation(summary = "检查医疗AI模型状态")
    public ResponseEntity<Map<String, Object>> checkAIStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isAvailable = baichuanMedicalAIService.isAPIAvailable();
            response.put("status", isAvailable ? "online" : "offline");
            response.put("model", "百川智能医疗大模型");
            response.put("version", "baichuan-medical-v1");
            response.put("message", isAvailable ? "医疗AI模型运行正常" : "医疗AI模型暂时不可用");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "检查AI状态时发生错误：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/test-diagnosis")
    @Operation(summary = "测试医疗诊断功能")
    public ResponseEntity<Map<String, Object>> testDiagnosis(@RequestBody TestDiagnosisRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String diagnosisResult = baichuanMedicalAIService.analyzeMedicalSymptoms(
                request.getPatientInfo(),
                request.getSymptoms(),
                request.getVitalSigns(),
                request.getMedicalHistory()
            );
            
            response.put("success", true);
            response.put("diagnosis", diagnosisResult);
            response.put("model", "baichuan-medical-v1");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "诊断测试失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/test-triage")
    @Operation(summary = "测试分诊评估功能")
    public ResponseEntity<Map<String, Object>> testTriageAssessment(@RequestBody TestTriageRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String triageResult = baichuanMedicalAIService.assessTriageLevel(
                request.getSymptoms(),
                request.getVitalSigns(),
                request.getPatientAge(),
                request.getConsciousness()
            );
            
            response.put("success", true);
            response.put("triageAssessment", triageResult);
            response.put("model", "baichuan-medical-v1");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "分诊评估测试失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/test-drug-interaction")
    @Operation(summary = "测试药物相互作用检查")
    public ResponseEntity<Map<String, Object>> testDrugInteraction(@RequestBody TestDrugInteractionRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String interactionResult = baichuanMedicalAIService.checkDrugInteractions(
                request.getCurrentMedications(),
                request.getProposedMedication(),
                request.getPatientCondition()
            );
            
            response.put("success", true);
            response.put("drugInteractionCheck", interactionResult);
            response.put("model", "baichuan-medical-v1");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "药物相互作用检查失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 请求数据类
    public static class TestDiagnosisRequest {
        private String patientInfo;
        private String symptoms;
        private String vitalSigns;
        private String medicalHistory;

        // Getters and Setters
        public String getPatientInfo() { return patientInfo; }
        public void setPatientInfo(String patientInfo) { this.patientInfo = patientInfo; }
        
        public String getSymptoms() { return symptoms; }
        public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
        
        public String getVitalSigns() { return vitalSigns; }
        public void setVitalSigns(String vitalSigns) { this.vitalSigns = vitalSigns; }
        
        public String getMedicalHistory() { return medicalHistory; }
        public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
    }

    public static class TestTriageRequest {
        private String symptoms;
        private String vitalSigns;
        private String patientAge;
        private String consciousness;

        // Getters and Setters
        public String getSymptoms() { return symptoms; }
        public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
        
        public String getVitalSigns() { return vitalSigns; }
        public void setVitalSigns(String vitalSigns) { this.vitalSigns = vitalSigns; }
        
        public String getPatientAge() { return patientAge; }
        public void setPatientAge(String patientAge) { this.patientAge = patientAge; }
        
        public String getConsciousness() { return consciousness; }
        public void setConsciousness(String consciousness) { this.consciousness = consciousness; }
    }

    public static class TestDrugInteractionRequest {
        private String currentMedications;
        private String proposedMedication;
        private String patientCondition;

        // Getters and Setters
        public String getCurrentMedications() { return currentMedications; }
        public void setCurrentMedications(String currentMedications) { this.currentMedications = currentMedications; }
        
        public String getProposedMedication() { return proposedMedication; }
        public void setProposedMedication(String proposedMedication) { this.proposedMedication = proposedMedication; }
        
        public String getPatientCondition() { return patientCondition; }
        public void setPatientCondition(String patientCondition) { this.patientCondition = patientCondition; }
    }
}