package com.medical.repository;

import com.medical.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * 根据权限代码查询
     */
    Optional<Permission> findByCode(String code);
    
    /**
     * 根据权限类型查询
     */
    List<Permission> findByTypeOrderBySortAsc(String type);
    
    /**
     * 根据父权限ID查询
     */
    List<Permission> findByParentIdOrderBySortAsc(Long parentId);
    
    /**
     * 查询所有启用的权限
     */
    List<Permission> findByEnabledTrueOrderBySortAsc();
}
