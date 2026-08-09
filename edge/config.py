"""
边缘端配置文件
Jetson Orin Nano Super
"""
import os

# ========== MQTT配置 ==========
MQTT_BROKER = os.getenv('MQTT_BROKER', '192.168.71.7')  # Windows云端IP，Jetson连接用
MQTT_PORT = int(os.getenv('MQTT_PORT', '1883'))
MQTT_CLIENT_ID = os.getenv('MQTT_CLIENT_ID', 'jetson-orin-nano-01')
MQTT_USERNAME = os.getenv('MQTT_USERNAME', '')  # 可选
MQTT_PASSWORD = os.getenv('MQTT_PASSWORD', '')  # 可选
MQTT_TOPIC_TRIAGE = "medical/triage/data"
MQTT_TOPIC_HEARTBEAT = "medical/device/heartbeat"

# ========== 传感器配置 ==========
# DS18B20体温传感器
DS18B20_GPIO_PIN = 4
# 跨平台路径：Linux使用真实路径，Windows使用模拟
import platform
if platform.system() == "Linux":
    DS18B20_DEVICE_PATH = "/sys/bus/w1/devices/"
else:
    DS18B20_DEVICE_PATH = ""  # Windows无此设备，使用模拟模式

# MAX30102心率血氧传感器
MAX30102_I2C_BUS = 7  # Jetson Orin Nano 使用I2C总线7
MAX30102_I2C_ADDRESS = 0x57

# 血压计传感器（模拟接口）
BLOOD_PRESSURE_DEVICE = "/dev/ttyUSB0"  # 血压计串口设备
BLOOD_PRESSURE_BAUD_RATE = 9600

# USB麦克风
USB_MIC_SAMPLE_RATE = 16000  # 16kHz
USB_MIC_CHANNELS = 1
USB_MIC_CHUNK_SIZE = 1024

# ========== 本地语音识别配置 (Vosk离线识别) ==========
VOSK_MODEL_NAME = "vosk-model-small-cn-0.22"  # 中文小模型（约42MB）
VOSK_SAMPLE_RATE = 16000  # 采样率

# ========== AI模型配置 ==========
# BERT-Tiny分诊模型
BERT_TINY_MODEL_PATH = "models/bert-tiny-triage.pth"  # PyTorch模型（训练和部署）
BERT_TINY_TRT_PATH = "models/bert-tiny-triage.trt"  # TensorRT优化后的模型（部署时使用）
BERT_TINY_VOCAB_PATH = "models/vocab.txt"
BERT_TINY_MAX_LENGTH = 128
BERT_TINY_TRIAGE_THRESHOLD = 0.75  # 分诊置信度阈值

# TensorRT配置
TENSORRT_PRECISION = "fp16"  # fp32/fp16/int8
TENSORRT_WORKSPACE_SIZE = 1 << 30  # 1GB

# ========== 卡尔曼滤波配置 ==========
KALMAN_PROCESS_NOISE = 0.01   # 过程噪声
KALMAN_MEASUREMENT_NOISE = 0.1  # 测量噪声
KALMAN_INITIAL_ESTIMATE = 37.0  # 初始估计（体温）

# ========== 边缘规则引擎配置 ==========
# 分诊等级判断规则
TRIAGE_RULES = {
    "temperature": {
        "level_1": {"min": 41.0, "max": 45.0},  # 濒危
        "level_2": {"min": 39.5, "max": 41.0},  # 危急
        "level_3": {"min": 38.5, "max": 39.5},  # 急症
        "level_4": {"min": 37.5, "max": 38.5},  # 次急症
        "level_5": {"min": 35.0, "max": 37.5}   # 非急症
    },
    "systolic_bp": {
        "level_1": {"min": 0, "max": 70, "or_min": 200, "or_max": 250},  # 濒危：严重低血压或高血压危象
        "level_2": {"min": 70, "max": 90, "or_min": 180, "or_max": 200},  # 危急：低血压或高血压危象
        "level_3": {"min": 160, "max": 180},  # 急症：高血压
        "level_4": {"min": 140, "max": 160},  # 次急症：血压偏高
        "level_5": {"min": 90, "max": 140}    # 非急症：正常范围
    },
    "diastolic_bp": {
        "level_1": {"min": 0, "max": 50, "or_min": 110, "or_max": 150},  # 濒危
        "level_2": {"min": 50, "max": 60, "or_min": 100, "or_max": 110},  # 危急
        "level_3": {"min": 95, "max": 100},   # 急症
        "level_4": {"min": 90, "max": 95},    # 次急症
        "level_5": {"min": 60, "max": 90}     # 非急症
    },
    "heart_rate": {
        "level_1": {"min": 140, "max": 200, "or_min": 0, "or_max": 40},
        "level_2": {"min": 120, "max": 140, "or_min": 40, "or_max": 50},
        "level_3": {"min": 100, "max": 120, "or_min": 50, "or_max": 60},
        "level_4": {"min": 90, "max": 100},
        "level_5": {"min": 60, "max": 90}
    },
    "blood_oxygen": {
        "level_1": {"min": 0, "max": 85},
        "level_2": {"min": 85, "max": 90},
        "level_3": {"min": 90, "max": 93},
        "level_4": {"min": 93, "max": 95},
        "level_5": {"min": 95, "max": 100}
    }
}

# ========== 系统配置 ==========
DEVICE_ID = "jetson-orin-nano-01"
SQLITE_DB_PATH = "edge_cache.db"
LOG_LEVEL = "INFO"
LOG_FILE = "edge_system.log"

# 心跳间隔（秒）
HEARTBEAT_INTERVAL = 30

# 数据采集间隔（秒）
SENSOR_POLL_INTERVAL = 2

# 缓存配置
MAX_CACHE_SIZE = 1000  # 最多缓存1000条记录
CACHE_UPLOAD_BATCH = 50  # 每次批量上传50条
