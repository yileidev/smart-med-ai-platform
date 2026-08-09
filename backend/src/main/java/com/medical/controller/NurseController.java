package com.medical.controller;

import com.medical.dto.ApiResponse;
import com.medical.entity.Patient;
import com.medical.entity.TriageRecord;
import com.medical.entity.User;
import com.medical.service.PatientService;
import com.medical.service.TriageService;
import com.medical.service.UserService;
import com.medical.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused") // REST API端点
@Tag(name = "护士工作台", description = "护士端功能接口")
@RestController
@RequestMapping("/nurse")
public class NurseController {
    
    private static final Logger log = LoggerFactory.getLogger(NurseController.class);
    
    private final TriageService triageService;
    private final PatientService patientService;
    private final UserService userService;
    private final EncryptionUtil encryptionUtil;
    private SimpMessagingTemplate messagingTemplate;
    private MqttClient mqttClient;
    private ObjectMapper objectMapper;
    
    @Autowired
    public NurseController(TriageService triageService, PatientService patientService, UserService userService, EncryptionUtil encryptionUtil) {
        this.triageService = triageService;
        this.patientService = patientService;
        this.userService = userService;
        this.encryptionUtil = encryptionUtil;
    }
    
    @Autowired
    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    @Autowired(required = false)
    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }
    
    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Operation(summary = "获取护士工作台统计数据")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNurseStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("newArrivals", triageService.countByStatus(TriageRecord.TriageStatus.WAITING));
        stats.put("pendingTriage", triageService.countByTriageLevelAndStatus(null, TriageRecord.TriageStatus.WAITING));
        stats.put("confirmedToday", triageService.countTodayCompleted());
        stats.put("avgResponseTime", "2.5分钟"); // 平均响应时间
        
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }
    
    @Operation(summary = "获取分诊队列")
    @GetMapping("/triage-queue")
    public ResponseEntity<Map<String, Object>> getTriageQueue() {
        try {
            // 获取待复核分诊列表
            java.util.List<TriageRecord> triageRecords = triageService.findPendingTriageRecords();
            
            // 转换为简化的Map格式，避免懒加载问题
            java.util.List<Map<String, Object>> patientList = triageRecords.stream()
                .map(record -> {
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("id", record.getId());
                    item.put("patientId", record.getPatient() != null ? record.getPatient().getId() : null);
                    item.put("patientName", record.getPatient() != null ? record.getPatient().getPatientName() : "未知");
                    item.put("age", record.getPatient() != null ? record.getPatient().getAge() : 0);
                    item.put("gender", record.getPatient() != null ? record.getPatient().getGender() : null);
                    // 身份证解密并脱敏显示
                    String idNumber = record.getPatient() != null ? record.getPatient().getIdNumber() : null;
                    item.put("idNumber", decryptAndMaskIdCard(idNumber));
                    // 手机号解密并脱敏显示
                    String phone = record.getPatient() != null ? record.getPatient().getPhoneNumber() : null;
                    item.put("phone", decryptAndMaskPhone(phone));
                    item.put("chiefComplaint", record.getChiefComplaint());
                    item.put("triageLevel", record.getTriageLevel());
                    item.put("triageScore", record.getTriageScore());
                    item.put("assignedDepartment", record.getAssignedDepartment());
                    item.put("arrivalTime", record.getArrivalTime());
                    item.put("vitalSigns", record.getVitalSigns());
                    item.put("status", record.getStatus());
                    item.put("aiDiagnosis", record.getAiDiagnosis());
                    item.put("dataSource", record.getDataSource());
                    item.put("edgeDeviceId", record.getEdgeDeviceId());
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", patientList
            ));
        } catch (Exception e) {
            log.error("获取分诊队列失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "获取分诊队列失败: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "患者登记")
    @PostMapping("/patients/register")
    public ResponseEntity<ApiResponse<Patient>> registerPatient(@RequestBody Patient patient) {
        Patient savedPatient = patientService.savePatient(patient);
        return ResponseEntity.ok(ApiResponse.success("患者登记成功", savedPatient));
    }
    
    @Operation(summary = "创建分诊记录")
    @PostMapping("/triage")
    public ResponseEntity<ApiResponse<TriageRecord>> createTriageRecord(
            @RequestBody TriageRecord triageRecord,
            Principal principal) {
        
        User nurse = userService.findByUsername(principal.getName());
        triageRecord.setAssignedNurse(nurse);
        
        TriageRecord saved = triageService.saveTriageRecord(triageRecord);
        return ResponseEntity.ok(ApiResponse.success("分诊记录创建成功", saved));
    }
    
    @Operation(summary = "更新分诊信息")
    @PutMapping("/triage/{triageId}")
    public ResponseEntity<ApiResponse<TriageRecord>> updateTriageRecord(
            @PathVariable Long triageId,
            @RequestBody TriageRecord triageRecord) {
        
        triageRecord.setId(triageId);
        TriageRecord updated = triageService.updateTriageRecord(triageRecord);
        return ResponseEntity.ok(ApiResponse.success("分诊信息更新成功", updated));
    }
    
    @Operation(summary = "获取分诊队列")
    @GetMapping("/triage/queue")
    public ResponseEntity<Page<TriageRecord>> getTriageQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer triageLevel) {
        
        Pageable pageable = PageRequest.of(page, size);
        TriageRecord.TriageStatus triageStatus = status != null ? 
            TriageRecord.TriageStatus.valueOf(status) : null;
        
        Page<TriageRecord> queue = triageService.findByFilters(
            department, triageStatus, triageLevel, pageable);
        
        return ResponseEntity.ok(queue);
    }
    
    @Operation(summary = "患者搜索")
    @GetMapping("/patients/search")
    public ResponseEntity<Page<Patient>> searchPatients(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Patient> patients = patientService.searchPatients(name, idNumber, phone, pageable);
        
        return ResponseEntity.ok(patients);
    }
    
    @Operation(summary = "更新患者生命体征")
    @PutMapping("/patients/{patientId}/vital-signs")
    public ResponseEntity<ApiResponse<TriageRecord>> updateVitalSigns(
            @PathVariable Long patientId,
            @RequestBody Map<String, Object> vitalSigns) {
        
        TriageRecord updated = triageService.updateVitalSigns(patientId, vitalSigns);
        return ResponseEntity.ok(ApiResponse.success("生命体征更新成功", updated));
    }
    
    @Operation(summary = "分配医生")
    @PostMapping("/triage/{triageId}/assign-doctor")
    public ResponseEntity<ApiResponse<TriageRecord>> assignDoctor(
            @PathVariable Long triageId,
            @RequestParam Long doctorId) {
        
        TriageRecord updated = triageService.assignDoctor(triageId, doctorId);
        return ResponseEntity.ok(ApiResponse.success("医生分配成功", updated));
    }
    
    @Operation(summary = "获取可分配的医生列表")
    @GetMapping("/doctors/available")
    public ResponseEntity<Page<User>> getAvailableDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String department) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> doctors = userService.findAvailableDoctors(department, pageable);
        
        return ResponseEntity.ok(doctors);
    }
    
    @Operation(summary = "紧急呼叫")
    @PostMapping("/emergency-call/{triageId}")
    public ResponseEntity<ApiResponse<String>> emergencyCall(
            @PathVariable Long triageId,
            @RequestBody Map<String, String> request) {
        
        String reason = request.get("reason");
        triageService.emergencyCall(triageId, reason);
        
        return ResponseEntity.ok(ApiResponse.success("紧急呼叫已发送"));
    }
    
    @Operation(summary = "更新患者信息")
    @PostMapping("/update-patient-info")
    public ResponseEntity<Map<String, Object>> updatePatientInfo(
            @RequestBody Map<String, Object> request,
            Principal principal) {
        try {
            Object patientIdObj = request.get("patientId");
            if (patientIdObj == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "患者ID不能为空"));
            }
            Long patientId = Long.valueOf(patientIdObj.toString());
            String chiefComplaint = (String) request.get("chiefComplaint");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> vitalSigns = (Map<String, Object>) request.get("vitalSigns");
            
            // 查找分诊记录
            TriageRecord triageRecord = triageService.findById(patientId);
            
            // 更新主诉
            if (chiefComplaint != null) {
                triageRecord.setChiefComplaint(chiefComplaint);
            }
            
            // 更新生命体征
            if (vitalSigns != null) {
                String vitalSignsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(vitalSigns);
                triageRecord.setVitalSigns(vitalSignsJson);
            }
            
            // 保存
            triageService.updateTriageRecord(triageRecord);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "患者信息更新成功",
                "data", triageRecord
            ));
        } catch (Exception e) {
            log.error("更新患者信息失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "更新失败: " + e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "护士复核分诊")
    @PostMapping("/triage/confirm")
    public ResponseEntity<ApiResponse<String>> confirmTriage(
            @RequestBody Map<String, Object> request) {
        try {
            // 空值检查 - 支持patientId或triageRecordId
            Object patientIdObj = request.get("patientId");
            Object triageRecordIdObj = request.get("triageRecordId");
            Object triageLevelObj = request.get("triageLevel");
            
            if (patientIdObj == null && triageRecordIdObj == null) {
                return ResponseEntity.ok(ApiResponse.error("分诊确认失败: 患者ID或分诊记录ID不能同时为空"));
            }
            if (triageLevelObj == null) {
                return ResponseEntity.ok(ApiResponse.error("分诊确认失败: 分诊等级不能为空"));
            }
            
            Integer triageLevel = Integer.valueOf(triageLevelObj.toString());
            String nurseNotes = request.get("nurseNotes") != null ? request.get("nurseNotes").toString() : "";
            
            // 接收护士端传递的AI诊断结果
            String aiDiagnosis = request.get("aiDiagnosis") != null ? request.get("aiDiagnosis").toString() : "";
            Double aiConfidence = request.get("aiConfidence") != null ? Double.valueOf(request.get("aiConfidence").toString()) : null;
            String recommendedDepartment = request.get("recommendedDepartment") != null ? request.get("recommendedDepartment").toString() : "";
            
            // 优先使用triageRecordId，如果没有则使用patientId
            TriageRecord record = null;
            Long patientId = null;
            
            if (triageRecordIdObj != null) {
                Long triageRecordId = Long.valueOf(triageRecordIdObj.toString());
                record = triageService.findById(triageRecordId);
                if (record.getPatient() != null) {
                    patientId = record.getPatient().getId();
                }
            } else if (patientIdObj != null) {
                patientId = Long.valueOf(patientIdObj.toString());
            }
            
            // 接收护士端传递的生命体征数据
            Object vitalSignsObj = request.get("vitalSigns");
            String vitalSignsJson = null;
            if (vitalSignsObj != null) {
                try {
                    if (vitalSignsObj instanceof String) {
                        vitalSignsJson = (String) vitalSignsObj;
                    } else {
                        vitalSignsJson = objectMapper.writeValueAsString(vitalSignsObj);
                    }
                } catch (Exception e) {
                    log.warn("解析生命体征数据失败", e);
                }
            }
            
            // 执行分诊确认
            if (record != null) {
                // 直接更新分诊记录
                record.setTriageLevel(triageLevel);
                record.setNurseNotes(nurseNotes);
                record.setStatus(TriageRecord.TriageStatus.CONFIRMED);
                record.setConfirmedTime(java.time.LocalDateTime.now());
                // 保存护士端传递的AI诊断结果
                if (aiDiagnosis != null && !aiDiagnosis.isEmpty()) {
                    record.setAiDiagnosis(aiDiagnosis);
                }
                if (aiConfidence != null) {
                    record.setAiConfidence(aiConfidence);
                }
                if (recommendedDepartment != null && !recommendedDepartment.isEmpty()) {
                    record.setAssignedDepartment(recommendedDepartment);
                }
                // 保存生命体征数据
                if (vitalSignsJson != null && !vitalSignsJson.isEmpty()) {
                    record.setVitalSigns(vitalSignsJson);
                    log.info("保存生命体征数据: {}", vitalSignsJson);
                }
                triageService.updateTriageRecord(record);
            } else if (patientId != null) {
                triageService.confirmTriage(patientId, triageLevel, nurseNotes);
            } else {
                return ResponseEntity.ok(ApiResponse.error("分诊确认失败: 找不到分诊记录"));
            }
            
            // 获取已确认的分诊记录（用于推送WebSocket）
            TriageRecord confirmedRecord = record;  // 使用上面已查找的record
            final Long finalPatientId = patientId;  // 创建final变量用于lambda
            
            if (confirmedRecord == null && finalPatientId != null) {
                // 如果上面没有查到，尝试从 CONFIRMED 状态查找
                confirmedRecord = triageService.findByStatus(TriageRecord.TriageStatus.CONFIRMED, PageRequest.of(0, 100))
                    .getContent().stream()
                    .filter(r -> r.getPatient() != null && r.getPatient().getId().equals(finalPatientId))
                    .findFirst()
                    .orElse(null);
            }
            
            if (confirmedRecord != null) {
                Patient patient = confirmedRecord.getPatient();
                
                // 发送WebSocket通知到医生端 - 包含完整患者数据和AI诊断
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "NEW_DIAGNOSIS");
                notification.put("patientId", finalPatientId);
                notification.put("triageRecordId", confirmedRecord.getId());
                notification.put("patientName", patient != null ? patient.getPatientName() : "待登记");
                notification.put("triageLevel", triageLevel);
                notification.put("chiefComplaint", confirmedRecord.getChiefComplaint());
                notification.put("vitalSigns", confirmedRecord.getVitalSigns());
                // 优先使用护士端传递的AI诊断，否则使用记录中的
                notification.put("aiDiagnosis", (aiDiagnosis != null && !aiDiagnosis.isEmpty()) ? aiDiagnosis : confirmedRecord.getAiDiagnosis());
                notification.put("aiConfidence", aiConfidence != null ? aiConfidence : confirmedRecord.getAiConfidence());
                notification.put("nurseNotes", nurseNotes);
                notification.put("assignedDepartment", confirmedRecord.getAssignedDepartment());
                notification.put("arrivalTime", confirmedRecord.getArrivalTime());
                notification.put("message", "新患者已经护士复核，等待医生诊断");
                
                messagingTemplate.convertAndSend("/topic/new-patients", notification);
                messagingTemplate.convertAndSend("/topic/doctor-diagnosis", notification);
            }
            
            return ResponseEntity.ok(ApiResponse.success("分诊确认成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("分诊确认失败: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "申请重新评估")
    @PostMapping("/request-reassessment")
    public ResponseEntity<Map<String, Object>> requestReassessment(
            @RequestBody Map<String, Object> request) {
        try {
            String deviceId = (String) request.get("deviceId");
            String patientName = (String) request.get("patientName");
            String reassessType = (String) request.get("reassessType");
            String reason = (String) request.get("reason");
            String urgency = (String) request.get("urgency");
            String nurseName = (String) request.get("nurseName");
            
            log.info("护士申请重新评估 - 患者: {}, 设备: {}, 类型: {}, 原因: {}", 
                patientName, deviceId, reassessType, reason);
            
            // 构建MQTT消息
            Map<String, Object> mqttPayload = new HashMap<>();
            mqttPayload.put("type", "REASSESSMENT_REQUEST");
            mqttPayload.put("patientName", patientName);
            mqttPayload.put("reassessType", reassessType);
            mqttPayload.put("reason", reason);
            mqttPayload.put("urgency", urgency);
            mqttPayload.put("nurseName", nurseName);
            mqttPayload.put("timestamp", java.time.LocalDateTime.now().toString());
            
            // 发送MQTT消息到边缘设备
            if (mqttClient != null && mqttClient.isConnected()) {
                String topic = "cloud/" + deviceId + "/reassessment";
                String payload = objectMapper.writeValueAsString(mqttPayload);
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(1);
                mqttClient.publish(topic, message);
                log.info("重新评估请求已发送到边缘设备: {}", topic);
            } else {
                log.warn("MQTT未连接，尝试通过WebSocket通知");
            }
            
            // 同时通过WebSocket通知前端
            mqttPayload.put("deviceId", deviceId);
            messagingTemplate.convertAndSend("/topic/device-command/" + deviceId, mqttPayload);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "重新评估申请已发送"
            ));
            
        } catch (Exception e) {
            log.error("发送重新评估请求失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "发送失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 解密并脱敏身份证号
     */
    private String decryptAndMaskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return "未登记";
        }
        // 判断是否是加密数据（Base64格式，长度>30）
        boolean isEncrypted = idCard.length() > 30 && idCard.matches("^[A-Za-z0-9+/=]+$");
        if (isEncrypted) {
            try {
                String decrypted = encryptionUtil.decrypt(idCard);
                // 解密成功，进行脱敏
                return encryptionUtil.maskIdCard(decrypted);
            } catch (Exception e) {
                log.warn("身份证解密失败，显示占位符");
                return "证件号待确认";
            }
        }
        // 未加密数据，直接脱敏
        return encryptionUtil.maskIdCard(idCard);
    }
    
    /**
     * 解密并脱敏手机号
     */
    private String decryptAndMaskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "未登记";
        }
        // 判断是否是加密数据（Base64格式，长度>20）
        boolean isEncrypted = phone.length() > 20 && phone.matches("^[A-Za-z0-9+/=]+$");
        if (isEncrypted) {
            try {
                String decrypted = encryptionUtil.decrypt(phone);
                // 解密成功，进行脱敏
                return encryptionUtil.maskPhone(decrypted);
            } catch (Exception e) {
                log.warn("手机号解密失败，显示占位符");
                return "手机号待确认";
            }
        }
        // 未加密数据，直接脱敏
        return encryptionUtil.maskPhone(phone);
    }
}