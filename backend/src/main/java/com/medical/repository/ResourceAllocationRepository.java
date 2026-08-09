package com.medical.repository;

import com.medical.entity.ResourceAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceAllocationRepository extends JpaRepository<ResourceAllocation, Long> {

    Optional<ResourceAllocation> findByTriageRecordId(Long triageRecordId);

    List<ResourceAllocation> findByAssignedDoctorIdOrderByCreatedAtDesc(Long doctorId);

    List<ResourceAllocation> findByAllocatedDepartmentOrderByPriorityScoreDesc(String department);

    @Query("SELECT ra FROM ResourceAllocation ra WHERE ra.status = :status " +
           "ORDER BY ra.priorityScore DESC, ra.createdAt ASC")
    List<ResourceAllocation> findByStatusOrderByPriority(@Param("status") ResourceAllocation.AllocationStatus status);

    @Query("SELECT ra FROM ResourceAllocation ra WHERE ra.createdAt >= :fromDate " +
           "AND ra.status IN ('PENDING', 'CONFIRMED') ORDER BY ra.priorityScore DESC")
    List<ResourceAllocation> findActiveAllocations(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT COUNT(ra) FROM ResourceAllocation ra WHERE ra.allocatedDepartment = :department " +
           "AND ra.status IN ('CONFIRMED', 'IN_PROGRESS')")
    long countActiveBeDepartment(@Param("department") String department);

    @Query("SELECT COUNT(ra) FROM ResourceAllocation ra WHERE ra.assignedDoctor.id = :doctorId " +
           "AND ra.status IN ('CONFIRMED', 'IN_PROGRESS')")
    long countActiveByDoctor(@Param("doctorId") Long doctorId);

    @Query("SELECT AVG(ra.estimatedWaitTime) FROM ResourceAllocation ra WHERE ra.createdAt >= :fromDate")
    Double getAverageWaitTime(@Param("fromDate") LocalDateTime fromDate);
}