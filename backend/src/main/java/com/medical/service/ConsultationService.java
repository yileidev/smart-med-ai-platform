package com.medical.service;

import com.medical.entity.Consultation;
import com.medical.repository.ConsultationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final WebSocketService webSocketService;
    private final NotificationService notificationService;

    public ConsultationService(ConsultationRepository consultationRepository,
                                WebSocketService webSocketService,
                                NotificationService notificationService) {
        this.consultationRepository = consultationRepository;
        this.webSocketService = webSocketService;
        this.notificationService = notificationService;
    }

    /**
     * 创建会诊申请
     */
    public Consultation createConsultation(Consultation consultation) {
        consultation.setStatus("PENDING");
        Consultation saved = consultationRepository.save(consultation);
        
        // 发送WebSocket通知
        webSocketService.notifyConsultationRequest(saved.getId(), 
            consultation.getConsultingDoctorName());
        
        // 创建系统通知
        notificationService.createNotification(
            consultation.getConsultingDoctorId(),
            consultation.getConsultingDoctorName(),
            "CONSULTATION",
            "新会诊请求",
            consultation.getRequestingDoctorName() + "请求会诊",
            "HIGH"
        );
        
        return saved;
    }

    /**
     * 接受会诊
     */
    public Consultation acceptConsultation(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new RuntimeException("会诊记录不存在"));
        
        consultation.setStatus("ACCEPTED");
        consultation.setAcceptTime(LocalDateTime.now());
        
        return consultationRepository.save(consultation);
    }

    /**
     * 完成会诊
     */
    public Consultation completeConsultation(Long consultationId, String opinion) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new RuntimeException("会诊记录不存在"));
        
        consultation.setStatus("COMPLETED");
        consultation.setOpinion(opinion);
        consultation.setCompleteTime(LocalDateTime.now());
        
        Consultation saved = consultationRepository.save(consultation);
        
        // 通知申请医生
        notificationService.createNotification(
            consultation.getRequestingDoctorId(),
            consultation.getRequestingDoctorName(),
            "CONSULTATION",
            "会诊已完成",
            "会诊意见已提交",
            "NORMAL"
        );
        
        return saved;
    }

    /**
     * 获取医生的会诊申请
     */
    public List<Consultation> getDoctorConsultations(Long doctorId) {
        List<Consultation> requesting = consultationRepository
            .findByRequestingDoctorIdOrderByRequestTimeDesc(doctorId);
        List<Consultation> consulting = consultationRepository
            .findByConsultingDoctorIdOrderByRequestTimeDesc(doctorId);
        
        requesting.addAll(consulting);
        return requesting;
    }

    /**
     * 获取待处理的会诊
     */
    public List<Consultation> getPendingConsultations() {
        return consultationRepository.findByStatusOrderByRequestTimeDesc("PENDING");
    }
}
