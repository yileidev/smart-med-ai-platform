package com.medical.repository;

import com.medical.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR u.username LIKE %:keyword% OR u.fullName LIKE %:keyword% OR u.email LIKE %:keyword%) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:status IS NULL OR u.status = :status)")
    Page<User> findByConditions(@Param("keyword") String keyword,
                               @Param("role") User.Role role,
                               @Param("status") User.UserStatus status,
                               Pageable pageable);
    
    long countByStatus(User.UserStatus status);
    
    long countByRole(User.Role role);
    
    Page<User> findByRoleAndStatus(User.Role role, User.UserStatus status, Pageable pageable);
    
    /**
     * 根据角色查找所有用户
     */
    java.util.List<User> findByRole(User.Role role);
}