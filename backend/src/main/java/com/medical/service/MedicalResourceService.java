package com.medical.service;

import com.medical.entity.MedicalResource;
import com.medical.repository.MedicalResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicalResourceService {
    
    private final MedicalResourceRepository resourceRepository;
    
    public Page<MedicalResource> findResources(String keyword, 
                                             MedicalResource.ResourceType type,
                                             MedicalResource.ResourceStatus status,
                                             Pageable pageable) {
        return resourceRepository.findByConditions(keyword, type, status, pageable);
    }
    
    public Optional<MedicalResource> findById(Long id) {
        return resourceRepository.findById(id);
    }
    
    @Transactional
    public MedicalResource createResource(MedicalResource resource) {
        if (resource.getAvailableQuantity() == null) {
            resource.setAvailableQuantity(resource.getTotalQuantity());
        }
        return resourceRepository.save(resource);
    }
    
    @Transactional
    public MedicalResource updateResource(Long id, MedicalResource resourceDetails) {
        MedicalResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
        
        resource.setName(resourceDetails.getName());
        resource.setDescription(resourceDetails.getDescription());
        resource.setType(resourceDetails.getType());
        resource.setStatus(resourceDetails.getStatus());
        resource.setLocation(resourceDetails.getLocation());
        resource.setTotalQuantity(resourceDetails.getTotalQuantity());
        resource.setAvailableQuantity(resourceDetails.getAvailableQuantity());
        resource.setUnitPrice(resourceDetails.getUnitPrice());
        resource.setMaintenanceDate(resourceDetails.getMaintenanceDate());
        
        return resourceRepository.save(resource);
    }
    
    @Transactional
    public void deleteResource(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new RuntimeException("资源不存在");
        }
        resourceRepository.deleteById(id);
    }
    
    @Transactional
    public void updateResourceStatus(Long id, MedicalResource.ResourceStatus status) {
        MedicalResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资源不存在"));
        resource.setStatus(status);
        resourceRepository.save(resource);
    }
    
    public long countByStatus(MedicalResource.ResourceStatus status) {
        return resourceRepository.countByStatus(status);
    }
    
    public long countByType(MedicalResource.ResourceType type) {
        return resourceRepository.countByType(type);
    }
    
    public Long sumAvailableQuantityByStatus(MedicalResource.ResourceStatus status) {
        return resourceRepository.sumAvailableQuantityByStatus(status);
    }
}