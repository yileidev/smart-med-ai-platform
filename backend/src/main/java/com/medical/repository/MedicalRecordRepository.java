package com.medical.repository;

import com.medical.entity.MedicalRecord;
import com.medical.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    List<MedicalRecord> findByPatientIdOrderByVisitDateDesc(Long patientId);

    Page<MedicalRecord> findByPatientId(Long patientId, Pageable pageable);

    Optional<MedicalRecord> findTopByPatientIdOrderByVisitDateDesc(Long patientId);

    @Query("SELECT mr FROM MedicalRecord mr WHERE mr.patient.id = :patientId " +
           "AND mr.visitDate >= :fromDate ORDER BY mr.visitDate DESC")
    List<MedicalRecord> findRecentRecordsByPatient(@Param("patientId") Long patientId, 
                                                  @Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT mr FROM MedicalRecord mr WHERE mr.patient.id = :patientId " +
           "AND (LOWER(mr.diagnosis) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(mr.chiefComplaint) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY mr.visitDate DESC")
    List<MedicalRecord> findByPatientAndKeyword(@Param("patientId") Long patientId, 
                                               @Param("keyword") String keyword);

    long countByPatientId(Long patientId);

    @Query("SELECT COUNT(mr) FROM MedicalRecord mr WHERE mr.visitDate >= :fromDate")
    long countRecordsAfter(@Param("fromDate") LocalDateTime fromDate);
}