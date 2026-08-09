package com.medical.repository;

import com.medical.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    
    /**
     * 根据申请医生ID查询会诊记录
     */
    List<Consultation> findByRequestingDoctorIdOrderByRequestTimeDesc(Long doctorId);
    
    /**
     * 根据会诊医生ID查询会诊记录
     */
    List<Consultation> findByConsultingDoctorIdOrderByRequestTimeDesc(Long doctorId);
    
    /**
     * 根据状态查询会诊记录
     */
    List<Consultation> findByStatusOrderByRequestTimeDesc(String status);
    
    /**
     * 根据患者ID查询会诊记录
     */
    List<Consultation> findByPatientIdOrderByRequestTimeDesc(Long patientId);
    
    /**
     * 根据分诊记录ID查询会诊
     */
    List<Consultation> findByTriageRecordId(Long triageRecordId);
}
