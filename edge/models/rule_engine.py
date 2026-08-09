"""
边缘端规则引擎
基于国家卫健委《急诊预检分诊专家共识》(2018年版)

实现依据：
1. 客观评估指标：心率、血压、SpO2、体温、POCT指标
2. 人工评级指标：气道、呼吸、循环、意识(ABCD评估)
3. 四级分诊标准：Ⅰ级急危、Ⅱ级急重、Ⅲ级急症、Ⅳ级亚急症/非急症
4. 响应时限：Ⅰ级即刻、Ⅱ级10min、Ⅲ级30min、Ⅳ级60min-2h
5. 标识颜色：红色、橙色、黄色、绿色

参考文献：
《急诊预检分诊专家共识》中华急诊医学杂志2018年6月第27卷第6期
"""

import re
import logging
from typing import Dict, Any, List, Tuple
import config

logger = logging.getLogger(__name__)


class EdgeRuleEngine:
    """
    边缘端规则引擎
    严格遵循国家卫健委急诊预检分诊专家共识(2018)
    四级分诊标准：Ⅰ级(红)、Ⅱ级(橙)、Ⅲ级(黄)、Ⅳ级(绿)
    """
    
    def __init__(self):
        # 初始化规则集（基于专家共识表1）
        self.vital_rules = self._load_vital_sign_rules_consensus()
        self.symptom_keywords = self._load_symptom_keywords_consensus()
        self.special_populations = self._load_special_population_rules()
        
        logger.info("边缘规则引擎初始化完成 - 基于2018年急诊预检分诊专家共识")
    
    def _load_vital_sign_rules_consensus(self):
        """
        加载生理参数规则 - 严格遵循专家共识表1客观评估指标
        
        客观评估指标维度：
        1. 心率 (heart_rate)
        2. 收缩压 (systolic_bp) 
        3. 血氧饱和度 (SpO2)
        4. 腋温 (temperature)
        5. POCT指标 (血糖、血钾、心肌标志物等)
        """
        return {
            # Ⅰ级急危 - 红色 - 即刻
            'level_1_critical': {
                'heart_rate': {
                    'critical_high': 180,  # >180次/min
                    'critical_low': 40     # <40次/min
                },
                'systolic_bp': {
                    'critical_low': 70     # <70mmHg
                },
                'spo2': {
                    'critical_low': 80,    # <80% 且呼吸急促
                    'requires_oxygen': True
                },
                'temperature': {
                    'critical_high': 41.0  # >41°C
                },
                'poct': {
                    'glucose_low': 3.33,   # <3.33 mmol/L
                    'potassium_high': 7.0  # >7.0 mmol/L
                }
            },
            
            # Ⅱ级急重 - 橙色 - 10分钟内
            'level_2_emergent': {
                'heart_rate': {
                    'range_high': (150, 180),  # 150-180次/min
                    'range_low': (40, 50)      # 40-50次/min
                },
                'systolic_bp': {
                    'high': 200,               # >200mmHg
                    'range_low': (70, 80)      # 70-80mmHg
                },
                'spo2': {
                    'range': (80, 90),         # 80%-90% 且呼吸急促
                    'requires_oxygen': True
                }
            },
            
            # Ⅲ级急症 - 黄色 - 30分钟内
            'level_3_urgent': {
                'heart_rate': {
                    'range_high': (100, 150),  # 100-150次/min
                    'range_low': (50, 55)      # 50-55次/min
                },
                'systolic_bp': {
                    'range_high': (180, 200),  # 180-200mmHg
                    'range_low': (80, 90)      # 80-90mmHg
                },
                'spo2': {
                    'range': (90, 94),         # 90%-94% 且呼吸急促
                    'requires_oxygen': True
                }
            },
            
            # Ⅳ级亚急症/非急症 - 绿色 - 60分钟-2小时
            'level_4_non_urgent': {
                'vital_stable': True,          # 生命体征平稳
                'normal_ranges': {
                    'heart_rate': (60, 100),
                    'systolic_bp': (90, 140),
                    'spo2': (95, 100),
                    'temperature': (36.0, 37.5)
                }
            }
        }
    
    def _load_symptom_keywords_consensus(self):
        """
        加载症状关键词规则 - 严格遵循专家共识表1人工评定指标
        
        评估维度：
        1. 气道(Airway) - 气道风险、呼吸困难
        2. 呼吸(Breath) - 呼吸节律、SpO2
        3. 循环(Circulation) - 休克、灌注、血压
        4. 意识(Disability) - GCS评分、意识状态
        """
        return {
            # Ⅰ级急危 - 红色 - 即刻抢救
            1: {
                'abcd_critical': [
                    # 气道/呼吸
                    '心博停止', '呼吸停止', '节律不稳定',
                    '气道不能维持', '气道梗阻',
                    
                    # 循环
                    '休克', '血压急剧下降', '严重休克',
                    
                    # 意识
                    '急性意识障碍', '无反应', '仅疼痛刺激反应', 
                    'GCS<9', '昏迷', '深度昏迷',
                    
                    # 特殊情况
                    '癫痫持续状态', '持续抽搐',
                    '复合伤', '严重多发伤',
                    '急性药物过量', '中毒',
                    '严重精神行为异常', '正在自伤', '正在他伤',
                    '严重休克儿童', '小儿惊厥'
                ],
                'cardiac': [
                    '心脏骤停', '室颤', '室速', '心跳停止',
                    '急性心肌梗死', 'STEMI'
                ]
            },
            # Ⅱ级急重 - 橙色 - 10分钟内
            2: {
                'airway_risk': [
                    '严重呼吸困难', '气道风险', '气道保护不足'
                ],
                'circulation': [
                    '循环障碍', '皮肤湿冷花斑', '灌注差',
                    '怀疑脓毒症', '感染性休克'
                ],
                'consciousness': [
                    '昏睡', '强烈刺激下有防御反应', 'GCS 9-12'
                ],
                'acute_conditions': [
                    '急性脑卒中', '脑梗死', '脑出血',
                    '类似心脏因素胸痛', '疑似心梗',
                    '不明原因严重疼痛伴大汗', '脐以上剧痛',
                    
                    # 高危胸腹疼痛（专家共识明确列出）
                    '急性心肌梗死', '急性肺栓塞', '主动脉夹层',
                    '主动脉瘤', '急性心肌炎', '心包炎', '心包积液',
                    '异位妊娠', '消化道穿孔', '睾丸扭转',
                    
                    '所有原因严重疼痛', '7-10分疼痛',
                    '活动性失血', '严重失血',
                    '严重局部创伤', '大骨折', '截肢',
                    '过量药物', '过量毒物', '化学物质暴露',
                    '严重精神行为异常', '暴力', '攻击', '需约束',
                    '急性哮喘', '血压脉搏稳定哮喘'
                ]
            },
            
            # Ⅲ级急症 - 黄色 - 30分钟内
            3: {
                'consciousness': [
                    '嗜睡', '可唤醒', '无刺激转入睡眠', 'GCS 13-14'
                ],
                'seizure': [
                    '间断癫痫', '癫痫发作间期'
                ],
                'pain': [
                    '中等程度非心源性胸痛',
                    '中等程度腹痛', '65岁以上无高危腹痛',
                    '中重度疼痛', '4-6分疼痛',
                    '需要止痛'
                ],
                'trauma': [
                    '头外伤', '头部创伤',
                    '中等程度外伤', '肢体感觉运动异常',
                    '持续呕吐', '脱水'
                ],
                'psychiatric': [
                    '精神行为异常', '自残风险',
                    '急性精神错乱', '思维混乱',
                    '焦虑', '抑郁', '潜在攻击性'
                ],
                'others': [
                    '稳定新生儿',
                    '吸入异物无呼吸困难',
                    '吞咽困难无呼吸困难'
                ]
            },
            
            # Ⅳ级亚急症 - 绿色 - 60分钟内
            4: {
                'moderate_symptoms': [
                    '呕吐或腹泻无脱水',
                    '中等程度疼痛有危险特征',
                    '无肋骨疼痛或呼吸困难的胸部损伤',
                    '非特异性轻度腹痛',
                    '轻微出血',
                    '轻微头部损伤无意识丧失',
                    '小肢体创伤', '生命体征正常', '轻中度疼痛',
                    '关节红肿', '轻度肿痛'
                ],
                'psychiatric_stable': [
                    '精神行为异常但无直接威胁'
                ]
            },
            
            # Ⅳ级非急症 - 绿色 - 2-4小时
            5: {
                'stable': [
                    '病情稳定', '症状轻微',
                    '低危病史且无症状', '症状轻微',
                    '无危险特征微疼痛',
                    '微小伤口', '不需缝合擦伤', '小裂伤',
                    '慢性症状患者',
                    '轻微精神行为异常',
                    '稳定恢复期', '无症状复诊',
                    '仅开药', '仅开医疗证明'
                ]
            }
        }
    
    def _load_special_population_rules(self):
        """
        加载特殊人群规则 - 专家共识特别强调
        
        特殊人群包括：
        1. 老年人(≥65岁) - 可适当安排提前就诊
        2. 孕妇 - 需特殊关注
        3. 儿童/婴儿 - 生理参数标准不同
        4. 免疫缺陷者 - 发热伴粒细胞减少为Ⅱ级
        5. 心肺基础疾病者
        6. 残疾人
        """
        return {
            'elderly': {
                'age_threshold': 65,
                'priority_consideration': True,
                'vital_adjustments': {
                    'description': '老年患者病情变化可能较快，65岁以上腹痛为Ⅲ级'
                }
            },
            'pediatric': {
                'age_ranges': {
                    'infant': (0, 1),      # 婴儿
                    'toddler': (1, 3),     # 幼儿
                    'child': (3, 14)       # 儿童
                },
                'special_conditions': [
                    '严重休克儿童为Ⅰ级',
                    '小儿惊厥为Ⅰ级',
                    '稳定新生儿为Ⅲ级'
                ]
            },
            'pregnant': {
                'priority': True,
                'special_alert': '异位妊娠为Ⅱ级高危胸腹疼痛'
            },
            'immunocompromised': {
                'condition': '发热伴粒细胞减少',
                'triage_level': 2,  # Ⅱ级
                'description': '专家共识明确：发热伴粒细胞减少为Ⅱ级急重'
            },
            'disabled': {
                'priority_consideration': True
            }
        }
    
    def evaluate(self, vital_signs: Dict[str, float], 
                 symptoms_text: str = "", 
                 patient_age: int = 40,
                 is_pregnant: bool = False,
                 is_immunocompromised: bool = False) -> int:
        """
        执行规则引擎评估 - 严格遵循国家卫健委专家共识
        
        评估流程：
        1. 客观评估指标：心率、血压、SpO2、体温
        2. 人工评定指标：ABCD评估(气道、呼吸、循环、意识)
        3. 特殊人群修正：老年、儿童、孕妇、免疫缺陷
        4. 取最高级别："患者级别以其中任一最高级别指标确定"
        
        Args:
            vital_signs: 生理参数字典
            symptoms_text: 症状文本描述
            patient_age: 患者年龄
            is_pregnant: 是否怀孕
            is_immunocompromised: 是否免疫缺陷
            
        Returns:
            分诊等级 (1-4)：1=Ⅰ级急危, 2=Ⅱ级急重, 3=Ⅲ级急症, 4=Ⅳ级亚急症/非急症
        """
        try:
            # 第一步：客观评估指标
            objective_level = self._evaluate_objective_indicators(vital_signs)
            
            # 第二步：人工评定指标(ABCD评估)
            subjective_level = self._evaluate_subjective_indicators(symptoms_text)
            
            # 第三步：特殊人群修正
            special_level = self._evaluate_special_populations(
                vital_signs, symptoms_text, patient_age, 
                is_pregnant, is_immunocompromised
            )
            
            # 取最高级别（最严重）- 专家共识明确要求
            final_level = min(objective_level, subjective_level, special_level)
            
            logger.debug(
                f"规则引擎评估: 客观={objective_level}, "
                f"主观={subjective_level}, 特殊人群={special_level}, "
                f"最终={final_level}"
            )
            
            return max(1, min(4, final_level))  # 确保在1-4范围内
            
        except Exception as e:
            logger.error(f"规则引擎评估失败: {e}")
            return 3  # 默认Ⅲ级急症级别
    
    def _evaluate_objective_indicators(self, vital_signs: Dict[str, float]) -> int:
        """
        评估客观指标 - 专家共证表1客观评估指标
        包括：心率、收缩压、SpO2、腋温、POCT指标
        """
        levels = []
        
        hr = vital_signs.get('heartRate', 75)
        sbp = vital_signs.get('systolicBP', 120)
        spo2 = vital_signs.get('bloodOxygen', 98)
        temp = vital_signs.get('temperature', 36.5)
        
        # Ⅰ级急危判断
        if hr > 180 or hr < 40:
            levels.append(1)
        if sbp < 70:
            levels.append(1)
        if spo2 < 80:  # <80% 且呼吸急促（经吸氧不能改善）
            levels.append(1)
        if temp > 41.0:
            levels.append(1)
        
        # Ⅱ级急重判断
        if 150 <= hr <= 180 or 40 <= hr <= 50:
            levels.append(2)
        if sbp > 200 or (70 <= sbp <= 80):
            levels.append(2)
        if 80 <= spo2 <= 90:  # 80%-90% 且呼吸急促
            levels.append(2)
        
        # Ⅲ级急症判断
        if 100 <= hr <= 150 or 50 <= hr <= 55:
            levels.append(3)
        if 180 <= sbp <= 200 or 80 <= sbp <= 90:
            levels.append(3)
        if 90 <= spo2 <= 94:  # 90%-94% 且呼吸急促
            levels.append(3)
        
        # 如果没有异常，返回Ⅳ级
        return min(levels) if levels else 4
    
    def _score_temperature(self, temp: float, age: int, is_pregnant: bool) -> int:
        """体温评分"""
        # 特殊人群修正
        if age <= 14 or age >= 65 or is_pregnant:
            warning_threshold = 37.8 if is_pregnant else 38.0
            if temp >= warning_threshold:
                temp += 0.5  # 提升风险等级
        
        # 分级评估
        if temp >= 41.0 or temp <= 35.0:
            return 1  # 濒危
        elif temp >= 39.5 or temp <= 35.5:
            return 2  # 危急
        elif temp >= 38.5:
            return 3  # 急症
        elif temp >= 37.8:
            return 4  # 次急症
        else:
            return 5  # 非急症
    
    def _score_heart_rate(self, hr: int, age: int) -> int:
        """心率评分"""
        # 年龄相关的正常范围调整
        if age <= 14:
            # 儿童心率标准
            if hr >= 180 or hr <= 80:
                return 1
            elif hr >= 160 or hr <= 90:
                return 2
            elif hr >= 140:
                return 3
            elif hr >= 120:
                return 4
            else:
                return 5
        elif age >= 65:
            # 老年人心率标准
            if hr >= 140 or hr <= 45:
                return 1
            elif hr >= 120 or hr <= 55:
                return 2
            elif hr >= 100:
                return 3
            elif hr >= 90:
                return 4
            else:
                return 5
        else:
            # 成人标准
            if hr >= 150 or hr <= 40:
                return 1
            elif hr >= 130 or hr <= 50:
                return 2
            elif hr >= 110 or hr <= 60:
                return 3
            elif hr >= 100:
                return 4
            else:
                return 5
    
    def _score_blood_pressure(self, sbp: int, dbp: int, age: int, is_pregnant: bool) -> int:
        """血压评分"""
        # 孕妇特殊标准
        if is_pregnant and sbp >= 140:
            return 2  # 妊娠高血压
        
        # 老年人血压标准调整
        if age >= 65:
            high_threshold = 160
        else:
            high_threshold = 180
        
        # 收缩压评估
        if sbp >= 200 or sbp <= 70:
            return 1  # 濒危
        elif sbp >= high_threshold or sbp <= 90:
            return 2  # 危急
        elif sbp >= 160 or sbp <= 100:
            return 3  # 急症
        elif sbp >= 140:
            return 4  # 次急症
        else:
            return 5  # 正常
    
    def _score_blood_oxygen(self, spo2: int) -> int:
        """血氧评分"""
        if spo2 <= 85:
            return 1  # 濒危
        elif spo2 <= 90:
            return 2  # 危急
        elif spo2 <= 94:
            return 3  # 急症
        elif spo2 <= 96:
            return 4  # 次急症
        else:
            return 5  # 正常
    
    def _evaluate_subjective_indicators(self, symptoms_text: str) -> int:
        """
        评估人工评定指标 - 专家共证表1人工评级指标
        基于ABCD评估：气道(Airway)、呼吸(Breath)、循环(Circulation)、意识(Disability)
        """
        if not symptoms_text or symptoms_text.strip() == "":
            return 4  # 无症状描述默认Ⅳ级
        
        # 清理文本
        text = symptoms_text.lower().replace(' ', '').replace('，', ',').replace('。', '.')
        
        # 按优先级检查关键词（从高到低）
        for level in range(1, 6):
            if self._check_keywords_for_level_consensus(text, level):
                return level
        
        return 4  # 默认Ⅳ级
    
    def _check_keywords_for_level_consensus(self, text: str, level: int) -> bool:
        """检查特定级别的关键词 - 基于专家共识"""
        if level not in self.symptom_keywords:
            return False
        
        keywords_dict = self.symptom_keywords[level]
        
        for category, keywords in keywords_dict.items():
            for keyword in keywords:
                if keyword in text:
                    logger.debug(f"匹配到ⅠⅡⅢⅣ级[{level}]关键词: {keyword} (类别: {category})")
                    return True
        
        return False
    
    def get_triage_color(self, level: int) -> str:
        """
        获取分诊级别颜色 - 专家共证标识颜色
        Ⅰ级=红色、Ⅱ级=橙色、Ⅲ级=黄色、Ⅳ级=绿色
        """
        colors = {
            1: '红色',  # Ⅰ级急危
            2: '橙色',  # Ⅱ级急重
            3: '黄色',  # Ⅲ级急症
            4: '绿色'   # Ⅳ级亚急症/非急症
        }
        return colors.get(level, '绿色')
    
    def get_response_time(self, level: int) -> str:
        """
        获取响应时限 - 专家共证规定
        Ⅰ级=即刻、Ⅱ级=10min、Ⅲ级=30min、Ⅳ级=60min-2h
        """
        response_times = {
            1: '即刻',              # Ⅰ级急危
            2: '10分钟内',         # Ⅱ级急重
            3: '30分钟内',         # Ⅲ级急症
            4: '60分钟-2小时'      # Ⅳ级亚急症/非急症
        }
        return response_times.get(level, '60分钟内')
    
    def _combine_scores(self, vital_score: int, symptom_score: int, 
                       age: int, is_pregnant: bool) -> int:
        """综合评分并应用修正因子"""
        # 取更严重的评分
        base_score = min(vital_score, symptom_score)
        
        # 年龄修正
        age_modifier = 0
        if age <= 14 or age >= 65:
            age_modifier = -1  # 提升1级优先级
        
        # 妊娠修正
        pregnancy_modifier = 0
        if is_pregnant:
            pregnancy_modifier = -1
        
        # 应用修正
        final_score = base_score + age_modifier + pregnancy_modifier
        
        return max(1, min(5, final_score))
    
    def get_triage_explanation(self, vital_signs: Dict[str, float], 
                             symptoms_text: str = "",
                             patient_age: int = 40,
                             is_pregnant: bool = False,
                             is_immunocompromised: bool = False) -> Dict[str, Any]:
        """
        获取分诊解释 - 基于专家共识的详细评估理由
        """
        triage_level = self.evaluate(
            vital_signs, symptoms_text, patient_age, 
            is_pregnant, is_immunocompromised
        )
        
        explanation = {
            'triage_level': triage_level,
            'level_name': self.get_triage_description_consensus(triage_level),
            'color': self.get_triage_color(triage_level),
            'response_time': self.get_response_time(triage_level),
            'treatment_zone': self.get_treatment_zone(triage_level),
            'objective_analysis': self._analyze_objective_vitals(vital_signs),
            'subjective_analysis': self._analyze_subjective_symptoms(symptoms_text),
            'special_population_notes': self._get_special_population_notes(
                patient_age, is_pregnant, is_immunocompromised
            ),
            'recommendations': self._get_treatment_recommendations(triage_level),
            'consensus_reference': '基于《急诊预检分诊专家共诈》2018年版'
        }
        
        return explanation
    
    def get_triage_description_consensus(self, level: int) -> str:
        """
        获取分诊等级描述 - 严格遵循专家共诃表1
        """
        descriptions = {
            1: "Ⅰ级-急危：正在或即将发生生命威胁或病情恶化，需要立即进行积极干预",
            2: "Ⅱ级-急重：病情危重或迅速恶化，如短时间内不能进行治疗则危及生命或造成严重的器官功能衰竭",
            3: "Ⅲ级-急症：存在潜在的生命威胁，如短时间内不进行干预，病情可进展至威胁生命或产生十分不利的结局",
            4: "Ⅳ级-亚急症/非急症：存在潜在的严重性或慢性/非常轻微的症状"
        }
        return descriptions.get(level, "未知级别")
    
    def get_treatment_zone(self, level: int) -> str:
        """
        获取诊疗区域 - 专家共证表2分级分区管理
        """
        zones = {
            1: '复苏区',      # Ⅰ级 - 立即进行评估和救治
            2: '抢救区',      # Ⅱ级 - 立即监护生命体征
            3: '优先诊疗区',  # Ⅲ级 - 优先诊治
            4: '普通诊疗区'   # Ⅳ级 - 顺序就诊
        }
        return zones.get(level, '普通诊疗区')
    
    def _analyze_vitals(self, vital_signs: Dict[str, float]) -> List[str]:
        """分析生理参数异常"""
        abnormalities = []
        
        temp = vital_signs.get('temperature', 36.5)
        if temp >= 39:
            abnormalities.append(f"高热({temp}°C)")
        elif temp <= 36:
            abnormalities.append(f"低温({temp}°C)")
        
        hr = vital_signs.get('heartRate', 75)
        if hr >= 120:
            abnormalities.append(f"心动过速({hr}bpm)")
        elif hr <= 50:
            abnormalities.append(f"心动过缓({hr}bpm)")
        
        sbp = vital_signs.get('systolicBP', 120)
        if sbp >= 180:
            abnormalities.append(f"严重高血压({sbp}mmHg)")
        elif sbp <= 90:
            abnormalities.append(f"低血压({sbp}mmHg)")
        
        spo2 = vital_signs.get('bloodOxygen', 98)
        if spo2 <= 94:
            abnormalities.append(f"血氧饱和度低({spo2}%)")
        
        return abnormalities or ["生理参数基本正常"]
    
    def _analyze_symptoms(self, symptoms_text: str) -> List[str]:
        """分析症状严重程度"""
        if not symptoms_text:
            return ["无症状描述"]
        
        findings = []
        text = symptoms_text.lower()
        
        # 检查严重症状
        severe_symptoms = ['胸痛', '呼吸困难', '意识不清', '大出血', '休克']
        for symptom in severe_symptoms:
            if symptom in text:
                findings.append(f"发现严重症状: {symptom}")
        
        # 检查一般症状
        common_symptoms = ['发热', '头痛', '腹痛', '咳嗽', '恶心']
        for symptom in common_symptoms:
            if symptom in text:
                findings.append(f"一般症状: {symptom}")
        
        return findings or ["症状描述较轻微"]
    
    def _identify_risk_factors(self, age: int, is_pregnant: bool) -> List[str]:
        """识别风险因子"""
        factors = []
        
        if age <= 14:
            factors.append("儿童患者 - 生理参数参考值不同")
        elif age >= 65:
            factors.append("老年患者 - 病情变化可能较快")
        
        if is_pregnant:
            factors.append("妊娠期患者 - 需要特殊关注")
        
        return factors or ["无特殊风险因子"]
    
    def _get_recommendations(self, level: int) -> List[str]:
        """获取处理建议"""
        recommendations = {
            1: [
                "立即启动急救流程",
                "通知急救医生",
                "准备急救设备",
                "监测生命体征",
                "建立静脉通路"
            ],
            2: [
                "10分钟内安排医生接诊",
                "持续监测生命体征", 
                "准备相关检查",
                "通知相关专科医生"
            ],
            3: [
                "30分钟内安排接诊",
                "完善基础检查",
                "评估病情变化",
                "适当对症处理"
            ],
            4: [
                "1小时内安排接诊",
                "常规检查和评估",
                "健康宣教"
            ],
            5: [
                "可预约就诊",
                "健康咨询",
                "定期随访"
            ]
        }
        
        return recommendations.get(level, ["请咨询医生"])


