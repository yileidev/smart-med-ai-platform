"""
边缘端主程序
Jetson Orin Nano Super - 多模态急诊分诊系统

功能：
1. 传感器数据采集（DS18B20、MAX30102、USB麦克风）
2. 卡尔曼滤波去噪
3. 本地离线语音识别（Vosk）
4. BERT-Tiny轻量分诊
5. MQTT数据上报云端
"""

import time
import json
import threading
import logging
from datetime import datetime
from colorlog import ColoredFormatter

import config
from sensors.ds18b20_driver import DS18B20Sensor
from sensors.max30102_driver import MAX30102Sensor
from sensors.usb_mic_driver import USBMicRecorder
from preprocessing.kalman_filter import KalmanFilter
from preprocessing.local_voice_recognizer import LocalVoiceRecognizer
from models.bert_tiny_triage import BERTTinyTriage
from models.rule_engine import EdgeRuleEngine
from mqtt_client.mqtt_publisher import MQTTPublisher

# 配置日志
formatter = ColoredFormatter(
    "%(log_color)s%(asctime)s - %(name)s - %(levelname)s - %(message)s%(reset)s",
    datefmt='%Y-%m-%d %H:%M:%S',
    log_colors={
        'DEBUG': 'cyan',
        'INFO': 'green',
        'WARNING': 'yellow',
        'ERROR': 'red',
        'CRITICAL': 'red,bg_white',
    }
)

handler = logging.StreamHandler()
handler.setFormatter(formatter)
logger = logging.getLogger(__name__)
logger.addHandler(handler)
logger.setLevel(getattr(logging, config.LOG_LEVEL))


