package com.medical.repository;

import com.medical.entity.MedicalResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalResourceRepository extends JpaRepository<MedicalResource, Long> {
    
    @Query("SELECT r FROM MedicalResource r WHERE " +
           "(:keyword IS NULL OR r.name LIKE %:keyword% OR r.description LIKE %:keyword%) " +
           "AND (:type IS NULL OR r.type = :type) " +
           "AND (:status IS NULL OR r.status = :status)")
    Page<MedicalResource> findByConditions(@Param("keyword") String keyword,
                                         @Param("type") MedicalResource.ResourceType type,
                                         @Param("status") MedicalResource.ResourceStatus status,
                                         Pageable pageable);
    
    long countByStatus(MedicalResource.ResourceStatus status);
    
    long countByType(MedicalResource.ResourceType type);
    
    @Query("SELECT SUM(r.availableQuantity) FROM MedicalResource r WHERE r.status = :status")
    Long sumAvailableQuantityByStatus(@Param("status") MedicalResource.ResourceStatus status);
}