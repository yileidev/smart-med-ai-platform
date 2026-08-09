package com.medical.enums;

/**
 * 急诊分诊等级枚举
 * 严格遵循国家卫健委《急诊预检分诊专家共识》(2018年版)
 * 
 * 四级分诊标准：
 * - Ⅰ级(红色) - 急危：濒临死亡或生命体征不稳定，需即刻处理
 * - Ⅱ级(橙色) - 急重：存在潜在生命威胁，需10分钟内处理
 * - Ⅲ级(黄色) - 急症：病情急迫但暂无生命危险，需30分钟内处理
 * - Ⅳ级(绿色) - 亚急症/非急症：病情稳定，可在60分钟-2小时内处理
 * 
 * @author Medical System
 * @version 2.0 - 基于国家卫健委2018年版标准
 */
public enum TriageLevel {
    
    /**
     * Ⅰ级 - 急危(红色)
     * 响应时限：即刻处理
     * 分诊分区：复苏区/抢救区
     * 
     * 客观评估指标（满足任一条件）：
     * - 心率：>180次/min 或 <40次/min
     * - 收缩压：<70mmHg 或 >200mmHg
     * - SpO2：<80%
     * - 体温：>41℃ 或 <35℃
     * - POCT：血糖<3.33mmol/L，血钾>7.0mmol/L
     * 
     * 人工评定指标（ABCD评估）：
     * - A(气道)：心博停止、呼吸停止、气道梗阻
     * - B(呼吸)：呼吸停止、严重呼吸困难
     * - C(循环)：休克、严重循环障碍
     * - D(意识)：昏迷(GCS<9)、急性意识障碍
     * - 其他：癫痫持续状态、复合伤、急性药物过量
     */
    LEVEL_1(1, "Ⅰ级-急危", "红色", "即刻", "复苏区/抢救区", 
            "濒临死亡或生命体征不稳定，需立即救治"),
    
    /**
     * Ⅱ级 - 急重(橙色)
     * 响应时限：10分钟内
     * 分诊分区：抢救区
     * 
     * 客观评估指标（满足任一条件）：
     * - 心率：150-180次/min 或 40-50次/min
     * - 收缩压：70-80mmHg 或 >200mmHg
     * - SpO2：80-90%
     * - 体温：39-41℃
     * - POCT：血糖3.33-3.88mmol/L，血钾6.0-7.0mmol/L
     * 
     * 人工评定指标（ABCD评估）：
     * - A(气道)：可维持但不稳定
     * - B(呼吸)：严重呼吸困难、呼吸频率<10次/min或>30次/min
     * - C(循环)：循环障碍、胸痛伴大汗
     * - D(意识)：昏睡、定向障碍
     * - 高危胸腹痛：急性心肌梗死、急性肺栓塞、主动脉夹层、异位妊娠等
     */
    LEVEL_2(2, "Ⅱ级-急重", "橙色", "10分钟内", "抢救区", 
            "存在潜在生命威胁，需尽快处理"),
    
    /**
     * Ⅲ级 - 急症(黄色)
     * 响应时限：30分钟内
     * 分诊分区：优先诊疗区
     * 
     * 客观评估指标（满足任一条件）：
     * - 心率：100-150次/min 或 50-55次/min
     * - 收缩压：80-90mmHg 或 180-200mmHg
     * - SpO2：90-94%
     * - 体温：38.5-39℃
     * - POCT：血糖3.88-4.0mmol/L，血钾5.5-6.0mmol/L
     * 
     * 人工评定指标（ABCD评估）：
     * - A(气道)：可维持
     * - B(呼吸)：呼吸困难
     * - C(循环)：中等程度疼痛、循环稳定
     * - D(意识)：嗜睡、间断癫痫
     * - 其他：头外伤、精神行为异常、体液丢失
     */
    LEVEL_3(3, "Ⅲ级-急症", "黄色", "30分钟内", "优先诊疗区", 
            "病情急迫但暂无生命危险，需优先处理"),
    
    /**
     * Ⅳ级 - 亚急症/非急症(绿色)
     * 响应时限：60分钟-2小时
     * 分诊分区：普通诊疗区
     * 
     * 客观评估指标：
     * - 心率：55-100次/min
     * - 收缩压：90-180mmHg
     * - SpO2：≥94%
     * - 体温：35-38.5℃
     * - POCT指标正常
     * 
     * 临床表现：
     * - 生命体征平稳
     * - 症状轻微
     * - 仅开药、开证明
     * - 慢性病常规随访
     */
    LEVEL_4(4, "Ⅳ级-亚急症", "绿色", "60分钟-2小时", "普通诊疗区", 
            "病情稳定，可按常规流程处理");
    
    private final Integer code;
    private final String name;
    private final String color;
    private final String responseTime;
    private final String treatmentZone;
    private final String description;
    
    TriageLevel(Integer code, String name, String color, String responseTime, 
                String treatmentZone, String description) {
        this.code = code;
        this.name = name;
        this.color = color;
        this.responseTime = responseTime;
        this.treatmentZone = treatmentZone;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getColor() {
        return color;
    }
    
    public String getResponseTime() {
        return responseTime;
    }
    
    public String getTreatmentZone() {
        return treatmentZone;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取分诊等级
     */
    public static TriageLevel fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TriageLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
    
    /**
     * 获取完整描述（包含响应时限和分区）
     */
    public String getFullDescription() {
        return String.format("%s - %s - 响应时限：%s - 分诊分区：%s",
                name, color, responseTime, treatmentZone);
    }
    
    /**
     * 判断是否为急危/急重等级（需优先处理）
     */
    public boolean isUrgent() {
        return code <= 2;
    }
    
    /**
     * 判断是否为Ⅰ级急危等级
     */
    public boolean isCritical() {
        return code == 1;
    }
}