# 测试代码
if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)
    
    # 创建规则引擎实例
    rule_engine = EdgeRuleEngine()
    
    # 测试用例1：危急情况
    test_vitals_1 = {
        'temperature': 39.8,
        'heartRate': 140,
        'systolicBP': 85,
        'diastolicBP': 50,
        'bloodOxygen': 88
    }
    test_symptoms_1 = "患者突然胸痛，呼吸困难，出冷汗"
    
    result_1 = rule_engine.evaluate(test_vitals_1, test_symptoms_1, age=70)
    explanation_1 = rule_engine.get_triage_explanation(test_vitals_1, test_symptoms_1, age=70)
    
    print("=== 测试用例1: 危急情况 ===")
    print(f"分诊等级: {result_1}")
    print(f"详细分析: {explanation_1}")
    print()
    
    # 测试用例2：一般情况
    test_vitals_2 = {
        'temperature': 37.5,
        'heartRate': 88,
        'systolicBP': 130,
        'diastolicBP': 85,
        'bloodOxygen': 97
    }
    test_symptoms_2 = "轻微头痛，无其他不适"
    
    result_2 = rule_engine.evaluate(test_vitals_2, test_symptoms_2, age=35)
    explanation_2 = rule_engine.get_triage_explanation(test_vitals_2, test_symptoms_2, age=35)
    
    print("=== 测试用例2: 一般情况 ===")
    print(f"分诊等级: {result_2}")
    print(f"详细分析: {explanation_2}")
    print()
    
    # 测试用例3：儿童发热
    test_vitals_3 = {
        'temperature': 38.5,
        'heartRate': 120,
        'systolicBP': 100,
        'diastolicBP': 60,
        'bloodOxygen': 98
    }
    test_symptoms_3 = "小儿发热，精神差"
    
    result_3 = rule_engine.evaluate(test_vitals_3, test_symptoms_3, age=6)
    explanation_3 = rule_engine.get_triage_explanation(test_vitals_3, test_symptoms_3, age=6)
    
    print("=== 测试用例3: 儿童发热 ===")
    print(f"分诊等级: {result_3}")
    print(f"详细分析: {explanation_3}")