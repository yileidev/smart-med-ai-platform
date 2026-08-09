package com.medical.util;

import com.medical.enums.TriageLevel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 分诊工具类
 * 提供基于国家卫健委《急诊预检分诊专家共识》(2018年版)的标准化分诊功能
 * 
 * @author Medical System
 * @version 2.0
 */
@Component
public class TriageUtil {
    
    /**
     * 获取分诊等级文本描述
     * 
     * @param levelCode 分诊等级代码 (1-4)
     * @return 分诊等级文本，如 "Ⅰ级-急危"
     */
    public static String getTriageLevelText(Integer levelCode) {
        TriageLevel level = TriageLevel.fromCode(levelCode);
        return level != null ? level.getName() : "未分诊";
    }
    
    /**
     * 获取分诊等级颜色标识
     * 
     * @param levelCode 分诊等级代码
     * @return 颜色标识，如 "红色"
     */
    public static String getTriageColor(Integer levelCode) {
        TriageLevel level = TriageLevel.fromCode(levelCode);
        return level != null ? level.getColor() : "灰色";
    }
    
    /**
     * 获取响应时限
     * 
     * @param levelCode 分诊等级代码
     * @return 响应时限，如 "即刻"、"10分钟内"
     */
    public static String getResponseTime(Integer levelCode) {
        TriageLevel level = TriageLevel.fromCode(levelCode);
        return level != null ? level.getResponseTime() : "未定义";
    }
    
    /**
     * 获取分诊分区
     * 
     * @param levelCode 分诊等级代码
     * @return 分诊分区，如 "复苏区/抢救区"
     */
    public static String getTreatmentZone(Integer levelCode) {
        TriageLevel level = TriageLevel.fromCode(levelCode);
        return level != null ? level.getTreatmentZone() : "待分配";
    }
    
    /**
     * 获取完整的分诊描述
     * 
     * @param levelCode 分诊等级代码
     * @return 完整描述信息
     */
    public static String getFullDescription(Integer levelCode) {
        TriageLevel level = TriageLevel.fromCode(levelCode);
        return level != null ? level.getFullDescription() : "未分诊";
    }
    
    /**
     * 获取分诊等级详细信息（用于API返回）
     * 
     * @param levelCode 分诊等级代码
     * @return 包含所有分诊信息的Map
     */
    public static Map<String, Object> getTriageInfo(Integer levelCode) {
        Map<String, Object> info = new HashMap<>();
        TriageLevel level = TriageLevel.fromCode(levelCode);
        
        if (level != null) {
            info.put("code", level.getCode());
            info.put("name", level.getName());
            info.put("color", level.getColor());
            info.put("responseTime", level.getResponseTime());
            info.put("treatmentZone", level.getTreatmentZone());
            info.put("description", level.getDescription());
            info.put("isUrgent", level.isUrgent());
            info.put("isCritical", level.isCritical());
        } else {
            info.put("code", null);
            info.put("name", "未分诊");
            info.put("color", "灰色");
            info.put("responseTime", "未定义");
            info.put("treatmentZone", "待分配");
            info.put("description", "等待分诊");
            info.put("isUrgent", false);
            info.put("isCritical", false);
        }
        
        return info;
    }
    
    /**
     * 判断是否为紧急等级（Ⅰ级或Ⅱ级）
     * 
     * @param levelCode 分诊等级代码
     * @return true-紧急等级，false-非紧急等级
     */
    public static boolean isUrgent(Integer levelCode) {
        TriageLevel level = TriageLevel.fromCode(levelCode);
        return level != null && level.isUrgent();
    }
    
    /**
     * 判断是否为Ⅰ级急危等级
     * 
     * @param levelCode 分诊等级代码
     * @return true-Ⅰ级急危，false-非Ⅰ级
     */
    public static boolean isCritical(Integer levelCode) {
        TriageLevel level = TriageLevel.fromCode(levelCode);
        return level != null && level.isCritical();
    }
    
    /**
     * 验证分诊等级代码是否有效
     * 
     * @param levelCode 分诊等级代码
     * @return true-有效，false-无效
     */
    public static boolean isValidTriageLevel(Integer levelCode) {
        return levelCode != null && levelCode >= 1 && levelCode <= 4;
    }
    
    /**
     * 根据生命体征评估分诊等级（客观评估指标 - 专家共识表1）
     * 
     * @param heartRate 心率(次/min)
     * @param systolicBP 收缩压(mmHg)
     * @param spo2 血氧饱和度(%)
     * @param temperature 体温(℃)
     * @return 建议的分诊等级代码
     */
    public static Integer assessTriageLevelByVitals(Double heartRate, Double systolicBP, 
                                                     Double spo2, Double temperature) {
        // Ⅰ级急危判断
        if (heartRate != null && (heartRate > 180 || heartRate < 40)) {
            return 1;
        }
        if (systolicBP != null && (systolicBP < 70 || systolicBP > 200)) {
            return 1;
        }
        if (spo2 != null && spo2 < 80) {
            return 1;
        }
        if (temperature != null && (temperature > 41.0 || temperature < 35.0)) {
            return 1;
        }
        
        // Ⅱ级急重判断
        if (heartRate != null && ((heartRate >= 150 && heartRate <= 180) || 
                                  (heartRate >= 40 && heartRate <= 50))) {
            return 2;
        }
        if (systolicBP != null && ((systolicBP >= 70 && systolicBP <= 80) || 
                                    systolicBP > 200)) {
            return 2;
        }
        if (spo2 != null && spo2 >= 80 && spo2 < 90) {
            return 2;
        }
        if (temperature != null && temperature > 39.0 && temperature <= 41.0) {
            return 2;
        }
        
        // Ⅲ级急症判断
        if (heartRate != null && ((heartRate >= 100 && heartRate < 150) || 
                                  (heartRate > 50 && heartRate <= 55))) {
            return 3;
        }
        if (systolicBP != null && ((systolicBP > 80 && systolicBP <= 90) || 
                                    (systolicBP >= 180 && systolicBP <= 200))) {
            return 3;
        }
        if (spo2 != null && spo2 >= 90 && spo2 < 94) {
            return 3;
        }
        if (temperature != null && temperature >= 38.5 && temperature <= 39.0) {
            return 3;
        }
        
        // Ⅳ级亚急症（默认）
        return 4;
    }
    
    /**
     * 获取所有分诊等级列表（用于前端下拉框等）
     * 
     * @return 所有分诊等级的信息列表
     */
    public static Map<Integer, Map<String, Object>> getAllTriageLevels() {
        Map<Integer, Map<String, Object>> levels = new HashMap<>();
        for (TriageLevel level : TriageLevel.values()) {
            levels.put(level.getCode(), getTriageInfo(level.getCode()));
        }
        return levels;
    }
}
