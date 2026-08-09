package com.medical.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.entity.Patient;
import com.medical.entity.TriageRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HL7医疗信息交换标准支持服务
 * 实现HL7 v2.x消息格式的生成和解析
 */
@Slf4j
@Service
public class HL7IntegrationService {

    private final ObjectMapper objectMapper;
    private static final String HL7_VERSION = "2.5";
    private static final String SENDING_APPLICATION = "MEDICAL_TRIAGE_SYSTEM";
    private static final String RECEIVING_APPLICATION = "HOSPITAL_HIS";

    public HL7IntegrationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 生成HL7 ADT^A01消息（患者入院）
     */
    public String generateADT_A01(Patient patient, TriageRecord triageRecord) {
        try {
            StringBuilder hl7Message = new StringBuilder();
            String timestamp = getCurrentTimestamp();
            String controlId = generateControlId();

            // MSH - 消息头段
            hl7Message.append("MSH|^~\\&|").append(SENDING_APPLICATION).append("|")
                    .append("EMERGENCY_DEPT|").append(RECEIVING_APPLICATION).append("|")
                    .append("MAIN_HIS|").append(timestamp).append("||ADT^A01^ADT_A01|")
                    .append(controlId).append("|P|").append(HL7_VERSION).append("\r");

            // EVN - 事件类型段
            hl7Message.append("EVN|A01|").append(timestamp).append("|||")
                    .append(triageRecord.getAssignedNurse() != null ? 
                            triageRecord.getAssignedNurse().getUsername() : "SYSTEM")
                    .append("\r");

            // PID - 患者标识段
            hl7Message.append("PID|1||").append(patient.getId()).append("^^^HOSPITAL_MRN^MR||")
                    .append(formatPatientName(patient.getPatientName())).append("||")
                    .append(formatDate(patient.getDateOfBirth())).append("|")
                    .append(patient.getGender() != null ? patient.getGender() : "U").append("|||")
                    .append(formatAddress(patient)).append("|||")
                    .append(patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "")
                    .append("|\r");

            // PV1 - 患者访问段
            hl7Message.append("PV1|1|E|").append("EMERGENCY^ER^BED").append(triageRecord.getId())
                    .append("^^^^^R|||||||||||")
                    .append(triageRecord.getAssignedDoctor() != null ? 
                            triageRecord.getAssignedDoctor().getUsername() : "")
                    .append("|").append(triageRecord.getTriageLevel()).append("|||")
                    .append(formatDateTime(triageRecord.getArrivalTime()))
                    .append("||||||||||||||||||||||||||||")
                    .append(formatDateTime(triageRecord.getCreatedAt())).append("\r");

            // DG1 - 诊断段
            if (triageRecord.getAiDiagnosis() != null) {
                hl7Message.append("DG1|1||").append(triageRecord.getAiDiagnosis())
                        .append("^").append(triageRecord.getAiDiagnosis()).append("^ICD10|")
                        .append("|A|").append(timestamp).append("\r");
            }

            // OBX - 观察结果段（生命体征）
            hl7Message.append(generateVitalSignsOBX(triageRecord.getVitalSigns()));

            log.info("生成HL7 ADT^A01消息，患者ID: {}", patient.getId());
            return hl7Message.toString();

        } catch (Exception e) {
            log.error("生成HL7消息失败", e);
            return null;
        }
    }