class EdgeTriageSystem:
    """边缘端分诊系统主类"""
    
    def __init__(self):
        logger.info("=" * 60)
        logger.info("边缘端分诊系统启动中...")
        logger.info("设备ID: %s", config.DEVICE_ID)
        logger.info("=" * 60)
        
        # 初始化硬件传感器
        try:
            logger.info("初始化DS18B20体温传感器...")
            self.temp_sensor = DS18B20Sensor()
            
            logger.info("初始化MAX30102心率血氧传感器...")
            self.pulse_oximeter = MAX30102Sensor()
            
            logger.info("初始化USB麦克风...")
            self.microphone = USBMicRecorder()
            
            logger.info("✓ 传感器初始化完成")
        except Exception as e:
            logger.error("传感器初始化失败: %s", str(e))
            raise
        
        # 初始化数据处理模块
        self.kalman_temp = KalmanFilter()
        self.kalman_hr = KalmanFilter()
        self.kalman_spo2 = KalmanFilter()
        logger.info("✓ 卡尔曼滤波器初始化完成")
        
        # 初始化本地离线语音识别 (Vosk)
        self.voice_recognizer = LocalVoiceRecognizer()
        logger.info("✓ Vosk本地语音识别器初始化完成（离线模式）")
        
        # 初始化AI模型
        try:
            logger.info("加载BERT-Tiny分诊模型...")
            self.bert_model = BERTTinyTriage()
            logger.info("✓ BERT-Tiny模型加载完成")
        except Exception as e:
            logger.warning("BERT模型加载失败，将使用规则引擎: %s", str(e))
            self.bert_model = None
        
        # 初始化规则引擎
        self.rule_engine = EdgeRuleEngine()
        logger.info("✓ 边缘规则引擎初始化完成")
        
        # 初始化MQTT客户端
        self.mqtt_client = MQTTPublisher()
        logger.info("✓ MQTT客户端初始化完成")
        
        # 系统状态
        self.running = False
        self.patient_count = 0
        
        logger.info("=" * 60)
        logger.info("边缘端分诊系统初始化完成！")
        logger.info("=" * 60)
    
    def collect_sensor_data(self):
        """采集传感器数据并滤波"""
        try:
            # 采集原始数据
            raw_temp = self.temp_sensor.read_temperature()
            raw_hr, raw_spo2 = self.pulse_oximeter.read_data()
            
            # 手动输入血压
            raw_sbp, raw_dbp = self.input_blood_pressure()
            
            # 卡尔曼滤波去噪
            filtered_temp = self.kalman_temp.filter(raw_temp)
            filtered_hr = self.kalman_hr.filter(raw_hr)
            filtered_spo2 = self.kalman_spo2.filter(raw_spo2)
            
            vital_signs = {
                "temperature": round(filtered_temp, 1),
                "systolicBP": raw_sbp,      # 收缩压
                "diastolicBP": raw_dbp,     # 舒张压
                "heartRate": int(filtered_hr),
                "bloodOxygen": int(filtered_spo2),
                "raw_temperature": round(raw_temp, 1),
                "raw_heartRate": int(raw_hr),
                "raw_bloodOxygen": int(raw_spo2)
            }
            
            logger.debug("生命体征采集: 体温=%.1f°C, 血压=%d/%dmmHg, 心率=%dbpm, 血氧=%d%%",
                        filtered_temp, raw_sbp, raw_dbp, filtered_hr, filtered_spo2)
            
            return vital_signs
            
        except Exception as e:
            logger.error("传感器数据采集失败: %s", str(e))
            return None
    
    def input_blood_pressure(self):
        """手动输入血压值"""
        while True:
            try:
                bp_input = input("请输入血压 (格式: 收缩压/舒张压，如 120/80): ").strip()
                
                if '/' in bp_input:
                    parts = bp_input.split('/')
                    sbp = int(parts[0].strip())
                    dbp = int(parts[1].strip())
                else:
                    # 分开输入
                    sbp = int(input("请输入收缩压 (mmHg): ").strip())
                    dbp = int(input("请输入舒张压 (mmHg): ").strip())
                
                # 验证血压范围
                if 60 <= sbp <= 250 and 40 <= dbp <= 150:
                    if sbp > dbp:
                        logger.info("血压输入: %d/%d mmHg", sbp, dbp)
                        return sbp, dbp
                    else:
                        logger.warning("收缩压应大于舒张压，请重新输入")
                else:
                    logger.warning("血压值超出合理范围，请重新输入 (收缩压60-250, 舒张压40-150)")
                    
            except ValueError:
                logger.warning("输入格式错误，请输入数字")
            except Exception as e:
                logger.error("血压输入异常: %s", str(e))
    
    def record_voice_complaint(self):
        """录制并转换语音主诉（本地Vosk离线识别）"""
        try:
            if not self.voice_recognizer.is_available():
                logger.warning("Vosk语音识别不可用，请手动输入症状")
                return input("请输入患者症状主诉: ").strip()
            
            logger.info("请开始描述症状（10秒内）...")
            
            def status_callback(status):
                logger.info("语音识别状态: %s", status)
            
            transcript = self.voice_recognizer.recognize_from_mic(
                duration=10, 
                callback=status_callback
            )
            
            if transcript and transcript not in ["未检测到语音内容", "语音识别模型未初始化"]:
                logger.info("本地语音识别结果: %s", transcript)
                return transcript
            else:
                logger.warning("语音识别无结果，请手动输入")
                return input("请输入患者症状主诉: ").strip()
            
        except Exception as e:
            logger.error("语音处理失败: %s", str(e))
            return input("请输入患者症状主诉: ").strip()
    
    def perform_edge_triage(self, vital_signs, symptoms_text):
        """执行边缘端分诊"""
        try:
            # 优先使用BERT-Tiny模型
            if self.bert_model:
                logger.info("使用BERT-Tiny模型进行分诊...")
                triage_level, confidence = self.bert_model.predict(
                    symptoms_text, vital_signs
                )
                method = "BERT-Tiny"
            else:
                # 降级使用规则引擎
                logger.info("使用规则引擎进行分诊...")
                triage_level = self.rule_engine.evaluate(vital_signs)
                confidence = 0.85
                method = "Rule Engine"
            
            # 获取分诊描述
            triage_desc = self.get_triage_description(triage_level)
            
            logger.info("分诊结果: 等级=%d级(%s), 置信度=%.2f, 方法=%s",
                       triage_level, triage_desc, confidence, method)
            
            return {
                "triageLevel": triage_level,
                "triageDescription": triage_desc,
                "confidence": round(confidence, 3),
                "method": method
            }
            
        except Exception as e:
            logger.error("边缘分诊失败: %s", str(e))
            return {
                "triageLevel": 3,
                "triageDescription": "急症（默认）",
                "confidence": 0.5,
                "method": "Fallback"
            }
    
    def get_triage_description(self, level):
        """获取分诊等级描述"""
        descriptions = {
            1: "濒危（立即处理）",
            2: "危急（10分钟内）",
            3: "急症（30分钟内）",
            4: "次急症（60分钟内）",
            5: "非急症（120分钟或预约）"
        }
        return descriptions.get(level, "未知")
    
    def get_triage_color(self, level):
        """获取分诊等级对应颜色"""
        colors = {
            1: "红色",
            2: "橙色",
            3: "黄色",
            4: "绿色",
            5: "蓝色"
        }
        return colors.get(level, "未知")
    
    def get_wait_time(self, level):
        """获取分诊等级对应等待时限"""
        wait_times = {
            1: "立即处理",
            2: "10分钟内",
            3: "30分钟内",
            4: "60分钟内",
            5: "120分钟或预约"
        }
        return wait_times.get(level, "未知")
    
    def input_patient_info(self):
        """手动输入患者基本信息"""
        logger.info("请输入患者基本信息：")
        logger.info("-" * 40)
        
        patient_info = {}
        
        # 姓名（必填）
        while True:
            name = input("患者姓名: ").strip()
            if name:
                patient_info["patientName"] = name
                break
            logger.warning("姓名不能为空，请重新输入")
        
        # 年龄（必填）
        while True:
            try:
                age_input = input("年龄: ").strip()
                age = int(age_input)
                if 0 <= age <= 150:
                    patient_info["patientAge"] = age
                    break
                else:
                    logger.warning("年龄超出合理范围(0-150)，请重新输入")
            except ValueError:
                logger.warning("请输入有效的数字")
        
        # 性别（必填）
        while True:
            gender_input = input("性别 (男/女/其他 或 M/F/O): ").strip()
            gender_map = {
                "男": "MALE", "m": "MALE", "male": "MALE", "M": "MALE",
                "女": "FEMALE", "f": "FEMALE", "female": "FEMALE", "F": "FEMALE",
                "其他": "OTHER", "o": "OTHER", "other": "OTHER", "O": "OTHER"
            }
            if gender_input in gender_map:
                patient_info["patientGender"] = gender_map[gender_input]
                break
            elif gender_input.upper() in ["MALE", "FEMALE", "OTHER"]:
                patient_info["patientGender"] = gender_input.upper()
                break
            else:
                logger.warning("请输入有效的性别（男/女/其他）")
        
        # 身份证号（选填，但建议填写）
        id_card = input("身份证号 (可选，按回车跳过): ").strip()
        if id_card:
            # 简单验证身份证格式
            if len(id_card) == 18 or len(id_card) == 15:
                patient_info["patientIdCard"] = id_card
            else:
                logger.warning("身份证号格式不正确，已跳过")
                patient_info["patientIdCard"] = ""
        else:
            patient_info["patientIdCard"] = ""
        
        # 手机号（选填，但建议填写）
        phone = input("手机号 (可选，按回车跳过): ").strip()
        if phone:
            if len(phone) == 11 and phone.isdigit():
                patient_info["patientPhone"] = phone
            else:
                logger.warning("手机号格式不正确，已跳过")
                patient_info["patientPhone"] = ""
        else:
            patient_info["patientPhone"] = ""
        
        logger.info("-" * 40)
        logger.info("患者信息录入完成: %s, %d岁, %s", 
                   patient_info["patientName"], 
                   patient_info["patientAge"],
                   patient_info["patientGender"])
        
        return patient_info
    
    def send_to_cloud(self, data):
        """通过MQTT发送数据到云端"""
        try:
            message = json.dumps(data, ensure_ascii=False)
            self.mqtt_client.publish(config.MQTT_TOPIC_TRIAGE, message)
            logger.info("✓ 数据已发送到云端")
            return True
        except Exception as e:
            logger.error("MQTT发送失败: %s", str(e))
            return False
    
    def process_patient(self):
        """处理单个患者的完整流程"""
        self.patient_count += 1
        patient_temp_id = f"TEMP_{datetime.now().strftime('%Y%m%d_%H%M%S')}_{self.patient_count}"
        start_time = time.time()
        
        logger.info("")
        logger.info("=" * 60)
        logger.info("开始处理患者: %s", patient_temp_id)
        logger.info("=" * 60)
        
        try:
            # 步骤1: 输入患者基本信息
            logger.info("[1/5] 录入患者基本信息...")
            patient_info = self.input_patient_info()
            
            # 步骤2: 采集生命体征
            logger.info("[2/5] 采集生命体征数据...")
            vital_signs = self.collect_sensor_data()
            if not vital_signs:
                logger.error("生命体征采集失败，终止流程")
                return False
            
            # 步骤3: 录制语音主诉
            logger.info("[3/5] 录制患者主诉...")
            voice_transcript = self.record_voice_complaint()
            
            # 步骤4: 边缘端分诊
            logger.info("[4/5] 执行边缘端分诊...")
            triage_result = self.perform_edge_triage(vital_signs, voice_transcript)
            triage_level = triage_result["triageLevel"]
            
            # 计算处理时间
            processing_time_ms = int((time.time() - start_time) * 1000)
            
            # 步骤5: 上报云端
            logger.info("[5/5] 上报数据到云端...")
            
            # 构建与云端EdgeDeviceData实体一致的MQTT消息
            mqtt_message = {
                # ===== 设备标识 =====
                "deviceId": config.DEVICE_ID,
                "patientTempId": patient_temp_id,
                "timestamp": int(time.time() * 1000),
                
                # ===== 患者基本信息 =====
                "patientName": patient_info["patientName"],
                "patientAge": patient_info["patientAge"],
                "patientGender": patient_info["patientGender"],
                "patientIdCard": patient_info.get("patientIdCard", ""),
                "patientPhone": patient_info.get("patientPhone", ""),
                
                # ===== 生命体征数据 =====
                "temperature": vital_signs["temperature"],
                "heartRate": vital_signs["heartRate"],
                "bloodOxygen": vital_signs["bloodOxygen"],
                "systolicBP": vital_signs["systolicBP"],
                "diastolicBP": vital_signs["diastolicBP"],
                "vitalSigns": json.dumps(vital_signs, ensure_ascii=False),
                
                # ===== 语音数据 =====
                "voiceText": voice_transcript,
                "voiceComplaint": voice_transcript,
                "symptomText": voice_transcript,
                
                # ===== 边缘端AI分诊结果 =====
                "triageLevel": triage_level,
                "triagePriority": triage_result["triageDescription"].split("（")[0],
                "triageColor": self.get_triage_color(triage_level),
                "waitTime": self.get_wait_time(triage_level),
                "triageConfidence": triage_result["confidence"],
                "triageScore": triage_result["confidence"] * 100,
                
                # ===== 处理信息 =====
                "edgeProcessingTime": processing_time_ms,
                "processingTimeMs": processing_time_ms,
                "deviceStatus": "ONLINE",
                "processingStatus": "RECEIVED",
                
                # ===== 原始数据 =====
                "rawSensorData": json.dumps({
                    "raw_temperature": vital_signs.get("raw_temperature"),
                    "raw_heartRate": vital_signs.get("raw_heartRate"),
                    "raw_bloodOxygen": vital_signs.get("raw_bloodOxygen")
                }, ensure_ascii=False)
            }
            
            success = self.send_to_cloud(mqtt_message)
            
            logger.info("=" * 60)
            if success:
                logger.info("✓ 患者 %s (%s) 处理完成", 
                           patient_info["patientName"], patient_temp_id)
                logger.info("  分诊结果: %d级 - %s (%s)", 
                           triage_level, 
                           triage_result["triageDescription"],
                           self.get_triage_color(triage_level))
                logger.info("  处理耗时: %dms", processing_time_ms)
            else:
                logger.warning("⚠ 患者 %s 处理完成，但云端上报失败", patient_temp_id)
            logger.info("=" * 60)
            logger.info("")
            
            return success
            
        except Exception as e:
            logger.error("患者处理流程异常: %s", str(e))
            return False
    
    def send_heartbeat(self):
        """发送设备心跳"""
        while self.running:
            try:
                heartbeat_data = {
                    "deviceId": config.DEVICE_ID,
                    "timestamp": int(time.time() * 1000),
                    "status": "online",
                    "processedPatients": self.patient_count
                }
                self.mqtt_client.publish(
                    config.MQTT_TOPIC_HEARTBEAT,
                    json.dumps(heartbeat_data)
                )
                logger.debug("心跳发送成功")
            except Exception as e:
                logger.warning("心跳发送失败: %s", str(e))
            
            time.sleep(config.HEARTBEAT_INTERVAL)
    
    def run(self):
        """启动系统主循环"""
        self.running = True
        
        # 启动心跳线程
        heartbeat_thread = threading.Thread(target=self.send_heartbeat, daemon=True)
        heartbeat_thread.start()
        logger.info("心跳线程已启动")
        
        logger.info("")
        logger.info("系统已就绪，等待患者...")
        logger.info("按 Ctrl+C 退出系统")
        logger.info("")
        
        try:
            while self.running:
                # 等待用户输入开始处理
                input("按回车键开始处理下一位患者...\n")
                self.process_patient()
                
        except KeyboardInterrupt:
            logger.info("\n收到退出信号，正在关闭系统...")
            self.shutdown()
    
    def shutdown(self):
        """关闭系统"""
        self.running = False
        
        logger.info("正在关闭传感器...")
        self.temp_sensor.close()
        self.pulse_oximeter.close()
        self.microphone.close()
        
        logger.info("正在断开MQTT连接...")
        self.mqtt_client.disconnect()
        
        logger.info("系统已安全关闭")


def main():
    """主函数"""
    try:
        system = EdgeTriageSystem()
        system.run()
    except Exception as e:
        logger.critical("系统启动失败: %s", str(e))
        raise


if __name__ == "__main__":
    main()
