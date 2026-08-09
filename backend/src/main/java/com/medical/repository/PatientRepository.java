package com.medical.repository;

import com.medical.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    Optional<Patient> findByIdNumber(String idNumber);
    
    Optional<Patient> findByPhoneNumber(String phoneNumber);
    
    @Query("SELECT p FROM Patient p WHERE p.patientName LIKE %:name%")
    Page<Patient> findByPatientNameContaining(@Param("name") String name, Pageable pageable);
    
    @Query("SELECT p FROM Patient p WHERE " +
           "(:name IS NULL OR p.patientName LIKE %:name%) AND " +
           "(:idNumber IS NULL OR p.idNumber = :idNumber) AND " +
           "(:phone IS NULL OR p.phoneNumber = :phone)")
    Page<Patient> findBySearchCriteria(@Param("name") String name, 
                                      @Param("idNumber") String idNumber, 
                                      @Param("phone") String phone, 
                                      Pageable pageable);
}