    /**
     * 生成HL7 ORM^O01消息（医嘱请求）
     */
    public String generateORM_O01(TriageRecord triageRecord, String orderDetails) {
        try {
            StringBuilder hl7Message = new StringBuilder();
            String timestamp = getCurrentTimestamp();
            String controlId = generateControlId();

            // MSH - 消息头段
            hl7Message.append("MSH|^~\\&|").append(SENDING_APPLICATION).append("|")
                    .append("EMERGENCY_DEPT|").append(RECEIVING_APPLICATION).append("|")
                    .append("ORDER_SYSTEM|").append(timestamp).append("||ORM^O01^ORM_O01|")
                    .append(controlId).append("|P|").append(HL7_VERSION).append("\r");

            // PID - 患者标识段
            Patient patient = triageRecord.getPatient();
            hl7Message.append("PID|1||").append(patient.getId()).append("^^^HOSPITAL_MRN^MR||")
                    .append(formatPatientName(patient.getPatientName())).append("||")
                    .append(formatDate(patient.getDateOfBirth())).append("|")
                    .append(patient.getGender() != null ? patient.getGender() : "U").append("\r");

            // ORC - 通用医嘱控制段
            String orderId = "ORD" + System.currentTimeMillis();
            hl7Message.append("ORC|NW|").append(orderId).append("||||||")
                    .append(timestamp).append("|||")
                    .append(triageRecord.getAssignedDoctor() != null ? 
                            triageRecord.getAssignedDoctor().getUsername() : "")
                    .append("||||").append(triageRecord.getAssignedDepartment()).append("\r");

            // OBR - 观察请求段
            hl7Message.append("OBR|1|").append(orderId).append("||")
                    .append("EMERGENCY_WORKUP^急诊检查^LOCAL|||")
                    .append(timestamp).append("||||||||")
                    .append(triageRecord.getAssignedDoctor() != null ? 
                            triageRecord.getAssignedDoctor().getUsername() : "")
                    .append("||||").append(timestamp).append("||F\r");

            log.info("生成HL7 ORM^O01消息，分诊记录ID: {}", triageRecord.getId());
            return hl7Message.toString();

        } catch (Exception e) {
            log.error("生成医嘱HL7消息失败", e);
            return null;
        }
    }

