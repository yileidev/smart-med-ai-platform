package com.medical.repository;

import com.medical.entity.VectorKnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VectorKnowledgeBaseRepository extends JpaRepository<VectorKnowledgeBase, Long> {

    /**
     * 根据知识类型查找
     */
    List<VectorKnowledgeBase> findByKnowledgeType(String knowledgeType);

    /**
     * 根据分类查找启用的知识
     */
    List<VectorKnowledgeBase> findByKnowledgeTypeAndIsActiveTrueOrderByAccuracyScoreDesc(String knowledgeType);

    /**
     * 根据科室查找相关知识
     */
    List<VectorKnowledgeBase> findByDepartmentAndIsActiveTrueOrderByUsageCountDesc(String department);

    /**
     * 查找最常用的知识条目
     */
    @Query("SELECT v FROM VectorKnowledgeBase v WHERE v.isActive = true ORDER BY v.usageCount DESC")
    List<VectorKnowledgeBase> findMostUsedKnowledge();

    /**
     * 查找准确率最高的知识条目
     */
    @Query("SELECT v FROM VectorKnowledgeBase v WHERE v.isActive = true AND v.accuracyScore >= :minScore ORDER BY v.accuracyScore DESC")
    List<VectorKnowledgeBase> findHighAccuracyKnowledge(@Param("minScore") Double minScore);

    /**
     * 根据症状关键词搜索
     */
    @Query("SELECT v FROM VectorKnowledgeBase v WHERE v.isActive = true AND v.symptoms LIKE %:keyword% ORDER BY v.accuracyScore DESC")
    List<VectorKnowledgeBase> findBySymptomKeyword(@Param("keyword") String keyword);

    /**
     * 统计各科室的知识数量
     */
    @Query("SELECT v.department, COUNT(v) FROM VectorKnowledgeBase v WHERE v.isActive = true GROUP BY v.department ORDER BY COUNT(v) DESC")
    List<Object[]> countByDepartment();

    /**
     * 查找需要更新的知识（使用次数高但准确率低）
     */
    @Query("SELECT v FROM VectorKnowledgeBase v WHERE v.isActive = true AND v.usageCount >= :minUsage AND v.accuracyScore < :maxScore ORDER BY v.usageCount DESC")
    List<VectorKnowledgeBase> findKnowledgeNeedingUpdate(@Param("minUsage") Integer minUsage, @Param("maxScore") Double maxScore);
}