package com.medical.service;

import com.medical.entity.Patient;
import com.medical.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {
    
    private final PatientRepository patientRepository;
    private final SystemLogService systemLogService;
    
    @Transactional
    public Patient savePatient(Patient patient) {
        Patient saved = patientRepository.save(patient);
        
        systemLogService.logUserAction(null, "system", "CREATE_PATIENT", "PATIENT",
            saved.getId().toString(), "创建患者信息：" + patient.getPatientName());
        
        log.info("创建患者信息：{}", patient.getPatientName());
        
        return saved;
    }
    
    public Patient findById(Long id) {
        return patientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("患者不存在"));
    }
    
    public Patient findByIdNumber(String idNumber) {
        return patientRepository.findByIdNumber(idNumber)
            .orElse(null);
    }
    
    public Patient findByPhoneNumber(String phoneNumber) {
        return patientRepository.findByPhoneNumber(phoneNumber)
            .orElse(null);
    }
    
    public Page<Patient> searchPatients(String name, String idNumber, String phone, Pageable pageable) {
        return patientRepository.findBySearchCriteria(name, idNumber, phone, pageable);
    }
    
    public Page<Patient> findByNameContaining(String name, Pageable pageable) {
        return patientRepository.findByPatientNameContaining(name, pageable);
    }
    
    @Transactional
    public Patient updatePatient(Patient patient) {
        Patient existing = findById(patient.getId());
        
        // 更新字段
        existing.setPatientName(patient.getPatientName());
        existing.setPhoneNumber(patient.getPhoneNumber());
        existing.setGender(patient.getGender());
        existing.setAge(patient.getAge());
        existing.setAddress(patient.getAddress());
        existing.setEmergencyContact(patient.getEmergencyContact());
        existing.setEmergencyPhone(patient.getEmergencyPhone());
        existing.setMedicalHistory(patient.getMedicalHistory());
        existing.setAllergies(patient.getAllergies());
        
        Patient updated = patientRepository.save(existing);
        
        systemLogService.logUserAction(null, "system", "UPDATE_PATIENT", "PATIENT",
            updated.getId().toString(), "更新患者信息：" + patient.getPatientName());
        
        return updated;
    }
    
    @Transactional
    public void deletePatient(Long id) {
        Patient patient = findById(id);
        patientRepository.delete(patient);
        
        systemLogService.logUserAction(null, "system", "DELETE_PATIENT", "PATIENT",
            id.toString(), "删除患者信息：" + patient.getPatientName());
    }
}