    /**
     * 解析HL7消息
     */
    public Map<String, Object> parseHL7Message(String hl7Message) {
        Map<String, Object> parsedData = new HashMap<>();
        
        try {
            String[] segments = hl7Message.split("\r");
            
            for (String segment : segments) {
                if (segment.startsWith("MSH")) {
                    parsedData.putAll(parseMSH(segment));
                } else if (segment.startsWith("PID")) {
                    parsedData.putAll(parsePID(segment));
                } else if (segment.startsWith("PV1")) {
                    parsedData.putAll(parsePV1(segment));
                } else if (segment.startsWith("OBX")) {
                    parsedData.putAll(parseOBX(segment));
                }
            }
            
            log.info("HL7消息解析完成");
            return parsedData;
            
        } catch (Exception e) {
            log.error("HL7消息解析失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 生成HL7 ACK确认消息
     */
    public String generateACK(String originalControlId, String ackCode) {
        StringBuilder ack = new StringBuilder();
        String timestamp = getCurrentTimestamp();
        String controlId = generateControlId();
        
        // MSH段
        ack.append("MSH|^~\\&|").append(RECEIVING_APPLICATION).append("|")
                .append("MAIN_HIS|").append(SENDING_APPLICATION).append("|")
                .append("EMERGENCY_DEPT|").append(timestamp).append("||ACK^A01^ACK|")
                .append(controlId).append("|P|").append(HL7_VERSION).append("\r");
        
        // MSA段
        ack.append("MSA|").append(ackCode).append("|").append(originalControlId)
                .append("|").append(getAckMessage(ackCode)).append("\r");
        
        return ack.toString();
    }

    /**
     * 验证HL7消息格式
     */
    public boolean validateHL7Message(String hl7Message) {
        if (hl7Message == null || hl7Message.trim().isEmpty()) {
            return false;
        }
        
        // 基本格式验证
        if (!hl7Message.startsWith("MSH")) {
            log.warn("HL7消息必须以MSH段开始");
            return false;
        }
        
        String[] segments = hl7Message.split("\r");
        for (String segment : segments) {
            if (segment.length() < 3) {
                log.warn("无效的HL7段: {}", segment);
                return false;
            }
        }
        
        return true;
    }

    // 私有辅助方法

    private String generateVitalSignsOBX(String vitalSignsJson) {
        StringBuilder obx = new StringBuilder();
        
        try {
            if (vitalSignsJson != null && !vitalSignsJson.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> vitalSigns = objectMapper.readValue(vitalSignsJson, Map.class);
                int sequence = 1;
                
                for (Map.Entry<String, Object> entry : vitalSigns.entrySet()) {
                    obx.append("OBX|").append(sequence++).append("|NM|")
                            .append(getVitalSignCode(entry.getKey())).append("^")
                            .append(getVitalSignName(entry.getKey())).append("^LN||")
                            .append(entry.getValue()).append("|")
                            .append(getVitalSignUnit(entry.getKey())).append("|||||F\r");
                }
            }
        } catch (Exception e) {
            log.warn("生命体征OBX段生成失败", e);
        }
        
        return obx.toString();
    }

    private Map<String, Object> parseMSH(String mshSegment) {
        String[] fields = mshSegment.split("\\|");
        Map<String, Object> msh = new HashMap<>();
        
        if (fields.length > 3) msh.put("sendingApplication", fields[3]);
        if (fields.length > 5) msh.put("receivingApplication", fields[5]);
        if (fields.length > 7) msh.put("timestamp", fields[7]);
        if (fields.length > 9) msh.put("messageType", fields[9]);
        if (fields.length > 10) msh.put("controlId", fields[10]);
        
        return msh;
    }

    private Map<String, Object> parsePID(String pidSegment) {
        String[] fields = pidSegment.split("\\|");
        Map<String, Object> pid = new HashMap<>();
        
        if (fields.length > 3) pid.put("patientId", fields[3].split("\\^")[0]);
        if (fields.length > 5) pid.put("patientName", fields[5]);
        if (fields.length > 7) pid.put("birthDate", fields[7]);
        if (fields.length > 8) pid.put("gender", fields[8]);
        
        return pid;
    }

    private Map<String, Object> parsePV1(String pv1Segment) {
        String[] fields = pv1Segment.split("\\|");
        Map<String, Object> pv1 = new HashMap<>();
        
        if (fields.length > 2) pv1.put("patientClass", fields[2]);
        if (fields.length > 3) pv1.put("assignedLocation", fields[3]);
        if (fields.length > 44) pv1.put("admitDateTime", fields[44]);
        
        return pv1;
    }

    private Map<String, Object> parseOBX(String obxSegment) {
        String[] fields = obxSegment.split("\\|");
        Map<String, Object> obx = new HashMap<>();
        
        if (fields.length > 3) obx.put("observationId", fields[3]);
        if (fields.length > 5) obx.put("observationValue", fields[5]);
        if (fields.length > 6) obx.put("units", fields[6]);
        
        return obx;
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String generateControlId() {
        return "MSG" + System.currentTimeMillis();
    }

    private String formatPatientName(String name) {
        return name != null ? name.replace(" ", "^") : "";
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "";
    }

    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) : "";
    }

    private String formatAddress(Patient patient) {
        return patient.getAddress() != null ? patient.getAddress().replace(",", "^") : "";
    }

    private String getVitalSignCode(String key) {
        switch (key.toLowerCase()) {
            case "temperature": return "8310-5";
            case "heartrate": return "8867-4";
            case "bloodoxygen": return "59408-5";
            case "systolicbp": return "8480-6";
            case "diastolicbp": return "8462-4";
            default: return "33747-0";
        }
    }

    private String getVitalSignName(String key) {
        switch (key.toLowerCase()) {
            case "temperature": return "体温";
            case "heartrate": return "心率";
            case "bloodoxygen": return "血氧饱和度";
            case "systolicbp": return "收缩压";
            case "diastolicbp": return "舒张压";
            default: return "其他生命体征";
        }
    }

    private String getVitalSignUnit(String key) {
        switch (key.toLowerCase()) {
            case "temperature": return "Cel";
            case "heartrate": return "/min";
            case "bloodoxygen": return "%";
            case "systolicbp": case "diastolicbp": return "mm[Hg]";
            default: return "";
        }
    }

    private String getAckMessage(String ackCode) {
        switch (ackCode) {
            case "AA": return "Application Accept";
            case "AE": return "Application Error";
            case "AR": return "Application Reject";
            default: return "Unknown";
        }
    }
}