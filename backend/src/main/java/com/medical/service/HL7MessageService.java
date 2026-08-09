package com.medical.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.entity.HL7MessageMapping;
import com.medical.entity.Patient;
import com.medical.entity.TriageRecord;
import com.medical.repository.HL7MessageMappingRepository;
import com.medical.repository.PatientRepository;
import com.medical.repository.TriageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HL7消息处理服务
 * 处理医疗信息交换标准数据的解析、存储和映射
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HL7MessageService {

    private final HL7MessageMappingRepository hl7Repository;
    private final PatientRepository patientRepository;
    private final TriageRecordRepository triageRepository;
    private final ObjectMapper objectMapper;

    /**
     * 处理HL7 ADT消息 (患者管理)
     */
    @Transactional
    public HL7MessageMapping processADTMessage(String rawMessage) {
        try {
            log.info("开始处理HL7 ADT消息");

            // 解析HL7消息
            Map<String, Object> parsedData = parseHL7Message(rawMessage);
            
            // 创建或更新患者信息
            Patient patient = createOrUpdatePatient(parsedData);
            
            // 创建HL7映射记录
            HL7MessageMapping mapping = HL7MessageMapping.builder()
                    .messageId(generateMessageId())
                    .messageType("ADT")
                    .patientId(patient.getId())
                    .rawMessage(rawMessage)
                    .parsedData(objectMapper.writeValueAsString(parsedData))
                    .sendingFacility(getStringValue(parsedData, "sendingFacility"))
                    .receivingFacility(getStringValue(parsedData, "receivingFacility"))
                    .status(HL7MessageMapping.MessageStatus.PROCESSED)
                    .processedTime(LocalDateTime.now())
                    .build();

            return hl7Repository.save(mapping);

        } catch (Exception e) {
            log.error("处理HL7 ADT消息失败", e);
            return createErrorMapping(rawMessage, "ADT", e.getMessage());
        }
    }

    /**
     * 处理HL7 ORU消息 (观察结果)
     */
    @Transactional
    public HL7MessageMapping processORUMessage(String rawMessage, Long triageRecordId) {
        try {
            log.info("开始处理HL7 ORU消息，分诊记录ID: {}", triageRecordId);

            // 解析HL7消息
            Map<String, Object> parsedData = parseHL7Message(rawMessage);
            
            // 更新分诊记录的检查结果
            updateTriageWithLabResults(triageRecordId, parsedData);
            
            // 创建HL7映射记录
            HL7MessageMapping mapping = HL7MessageMapping.builder()
                    .messageId(generateMessageId())
                    .messageType("ORU")
                    .triageRecordId(triageRecordId)
                    .rawMessage(rawMessage)
                    .parsedData(objectMapper.writeValueAsString(parsedData))
                    .sendingFacility(getStringValue(parsedData, "sendingFacility"))
                    .receivingFacility(getStringValue(parsedData, "receivingFacility"))
                    .status(HL7MessageMapping.MessageStatus.PROCESSED)
                    .processedTime(LocalDateTime.now())
                    .build();

            return hl7Repository.save(mapping);

        } catch (Exception e) {
            log.error("处理HL7 ORU消息失败", e);
            return createErrorMapping(rawMessage, "ORU", e.getMessage());
        }
    }

    /**
     * 生成HL7格式的患者信息消息
     */
    public String generateHL7ADTMessage(Patient patient) {
        try {
            StringBuilder hl7Message = new StringBuilder();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            
            // MSH段 - 消息头
            hl7Message.append("MSH|^~\\&|MEDICAL_SYSTEM|HOSPITAL|HIS|HOSPITAL|")
                     .append(timestamp).append("||ADT^A04^ADT_A01|")
                     .append(generateMessageId()).append("|P|2.5|||AL|||\r");

            // EVN段 - 事件类型
            hl7Message.append("EVN|A04|").append(timestamp).append("|||\r");

            // PID段 - 患者识别
            hl7Message.append("PID|1||").append(patient.getId()).append("^^^HOSPITAL^MR||")
                     .append(patient.getPatientName()).append("^||")
                     .append(formatDateOfBirth(patient.getDateOfBirth())).append("|")
                     .append(patient.getGender()).append("|||")
                     .append(patient.getAddress() != null ? patient.getAddress() : "")
                     .append("||").append(patient.getPhoneNumber() != null ? patient.getPhoneNumber() : "")
                     .append("|||||||||||||\r");

            // PV1段 - 患者访问
            hl7Message.append("PV1|1|E|ER^001^01^HOSPITAL||||||||||||||||")
                     .append(patient.getId()).append("^V|||||||||||||||||||||||")
                     .append(timestamp).append("|\r");

            return hl7Message.toString();

        } catch (Exception e) {
            log.error("生成HL7 ADT消息失败", e);
            return null;
        }
    }

    /**
     * 解析HL7消息
     */
    private Map<String, Object> parseHL7Message(String rawMessage) {
        Map<String, Object> parsedData = new HashMap<>();
        
        try {
            String[] segments = rawMessage.split("\\r");
            
            for (String segment : segments) {
                String[] fields = segment.split("\\|");
                if (fields.length > 0) {
                    String segmentType = fields[0];
                    
                    switch (segmentType) {
                        case "MSH":
                            parsedData.put("messageType", fields.length > 8 ? fields[8] : "");
                            parsedData.put("sendingFacility", fields.length > 3 ? fields[3] : "");
                            parsedData.put("receivingFacility", fields.length > 5 ? fields[5] : "");
                            parsedData.put("timestamp", fields.length > 6 ? fields[6] : "");
                            break;
                            
                        case "PID":
                            Map<String, Object> patientInfo = new HashMap<>();
                            patientInfo.put("patientId", fields.length > 3 ? fields[3] : "");
                            patientInfo.put("patientName", fields.length > 5 ? fields[5] : "");
                            patientInfo.put("dateOfBirth", fields.length > 7 ? fields[7] : "");
                            patientInfo.put("gender", fields.length > 8 ? fields[8] : "");
                            patientInfo.put("address", fields.length > 11 ? fields[11] : "");
                            patientInfo.put("phoneNumber", fields.length > 13 ? fields[13] : "");
                            parsedData.put("patient", patientInfo);
                            break;
                            
                        case "OBX":
                            // 观察结果
                            if (!parsedData.containsKey("observations")) {
                                parsedData.put("observations", new HashMap<String, Object>());
                            }
                            @SuppressWarnings("unchecked")
                            Map<String, Object> observations = (Map<String, Object>) parsedData.get("observations");
                            if (fields.length > 5) {
                                observations.put(fields[3], fields[5]); // 观察标识符 -> 观察值
                            }
                            break;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("解析HL7消息失败", e);
            parsedData.put("error", "解析失败: " + e.getMessage());
        }
        
        return parsedData;
    }

    /**
     * 创建或更新患者信息
     */
    private Patient createOrUpdatePatient(Map<String, Object> parsedData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> patientInfo = (Map<String, Object>) parsedData.get("patient");
        
        if (patientInfo == null) {
            throw new IllegalArgumentException("患者信息不能为空");
        }

        String patientIdStr = getStringValue(patientInfo, "patientId");
        Long patientId = null;
        
        try {
            patientId = Long.parseLong(patientIdStr);
        } catch (NumberFormatException e) {
            // 如果不能解析为数字，则创建新患者
        }

        Patient patient;
        if (patientId != null) {
            patient = patientRepository.findById(patientId).orElse(new Patient());
        } else {
            patient = new Patient();
        }

        // 更新患者信息
        patient.setPatientName(getStringValue(patientInfo, "patientName"));
        patient.setGenderFromString(getStringValue(patientInfo, "gender"));
        patient.setPhoneNumber(getStringValue(patientInfo, "phoneNumber"));
        patient.setAddress(getStringValue(patientInfo, "address"));

        return patientRepository.save(patient);
    }

    /**
     * 更新分诊记录的检查结果
     */
    private void updateTriageWithLabResults(Long triageRecordId, Map<String, Object> parsedData) {
        TriageRecord triageRecord = triageRepository.findById(triageRecordId)
                .orElseThrow(() -> new IllegalArgumentException("分诊记录不存在"));

        @SuppressWarnings("unchecked")
        Map<String, Object> observations = (Map<String, Object>) parsedData.get("observations");
        
        if (observations != null) {
            try {
                String labResults = objectMapper.writeValueAsString(observations);
                triageRecord.setLabResults(labResults);
                triageRepository.save(triageRecord);
            } catch (Exception e) {
                log.error("更新分诊记录检查结果失败", e);
            }
        }
    }

    /**
     * 创建错误映射记录
     */
    private HL7MessageMapping createErrorMapping(String rawMessage, String messageType, String errorMessage) {
        HL7MessageMapping mapping = HL7MessageMapping.builder()
                .messageId(generateMessageId())
                .messageType(messageType)
                .rawMessage(rawMessage)
                .status(HL7MessageMapping.MessageStatus.ERROR)
                .errorMessage(errorMessage)
                .processedTime(LocalDateTime.now())
                .build();

        return hl7Repository.save(mapping);
    }

    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return "MSG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * 格式化日期
     */
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    
    /**
     * 格式化出生日期
     */
    private String formatDateOfBirth(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 安全获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}