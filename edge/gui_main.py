"""
边缘端分诊系统 - GUI界面版本
Jetson Orin Nano - 多模态急诊分诊系统

使用Tkinter构建图形界面，适合触摸屏操作
"""

import os

import sys
import time
import json
import threading
import logging
import platform
import queue
from datetime import datetime
from tkinter import *
from tkinter import ttk, messagebox
from tkinter.scrolledtext import ScrolledText

# 设置环境变量
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'
os.environ['HF_HUB_DISABLE_SYMLINKS_WARNING'] = '1'

import config
from models.bert_tiny_triage import BERTTinyTriage
from models.rule_engine import EdgeRuleEngine
from mqtt_client.mqtt_publisher import MQTTPublisher
from preprocessing.local_voice_recognizer import LocalVoiceRecognizer, VOSK_AVAILABLE
from sensors.max30102_driver import MAX30102Sensor

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# 跨平台字体配置
def get_system_font():
    """根据操作系统返回合适的字体"""
    system = platform.system()
    if system == "Windows":
        return "Microsoft YaHei"
    elif system == "Darwin":  # macOS
        return "PingFang SC"
    else:  # Linux/Jetson
        # 优先使用文泉驿微米黑，其次是Noto Sans CJK
        return "WenQuanYi Micro Hei"

# 获取系统字体
SYSTEM_FONT = get_system_font()
EMOJI_FONT = "Segoe UI Emoji" if platform.system() == "Windows" else "Noto Color Emoji"

# 预定义字体元组（供全局使用）
FONT_TITLE = (SYSTEM_FONT, 18, "bold")
FONT_SECTION = (SYSTEM_FONT, 14, "bold")
FONT_LABEL = (SYSTEM_FONT, 11)
FONT_LABEL_BOLD = (SYSTEM_FONT, 11, "bold")
FONT_SMALL = (SYSTEM_FONT, 10)
FONT_TINY = (SYSTEM_FONT, 8)
FONT_RESULT = (SYSTEM_FONT, 24, "bold")
FONT_RESULT_DESC = (SYSTEM_FONT, 12)
FONT_BUTTON = (SYSTEM_FONT, 14, "bold")
FONT_EMOJI = (EMOJI_FONT, 24)
FONT_LOG = ("Consolas", 9) if platform.system() == "Windows" else ("Monospace", 9)


class EdgeTriageGUI:
    """边缘端分诊系统GUI"""
    
    # 分诊等级配置
    TRIAGE_CONFIG = {
        1: {"name": "濒危", "color": "#FF0000", "bg": "#FFE0E0", "wait": "立即处理"},
        2: {"name": "危急", "color": "#FF6600", "bg": "#FFE8D0", "wait": "10分钟内"},
        3: {"name": "急症", "color": "#FFCC00", "bg": "#FFFFD0", "wait": "30分钟内"},
        4: {"name": "次急症", "color": "#00CC00", "bg": "#E0FFE0", "wait": "60分钟内"},
        5: {"name": "非急症", "color": "#0066FF", "bg": "#E0E8FF", "wait": "120分钟或预约"}
    }
    
    def __init__(self):
        print("[DEBUG] 开始初始化GUI...")
        self.root = Tk()
        self.root.title("边缘端智能分诊系统 - Jetson Orin Nano")
        
        # 屏幕自适应大小
        screen_width = self.root.winfo_screenwidth()
        screen_height = self.root.winfo_screenheight()
        self.root.geometry(f"{screen_width}x{screen_height}+0+0")
        
        self.root.configure(bg="#F0F4F8")
        print(f"[DEBUG] 窗口大小: {screen_width}x{screen_height}")
        
        # 系统状态
        self.patient_count = 0
        self.mqtt_connected = False
        self.model_loaded = False
        
        # 消息队列（线程安全）
        self.message_queue = queue.Queue()
        
        # 初始化组件
        self.bert_model = None
        self.rule_engine = None
        self.mqtt_client = None
        
        # 语音识别器（本地离线Vosk）
        self.voice_recognizer = None
        self.is_recording = False
        
        # 传感器
        self.heart_sensor = None
        self.sensor_connected = False
        
        # 创建界面
        print("[DEBUG] 开始创建界面组件...")
        try:
            self.create_widgets()
            print("[DEBUG] 界面组件创建成功")
        except Exception as e:
            print(f"[ERROR] 界面创建失败: {e}")
            import traceback
            traceback.print_exc()
            return
        
        # 延迟异步初始化模型（增加延迟到2秒，让GUI先稳定）
        print("[DEBUG] 计划2秒后初始化系统...")
        self.root.after(2000, self.init_system_async)
        
        # 启动消息队列检查循环
        self.root.after(3000, self.check_message_queue)
    
    def create_widgets(self):
        """创建界面组件"""
        # 主容器
        main_frame = Frame(self.root, bg="#F0F4F8")
        main_frame.pack(fill=BOTH, expand=True, padx=20, pady=20)
        
        # ===== 顶部标题栏 =====
        self.create_header(main_frame)
        
        # ===== 中间内容区 =====
        content_frame = Frame(main_frame, bg="#F0F4F8")
        content_frame.pack(fill=BOTH, expand=True, pady=10)
        
        # 左侧：患者信息输入
        left_frame = Frame(content_frame, bg="white", relief=RAISED, bd=1)
        left_frame.pack(side=LEFT, fill=BOTH, expand=True, padx=(0, 10))
        self.create_patient_input(left_frame)
        
        # 右侧：生命体征和分诊结果
        right_frame = Frame(content_frame, bg="#F0F4F8")
        right_frame.pack(side=RIGHT, fill=BOTH, expand=True)
        
        # 右上：生命体征
        vital_frame = Frame(right_frame, bg="white", relief=RAISED, bd=1)
        vital_frame.pack(fill=BOTH, expand=True, pady=(0, 10))
        self.create_vital_signs(vital_frame)
        
        # 右下：分诊结果
        result_frame = Frame(right_frame, bg="white", relief=RAISED, bd=1)
        result_frame.pack(fill=BOTH, expand=True)
        self.create_triage_result(result_frame)
        
        # ===== 底部操作区 =====
        self.create_footer(main_frame)
    
    def create_header(self, parent):
        """创建顶部标题栏"""
        header = Frame(parent, bg="#1E3A5F", height=60)
        header.pack(fill=X)
        header.pack_propagate(False)
        
        # 标题
        title = Label(header, text="[医] 边缘端智能分诊系统", 
                     font=FONT_TITLE,
                     bg="#1E3A5F", fg="white")
        title.pack(side=LEFT, padx=20, pady=15)
        
        # 状态指示器
        status_frame = Frame(header, bg="#1E3A5F")
        status_frame.pack(side=RIGHT, padx=20)
        
        self.model_status = Label(status_frame, text="● 模型加载中", 
                                  font=FONT_SMALL,
                                  bg="#1E3A5F", fg="#FFA500")
        self.model_status.pack(side=LEFT, padx=10)
        
        self.mqtt_status = Label(status_frame, text="● MQTT未连接", 
                                font=FONT_SMALL,
                                bg="#1E3A5F", fg="#FF6666")
        self.mqtt_status.pack(side=LEFT, padx=10)
        
        # 设备ID
        device_label = Label(status_frame, text=f"设备: {config.DEVICE_ID}", 
                            font=FONT_SMALL,
                            bg="#1E3A5F", fg="#AAAAAA")
        device_label.pack(side=LEFT, padx=10)
    
    def create_patient_input(self, parent):
        """创建患者信息输入区"""
        # 标题
        title_frame = Frame(parent, bg="#E8F4FD")
        title_frame.pack(fill=X)
        Label(title_frame, text="[患者] 患者信息", font=FONT_SECTION,
              bg="#E8F4FD", fg="#1E3A5F").pack(pady=10)
        
        # 输入表单
        form_frame = Frame(parent, bg="white")
        form_frame.pack(fill=BOTH, expand=True, padx=20, pady=10)
        
        # 姓名
        self.create_input_row(form_frame, "姓名 *", "name_entry", 0)
        
        # 年龄
        self.create_input_row(form_frame, "年龄 *", "age_entry", 1)
        
        # 性别
        Label(form_frame, text="性别 *", font=FONT_LABEL,
              bg="white", anchor=W).grid(row=2, column=0, sticky=W, pady=8)
        gender_frame = Frame(form_frame, bg="white")
        gender_frame.grid(row=2, column=1, sticky=W, pady=8)
        
        self.gender_var = StringVar(value="MALE")
        Radiobutton(gender_frame, text="男", variable=self.gender_var, value="MALE",
                   font=FONT_LABEL, bg="white").pack(side=LEFT, padx=10)
        Radiobutton(gender_frame, text="女", variable=self.gender_var, value="FEMALE",
                   font=FONT_LABEL, bg="white").pack(side=LEFT, padx=10)
        Radiobutton(gender_frame, text="其他", variable=self.gender_var, value="OTHER",
                   font=FONT_LABEL, bg="white").pack(side=LEFT, padx=10)
        
        # 身份证号
        self.create_input_row(form_frame, "身份证号", "idcard_entry", 3)
        
        # 症状/主诉
        symptom_frame = Frame(form_frame, bg="white")
        symptom_frame.grid(row=4, column=0, columnspan=2, sticky=W, pady=8)
        
        Label(symptom_frame, text="症状主诉 *", font=FONT_LABEL,
              bg="white", anchor=W).pack(anchor=W)
        
        # 症状输入区域
        symptom_input_frame = Frame(symptom_frame, bg="white")
        symptom_input_frame.pack(fill=X, pady=5)
        
        self.symptom_text = Text(symptom_input_frame, height=4, width=28, 
                                font=FONT_LABEL,
                                relief=SOLID, bd=1)
        self.symptom_text.pack(side=LEFT)
        self.symptom_text.insert("1.0", "请输入或语音输入症状...")
        self.symptom_text.bind("<FocusIn>", self.clear_placeholder)
        
        # 语音输入按钮
        voice_btn_frame = Frame(symptom_input_frame, bg="white")
        voice_btn_frame.pack(side=LEFT, padx=10)
        
        self.voice_btn = Button(voice_btn_frame, text="MIC", 
                               font=FONT_BUTTON,
                               bg="#4A90D9", fg="white",
                               width=4, height=1,
                               relief=FLAT, cursor="hand2",
                               command=self.start_voice_input)
        self.voice_btn.pack()
        
        self.voice_status = Label(voice_btn_frame, text="点击语音输入", 
                                  font=FONT_TINY,
                                  bg="white", fg="#666666")
        self.voice_status.pack(pady=2)
    
    def create_input_row(self, parent, label, entry_name, row):
        """创建输入行"""
        Label(parent, text=label, font=FONT_LABEL,
              bg="white", anchor=W).grid(row=row, column=0, sticky=W, pady=8)
        entry = Entry(parent, font=FONT_LABEL, width=25,
                     relief=SOLID, bd=1)
        entry.grid(row=row, column=1, sticky=W, pady=8, ipady=5)
        setattr(self, entry_name, entry)
    
    def create_vital_signs(self, parent):
        """创建生命体征输入区"""
        # 标题
        title_frame = Frame(parent, bg="#E8F4FD")
        title_frame.pack(fill=X)
        Label(title_frame, text="[心] 生命体征", font=FONT_SECTION,
              bg="#E8F4FD", fg="#1E3A5F").pack(pady=10)
        
        # 生命体征网格
        vital_frame = Frame(parent, bg="white")
        vital_frame.pack(fill=BOTH, expand=True, padx=20, pady=10)
        
        # 体温
        self.create_vital_input(vital_frame, "体温", "temp_entry", "°C", 0, 0)
        
        # 心率
        self.create_vital_input(vital_frame, "心率", "hr_entry", "bpm", 0, 1)
        
        # 血氧
        self.create_vital_input(vital_frame, "血氧", "spo2_entry", "%", 1, 0)
        
        # 血压
        Label(vital_frame, text="血压", font=FONT_LABEL_BOLD,
              bg="white").grid(row=1, column=1, sticky=W, pady=10, padx=5)
        
        bp_frame = Frame(vital_frame, bg="white")
        bp_frame.grid(row=1, column=1, sticky=E, pady=10)
        
        self.sbp_entry = Entry(bp_frame, font=FONT_LABEL, width=6,
                              relief=SOLID, bd=1, justify=CENTER)
        self.sbp_entry.pack(side=LEFT, ipady=5)
        Label(bp_frame, text="/", font=FONT_LABEL, bg="white").pack(side=LEFT)
        self.dbp_entry = Entry(bp_frame, font=FONT_LABEL, width=6,
                              relief=SOLID, bd=1, justify=CENTER)
        self.dbp_entry.pack(side=LEFT, ipady=5)
        Label(bp_frame, text="mmHg", font=FONT_SMALL, 
              bg="white", fg="gray").pack(side=LEFT, padx=5)
        
        # 传感器按钮区域
        sensor_btn_frame = Frame(vital_frame, bg="white")
        sensor_btn_frame.grid(row=2, column=0, columnspan=2, pady=15)
        
        # 读取传感器按钮
        self.sensor_btn = Button(sensor_btn_frame, text="[传感器] 读取心率血氧", 
                        font=FONT_SMALL,
                        bg="#28A745", fg="white", relief=FLAT,
                        command=self.read_sensor_data)
        self.sensor_btn.pack(side=LEFT, padx=5)
        
        # 传感器状态
        self.sensor_status = Label(vital_frame, text="传感器: 未连接",
                                   font=FONT_TINY, bg="white", fg="#999999")
        self.sensor_status.grid(row=3, column=0, columnspan=2, pady=5)
    
    def create_vital_input(self, parent, label, entry_name, unit, row, col):
        """创建生命体征输入项"""
        frame = Frame(parent, bg="white")
        frame.grid(row=row, column=col, sticky=W, pady=10, padx=5)
        
        Label(frame, text=label, font=FONT_LABEL_BOLD,
              bg="white").pack(side=LEFT)
        
        entry = Entry(frame, font=FONT_LABEL, width=8,
                     relief=SOLID, bd=1, justify=CENTER)
        entry.pack(side=LEFT, padx=10, ipady=5)
        setattr(self, entry_name, entry)
        
        Label(frame, text=unit, font=FONT_SMALL,
              bg="white", fg="gray").pack(side=LEFT)
    
    def create_triage_result(self, parent):
        """创建分诊结果显示区"""
        # 标题
        title_frame = Frame(parent, bg="#E8F4FD")
        title_frame.pack(fill=X)
        Label(title_frame, text="[结果] 分诊结果", font=FONT_SECTION,
              bg="#E8F4FD", fg="#1E3A5F").pack(pady=10)
        
        # 结果显示
        result_frame = Frame(parent, bg="white")
        result_frame.pack(fill=BOTH, expand=True, padx=20, pady=10)
        
        # 分诊等级显示
        self.triage_level_label = Label(result_frame, text="等待分诊",
                                        font=FONT_RESULT,
                                        bg="white", fg="#999999")
        self.triage_level_label.pack(pady=10)
        
        # 分诊描述
        self.triage_desc_label = Label(result_frame, text="请输入患者信息并点击开始分诊",
                                       font=FONT_RESULT_DESC,
                                       bg="white", fg="#666666")
        self.triage_desc_label.pack(pady=5)
        
        # 置信度
        self.confidence_label = Label(result_frame, text="",
                                      font=FONT_LABEL,
                                      bg="white", fg="#888888")
        self.confidence_label.pack(pady=5)
        
        # 处理时间
        self.processing_time_label = Label(result_frame, text="",
                                           font=FONT_SMALL,
                                           bg="white", fg="#AAAAAA")
        self.processing_time_label.pack(pady=5)
    
    def create_footer(self, parent):
        """创建底部操作区"""
        footer = Frame(parent, bg="#F0F4F8", height=80)
        footer.pack(fill=X, pady=10)
        
        # 按钮区
        btn_frame = Frame(footer, bg="#F0F4F8")
        btn_frame.pack(expand=True)
        
        # 开始分诊按钮
        self.triage_btn = Button(btn_frame, text=">> 开始分诊", 
                                font=FONT_BUTTON,
                                bg="#28A745", fg="white", 
                                width=15, height=2,
                                relief=FLAT, cursor="hand2",
                                command=self.start_triage)
        self.triage_btn.pack(side=LEFT, padx=20)
        
        # 清空按钮
        clear_btn = Button(btn_frame, text="[清空] 清空数据", 
                          font=FONT_BUTTON,
                          bg="#6C757D", fg="white",
                          width=15, height=2,
                          relief=FLAT, cursor="hand2",
                          command=self.clear_all)
        clear_btn.pack(side=LEFT, padx=20)
        
        # 上报云端按钮 - 加大
        self.upload_btn = Button(btn_frame, text="[云] 上报云端", 
                                font=(SYSTEM_FONT, 16, "bold"),
                                bg="#007BFF", fg="white",
                                width=18, height=2,
                                relief=FLAT, cursor="hand2",
                                state=DISABLED,
                                command=self.upload_to_cloud)
        self.upload_btn.pack(side=LEFT, padx=20)
        
        # 日志区
        log_frame = Frame(parent, bg="white", relief=RAISED, bd=1)
        log_frame.pack(fill=X, pady=10)
        
        Label(log_frame, text="[日志] 系统日志", font=(SYSTEM_FONT, 10, "bold"),
              bg="#E8F4FD", anchor=W).pack(fill=X)
        
        self.log_text = ScrolledText(log_frame, height=4, font=FONT_LOG,
                                     bg="#F8F9FA", relief=FLAT)
        self.log_text.pack(fill=X, padx=5, pady=5)
    
    def create_header(self, parent):
        """创建顶部标题栏 - 紧凑版"""
        header = Frame(parent, bg="#1E3A5F", height=28)
        header.pack(fill=X)
        header.pack_propagate(False)
        
        # 标题
        title = Label(header, text="边缘端分诊系统", 
                     font=FONT_TITLE,
                     bg="#1E3A5F", fg="white")
        title.pack(side=LEFT, padx=5, pady=3)
        
        # 状态指示器
        status_frame = Frame(header, bg="#1E3A5F")
        status_frame.pack(side=RIGHT, padx=5)
        
        self.model_status = Label(status_frame, text="● 模型", 
                                  font=FONT_TINY,
                                  bg="#1E3A5F", fg="#FFA500")
        self.model_status.pack(side=LEFT, padx=3)
        
        self.mqtt_status = Label(status_frame, text="● MQTT", 
                                font=FONT_TINY,
                                bg="#1E3A5F", fg="#FF6666")
        self.mqtt_status.pack(side=LEFT, padx=3)
    
    def create_patient_input(self, parent):
        """创建患者信息输入区 - 紧凑版"""
        # 标题
        title_frame = Frame(parent, bg="#E8F4FD")
        title_frame.pack(fill=X)
        Label(title_frame, text="患者信息", font=FONT_SECTION,
              bg="#E8F4FD", fg="#1E3A5F").pack(pady=2)
        
        # 输入表单
        form_frame = Frame(parent, bg="white")
        form_frame.pack(fill=BOTH, expand=True, padx=5, pady=2)
        
        # 姓名和年龄同一行
        row0 = Frame(form_frame, bg="white")
        row0.pack(fill=X, pady=1)
        Label(row0, text="姓名:", font=FONT_LABEL, bg="white").pack(side=LEFT)
        self.name_entry = Entry(row0, font=FONT_LABEL, width=8)
        self.name_entry.pack(side=LEFT, padx=2)
        Label(row0, text="年龄:", font=FONT_LABEL, bg="white").pack(side=LEFT, padx=(5,0))
        self.age_entry = Entry(row0, font=FONT_LABEL, width=4)
        self.age_entry.pack(side=LEFT, padx=2)
        
        # 性别
        row1 = Frame(form_frame, bg="white")
        row1.pack(fill=X, pady=1)
        Label(row1, text="性别:", font=FONT_LABEL, bg="white").pack(side=LEFT)
        self.gender_var = StringVar(value="MALE")
        Radiobutton(row1, text="男", variable=self.gender_var, value="MALE",
                   font=FONT_TINY, bg="white").pack(side=LEFT)
        Radiobutton(row1, text="女", variable=self.gender_var, value="FEMALE",
                   font=FONT_TINY, bg="white").pack(side=LEFT)
        
        # 身份证
        row2 = Frame(form_frame, bg="white")
        row2.pack(fill=X, pady=1)
        Label(row2, text="身份证:", font=FONT_LABEL, bg="white").pack(side=LEFT)
        self.idcard_entry = Entry(row2, font=FONT_LABEL, width=18)
        self.idcard_entry.pack(side=LEFT, padx=2)
        
        # 症状
        row4 = Frame(form_frame, bg="white")
        row4.pack(fill=X, pady=1)
        Label(row4, text="症状:", font=FONT_LABEL, bg="white").pack(side=LEFT, anchor=N)
        self.symptom_text = Text(row4, height=2, width=20, font=FONT_LABEL, relief=SOLID, bd=1)
        self.symptom_text.pack(side=LEFT, padx=2)
        self.symptom_text.insert("1.0", "输入症状...")
        self.symptom_text.bind("<FocusIn>", self.clear_placeholder)
        
        # 语音按钮
        self.voice_btn = Button(row4, text="MIC", font=FONT_TINY,
                               bg="#4A90D9", fg="white", width=3,
                               command=self.start_voice_input)
        self.voice_btn.pack(side=LEFT, padx=2)
        self.voice_status = Label(form_frame, text="", font=FONT_TINY, bg="white", fg="#666")
        self.voice_status.pack()
    
    def create_input_row(self, parent, label, entry_name, row):
        """创建输入行 - 紧凑版"""
        Label(parent, text=label, font=FONT_LABEL,
              bg="white", anchor=W).grid(row=row, column=0, sticky=W, pady=2)
        entry = Entry(parent, font=FONT_LABEL, width=18,
                     relief=SOLID, bd=1)
        entry.grid(row=row, column=1, sticky=W, pady=2, ipady=2)
        setattr(self, entry_name, entry)
    
    def create_vital_signs(self, parent):
        """创建生命体征输入区 - 紧凑版"""
        # 标题
        title_frame = Frame(parent, bg="#E8F4FD")
        title_frame.pack(fill=X)
        Label(title_frame, text="生命体征", font=FONT_SECTION,
              bg="#E8F4FD", fg="#1E3A5F").pack(pady=2)
        
        # 生命体征网格
        vital_frame = Frame(parent, bg="white")
        vital_frame.pack(fill=BOTH, expand=True, padx=5, pady=2)
        
        # 第一行: 体温 心率
        row0 = Frame(vital_frame, bg="white")
        row0.pack(fill=X, pady=1)
        Label(row0, text="体温:", font=FONT_LABEL, bg="white").pack(side=LEFT)
        self.temp_entry = Entry(row0, font=FONT_LABEL, width=5)
        self.temp_entry.pack(side=LEFT)
        Label(row0, text="°C", font=FONT_TINY, bg="white", fg="gray").pack(side=LEFT)
        Label(row0, text="心率:", font=FONT_LABEL, bg="white").pack(side=LEFT, padx=(8,0))
        self.hr_entry = Entry(row0, font=FONT_LABEL, width=5)
        self.hr_entry.pack(side=LEFT)
        Label(row0, text="bpm", font=FONT_TINY, bg="white", fg="gray").pack(side=LEFT)
        
        # 第二行: 血氧 血压
        row1 = Frame(vital_frame, bg="white")
        row1.pack(fill=X, pady=1)
        Label(row1, text="血氧:", font=FONT_LABEL, bg="white").pack(side=LEFT)
        self.spo2_entry = Entry(row1, font=FONT_LABEL, width=5)
        self.spo2_entry.pack(side=LEFT)
        Label(row1, text="%", font=FONT_TINY, bg="white", fg="gray").pack(side=LEFT)
        Label(row1, text="血压:", font=FONT_LABEL, bg="white").pack(side=LEFT, padx=(8,0))
        self.sbp_entry = Entry(row1, font=FONT_LABEL, width=4)
        self.sbp_entry.pack(side=LEFT)
        Label(row1, text="/", font=FONT_LABEL, bg="white").pack(side=LEFT)
        self.dbp_entry = Entry(row1, font=FONT_LABEL, width=4)
        self.dbp_entry.pack(side=LEFT)
        
        # 传感器按钮
        btn_frame = Frame(vital_frame, bg="white")
        btn_frame.pack(fill=X, pady=2)
        self.sensor_btn = Button(btn_frame, text="读取传感器", 
                        font=FONT_TINY, bg="#28A745", fg="white",
                        command=self.read_sensor_data)
        self.sensor_btn.pack(side=LEFT)
        self.sensor_status = Label(btn_frame, text="未连接",
                                   font=FONT_TINY, bg="white", fg="#999")
        self.sensor_status.pack(side=LEFT, padx=5)
    
    def create_vital_input(self, parent, label, entry_name, unit, row, col):
        """创建生命体征输入项"""
        frame = Frame(parent, bg="white")
        frame.grid(row=row, column=col, sticky=W, pady=10, padx=5)
        
        Label(frame, text=label, font=FONT_LABEL_BOLD,
              bg="white").pack(side=LEFT)
        
        entry = Entry(frame, font=FONT_LABEL, width=8,
                     relief=SOLID, bd=1, justify=CENTER)
        entry.pack(side=LEFT, padx=10, ipady=5)
        setattr(self, entry_name, entry)
        
        Label(frame, text=unit, font=FONT_SMALL,
              bg="white", fg="gray").pack(side=LEFT)
    
    def create_triage_result(self, parent):
        """创建分诊结果显示区 - 紧凑版"""
        # 标题
        title_frame = Frame(parent, bg="#E8F4FD")
        title_frame.pack(fill=X)
        Label(title_frame, text="分诊结果", font=FONT_SECTION,
              bg="#E8F4FD", fg="#1E3A5F").pack(pady=2)
        
        # 结果显示
        result_frame = Frame(parent, bg="white")
        result_frame.pack(fill=BOTH, expand=True, padx=5, pady=2)
        
        # 分诊等级显示
        self.triage_level_label = Label(result_frame, text="等待分诊",
                                        font=FONT_RESULT,
                                        bg="white", fg="#999999")
        self.triage_level_label.pack(pady=2)
        
        # 分诊描述
        self.triage_desc_label = Label(result_frame, text="请输入患者信息",
                                       font=FONT_TINY,
                                       bg="white", fg="#666666")
        self.triage_desc_label.pack()
        
        # 置信度
        self.confidence_label = Label(result_frame, text="",
                                      font=FONT_TINY,
                                      bg="white", fg="#888888")
        self.confidence_label.pack()
        
        # 处理时间
        self.processing_time_label = Label(result_frame, text="",
                                           font=FONT_TINY,
                                           bg="white", fg="#AAAAAA")
        self.processing_time_label.pack()
    
    def create_footer(self, parent):
        """创建底部操作区 - 紧凑版"""
        footer = Frame(parent, bg="#F0F4F8")
        footer.pack(fill=X, pady=2)
        
        # 按钮区 - 横向排列
        btn_frame = Frame(footer, bg="#F0F4F8")
        btn_frame.pack(expand=True)
        
        # 开始分诊按钮
        self.triage_btn = Button(btn_frame, text="开始分诊", 
                                font=FONT_BUTTON,
                                bg="#28A745", fg="white", 
                                width=8,
                                command=self.start_triage)
        self.triage_btn.pack(side=LEFT, padx=3)
        
        # 清空按钮
        clear_btn = Button(btn_frame, text="清空", 
                          font=FONT_BUTTON,
                          bg="#6C757D", fg="white",
                          width=5,
                          command=self.clear_all)
        clear_btn.pack(side=LEFT, padx=3)
        
        # 上报云端按钮
        self.upload_btn = Button(btn_frame, text="上报云端", 
                                font=FONT_BUTTON,
                                bg="#007BFF", fg="white",
                                width=8,
                                state=DISABLED,
                                command=self.upload_to_cloud)
        self.upload_btn.pack(side=LEFT, padx=3)
        
        # 日志区 - 只显示2行
        log_frame = Frame(parent, bg="white", relief=RAISED, bd=1)
        log_frame.pack(fill=X, pady=2)
        
        self.log_text = ScrolledText(log_frame, height=2, font=FONT_LOG,
                                     bg="#F8F9FA", relief=FLAT)
        self.log_text.pack(fill=X, padx=2, pady=2)
    
    def init_system_async(self):
        """异步初始化系统"""
        print("[DEBUG] init_system_async被调用")
        try:
            self.log("正在初始化系统...")
            threading.Thread(target=self.init_system, daemon=True).start()
        except Exception as e:
            print(f"[ERROR] init_system_async失败: {e}")
            import traceback
            traceback.print_exc()
    
    def init_system(self):
        """初始化系统组件"""
        print("[DEBUG] init_system开始执行")
        try:
            # 加载规则引擎
            print("[DEBUG] 加载规则引擎...")
            self.log("正在加载规则引擎...")
            self.rule_engine = EdgeRuleEngine()
            self.log("✓ 规则引擎加载完成")
            print("[DEBUG] 规则引擎加载完成")
            
            # 加载BERT模型
            print("[DEBUG] 加载BERT模型...")
            self.log("正在加载BERT-Tiny分诊模型...")
            try:
                self.bert_model = BERTTinyTriage()
                self.model_loaded = True
                self.log("✓ BERT-Tiny模型加载完成")
                print("[DEBUG] BERT模型加载完成")
                self.root.after(0, lambda: self.model_status.config(
                    text="● 模型就绪", fg="#00FF00"))
            except Exception as e:
                print(f"[DEBUG] BERT模型加载失败: {e}")
                self.log(f"⚠ BERT模型加载失败: {e}，将使用规则引擎")
                self.root.after(0, lambda: self.model_status.config(
                    text="● 使用规则引擎", fg="#FFA500"))
            
            # 连接MQTT
            print("[DEBUG] 连接MQTT...")
            self.log("正在连接MQTT服务器...")
            try:
                self.mqtt_client = MQTTPublisher()
                # 注册云端消息回调
                self.mqtt_client.register_callback(self.handle_cloud_message)
                # 调用connect()方法建立连接
                if self.mqtt_client.connect():
                    self.mqtt_connected = True
                    self.log("✓ MQTT连接成功")
                    self.root.after(0, lambda: self.mqtt_status.config(
                        text="● MQTT已连接", fg="#00FF00"))
                else:
                    self.mqtt_connected = False
                    self.log("⚠ MQTT连接失败")
                    self.root.after(0, lambda: self.mqtt_status.config(
                        text="● MQTT未连接", fg="#FF0000"))
            except Exception as e:
                print(f"[DEBUG] MQTT连接失败: {e}")
                self.log(f"⚠ MQTT连接失败: {e}")
            
            # 初始化心率血氧传感器
            print("[DEBUG] 初始化传感器...")
            self.log("正在初始化MAX30102心率血氧传感器...")
            try:
                self.heart_sensor = MAX30102Sensor()
                self.sensor_connected = True
                self.log("✓ MAX30102传感器连接成功")
                self.root.after(0, lambda: self.sensor_status.config(
                    text="传感器: 已连接", fg="#00CC00"))
            except Exception as e:
                self.sensor_connected = False
                self.log(f"X MAX30102传感器连接失败: {e}")
                self.root.after(0, lambda: self.sensor_status.config(
                    text="传感器: 未连接", fg="#FF0000"))
            
            # 初始化本地语音识别（后台加载，不阻塞主流程）
            print("[DEBUG] 启动语音识别后台加载...")
            self.log("正在后台加载Vosk本地语音识别模型...")
            threading.Thread(target=self.init_voice_recognizer, daemon=True).start()
            
            self.log("系统初始化完成，可以开始分诊")
            self.log("注意: 语音识别模型正在后台加载，首次使用需下载约42MB")
            print("[DEBUG] init_system执行完成")
            
        except Exception as e:
            print(f"[ERROR] init_system失败: {e}")
            import traceback
            traceback.print_exc()
            self.log(f"❌ 系统初始化失败: {e}")
    
    def init_voice_recognizer(self):
        """后台初始化语音识别模型"""
        try:
            self.voice_recognizer = LocalVoiceRecognizer()
            if self.voice_recognizer.is_available():
                self.log("✓ Vosk本地语音识别加载完成（离线模式）")
            else:
                self.log("⚠ Vosk模型加载失败")
        except Exception as e:
            self.log(f"⚠ 语音识别初始化失败: {e}")
    
    def handle_cloud_message(self, topic, data):
        """处理云端消息 - 放入队列"""
        try:
            msg_data = dict(data) if isinstance(data, dict) else {}
            self.message_queue.put(('reassessment', str(topic), msg_data))
        except:
            pass
    
    def check_message_queue(self):
        """检查消息队列"""
        try:
            while not self.message_queue.empty():
                msg = self.message_queue.get_nowait()
                if msg[0] == 'reassessment':
                    topic, data = msg[1], msg[2]
                    self.log("[MQTT] 收到云端消息")
                    if 'reassessment' in topic:
                        self._show_reassess_alert(data)
        except:
            pass
        self.root.after(500, self.check_message_queue)
    
    def _show_reassess_alert(self, data):
        """显示重新评估提示"""
        try:
            name = data.get('patientName', '') if data else ''
            reason = data.get('reason', '') if data else ''
            msg = "[护士请求重新评估]\n"
            if name:
                msg += "患者: " + str(name) + "\n"
            if reason:
                msg += "原因: " + str(reason) + "\n"
            msg += "请引导患者重新采集数据"
            self.log("收到重新评估请求")
            messagebox.showinfo("提示", msg)
        except:
            pass
    
    def log(self, message):
        """添加日志"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        log_msg = f"[{timestamp}] {message}\n"
        
        def update():
            self.log_text.insert(END, log_msg)
            self.log_text.see(END)
        
        self.root.after(0, update)
        logger.info(message)
    
    def clear_placeholder(self, event):
        """清除占位符"""
        current_text = self.symptom_text.get("1.0", END).strip()
        placeholders = [
            "请输入患者症状描述...", 
            "请输入或语音输入症状...",
            "输入症状..."
        ]
        if current_text in placeholders:
            self.symptom_text.delete("1.0", END)
    
    def start_voice_input(self):
        """开始语音输入（本地离线）"""
        if not self.voice_recognizer or not self.voice_recognizer.is_available():
            messagebox.showwarning("提示", "本地语音识别模型未加载\n请等待模型下载完成")
            return
        
        if self.is_recording:
            return
        
        self.is_recording = True
        self.voice_btn.config(bg="#FF4444")
        self.voice_status.config(text="* 正在录音...", fg="#FF0000")
        self.log("开始本地语音识别（离线模式），请说出症状...")
        
        # 在后台线程执行语音识别
        threading.Thread(target=self.do_voice_recognition, daemon=True).start()
    
    def do_voice_recognition(self):
        """执行本地离线语音识别"""
        try:
            def status_callback(status):
                self.root.after(0, lambda: self.voice_status.config(
                    text=f"* {status}", fg="#0066FF"))
            
            # 使用Vosk本地识别（5秒录音）
            text = self.voice_recognizer.recognize_from_mic(
                duration=5, 
                callback=status_callback
            )
            
            if text and text not in ["未检测到语音内容", "语音识别模型未初始化"]:
                self.root.after(0, lambda: self.update_symptom_text(text))
                self.log(f"✓ 本地语音识别成功: {text}")
            else:
                self.root.after(0, lambda: self.voice_status.config(
                    text="X 未检测到语音", fg="#FF0000"))
                self.log("⚠ 未检测到语音内容，请重试")
                
        except Exception as e:
            self.root.after(0, lambda: self.voice_status.config(
                text="X 识别失败", fg="#FF0000"))
            self.log(f"❌ 语音识别失败: {e}")
        finally:
            self.is_recording = False
            self.root.after(0, lambda: self.voice_btn.config(bg="#4A90D9"))
            self.root.after(2000, lambda: self.voice_status.config(
                text="点击语音输入", fg="#666666"))
    
    def update_symptom_text(self, text):
        """更新症状文本"""
        current = self.symptom_text.get("1.0", END).strip()
        # 检查所有可能的占位符
        placeholders = [
            "请输入患者症状描述...", 
            "请输入或语音输入症状...", 
            "输入症状...",
            ""
        ]
        if current in placeholders:
            self.symptom_text.delete("1.0", END)
            self.symptom_text.insert("1.0", text)
        else:
            # 追加到现有内容
            self.symptom_text.insert(END, f"\n{text}")
        
        self.voice_status.config(text="OK 识别完成", fg="#00CC00")
    
    def read_sensor_data(self):
        """从真实传感器读取心率和血氧数据"""
        if not self.heart_sensor:
            messagebox.showwarning("提示", "传感器未初始化，请稍候")
            return
        
        self.sensor_btn.config(state=DISABLED, text="读取中...")
        self.log("正在从传感器读取数据...")
        
        # 在后台线程读取传感器
        threading.Thread(target=self._do_read_sensor, daemon=True).start()
    
    def _do_read_sensor(self):
        """执行传感器读取"""
        try:
            # 读取心率和血氧
            heart_rate, spo2 = self.heart_sensor.read_data()
            
            # 更新UI
            def update_ui():
                self.hr_entry.delete(0, END)
                self.hr_entry.insert(0, str(heart_rate))
                
                self.spo2_entry.delete(0, END)
                self.spo2_entry.insert(0, str(spo2))
                
                self.sensor_btn.config(state=NORMAL, text="[传感器] 读取心率血氧")
                
                mode = "实时" if self.sensor_connected else "未知"
                self.log(f"✓ 传感器数据: 心率={heart_rate}bpm, 血氧={spo2}%")
            
            self.root.after(0, update_ui)
            
        except Exception as e:
            self.root.after(0, lambda: self.sensor_btn.config(
                state=NORMAL, text="[传感器] 读取心率血氧"))
            self.log(f"X 传感器读取失败: {e}")
    
    def validate_input(self):
        """验证输入"""
        errors = []
        
        if not self.name_entry.get().strip():
            errors.append("请输入患者姓名")
        
        age = self.age_entry.get().strip()
        if not age:
            errors.append("请输入年龄")
        elif not age.isdigit() or not (0 <= int(age) <= 150):
            errors.append("年龄必须是0-150之间的数字")
        
        symptom = self.symptom_text.get("1.0", END).strip()
        if not symptom or symptom in ["请输入患者症状描述...", "请输入或语音输入症状..."]:
            errors.append("请输入症状描述")
        
        # 验证生命体征
        try:
            temp = float(self.temp_entry.get() or 0)
            if not (35 <= temp <= 43):
                errors.append("体温应在35-43°C之间")
        except:
            errors.append("体温格式不正确")
        
        try:
            hr = int(self.hr_entry.get() or 0)
            if not (30 <= hr <= 250):
                errors.append("心率应在30-250之间")
        except:
            errors.append("心率格式不正确")
        
        try:
            spo2 = int(self.spo2_entry.get() or 0)
            if not (50 <= spo2 <= 100):
                errors.append("血氧应在50-100%之间")
        except:
            errors.append("血氧格式不正确")
        
        try:
            sbp = int(self.sbp_entry.get() or 0)
            dbp = int(self.dbp_entry.get() or 0)
            if not (60 <= sbp <= 250):
                errors.append("收缩压应在60-250之间")
            if not (40 <= dbp <= 150):
                errors.append("舒张压应在40-150之间")
            if sbp <= dbp:
                errors.append("收缩压应大于舒张压")
        except:
            errors.append("血压格式不正确")
        
        if errors:
            messagebox.showerror("输入错误", "\n".join(errors))
            return False
        return True
    
    def start_triage(self):
        """开始分诊"""
        if not self.validate_input():
            return
        
        self.triage_btn.config(state=DISABLED, text="... 分诊中...")
        self.log("开始执行AI分诊...")
        
        # 在后台线程执行分诊
        threading.Thread(target=self.perform_triage, daemon=True).start()
    
    def perform_triage(self):
        """执行分诊"""
        start_time = time.time()
        
        try:
            # 收集数据
            symptom = self.symptom_text.get("1.0", END).strip()
            vital_signs = {
                "temperature": float(self.temp_entry.get()),
                "heartRate": int(self.hr_entry.get()),
                "bloodOxygen": int(self.spo2_entry.get()),
                "systolicBP": int(self.sbp_entry.get()),
                "diastolicBP": int(self.dbp_entry.get())
            }
            
            # 执行AI分诊
            if self.bert_model:
                triage_level, confidence = self.bert_model.predict(symptom, vital_signs)
                method = "BERT-Tiny"
            else:
                triage_level = self.rule_engine.evaluate(vital_signs)
                confidence = 0.85
                method = "规则引擎"
            
            processing_time = int((time.time() - start_time) * 1000)
            
            # 保存结果用于上传
            self.last_triage_result = {
                "triage_level": triage_level,
                "confidence": confidence,
                "method": method,
                "processing_time": processing_time,
                "vital_signs": vital_signs,
                "symptom": symptom
            }
            
            # 更新UI
            self.root.after(0, lambda: self.update_result(
                triage_level, confidence, method, processing_time))
            
        except Exception as e:
            self.log(f"X 分诊失败: {e}")
            self.root.after(0, lambda: self.triage_btn.config(
                state=NORMAL, text=">> 开始分诊"))
    
    def update_result(self, level, confidence, method, processing_time):
        """更新分诊结果显示"""
        config = self.TRIAGE_CONFIG.get(level, self.TRIAGE_CONFIG[3])
        
        # 更新分诊等级
        self.triage_level_label.config(
            text=f"{level}级 - {config['name']}",
            fg=config['color'],
            bg=config['bg']
        )
        
        # 更新描述
        self.triage_desc_label.config(
            text=f"处理时限: {config['wait']}",
            fg=config['color']
        )
        
        # 更新置信度
        self.confidence_label.config(
            text=f"置信度: {confidence:.1%} | 方法: {method}"
        )
        
        # 更新处理时间
        self.processing_time_label.config(
            text=f"处理耗时: {processing_time}ms"
        )
        
        # 启用按钮
        self.triage_btn.config(state=NORMAL, text=">> 开始分诊")
        self.upload_btn.config(state=NORMAL)
        
        self.log(f"✓ 分诊完成: {level}级({config['name']}), 置信度{confidence:.1%}")
    
    def upload_to_cloud(self):
        """上报数据到云端"""
        if not hasattr(self, 'last_triage_result'):
            messagebox.showwarning("提示", "请先执行分诊")
            return
        
        if not self.mqtt_connected:
            messagebox.showerror("错误", "MQTT未连接，无法上报")
            return
        
        self.upload_btn.config(state=DISABLED, text="... 上报中...")
        threading.Thread(target=self.do_upload, daemon=True).start()
    
    def do_upload(self):
        """执行上传 - 发送符合云端格式的MQTT消息"""
        try:
            self.patient_count += 1
            result = self.last_triage_result
            level = result["triage_level"]
            config_data = self.TRIAGE_CONFIG.get(level, self.TRIAGE_CONFIG[3])
            patient_temp_id = f"TEMP_{datetime.now().strftime('%Y%m%d_%H%M%S')}_{self.patient_count}"
            
            # 构建符合云端MqttMessageHandler.EdgeTriageMessage格式的MQTT消息
            mqtt_message = {
                # 基础信息
                "deviceId": config.DEVICE_ID,
                "patientTempId": patient_temp_id,
                "timestamp": datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                
                # 传感器数据（嵌套结构，对应云端SensorData类）
                "sensorData": {
                    "temperature": result["vital_signs"]["temperature"],
                    "heartRate": result["vital_signs"]["heartRate"],
                    "bloodOxygen": result["vital_signs"]["bloodOxygen"],
                    "systolicBP": result["vital_signs"]["systolicBP"],
                    "diastolicBP": result["vital_signs"]["diastolicBP"],
                    "ambientTemperature": 25.0,
                    "humidity": 50.0
                },
                
                # 语音数据（嵌套结构，对应云端VoiceData类）
                "voiceData": {
                    "text": result["symptom"],
                    "confidence": 0.95,
                    "language": "zh_cn",
                    "duration": 5
                },
                
                # 分诊结果（嵌套结构，对应云端TriageResultData类）
                "triageResult": {
                    "level": level,
                    "priority": config_data["name"],
                    "color": config_data["color"].replace("#", ""),  # 去掉#号
                    "waitTime": config_data["wait"],
                    "confidence": result["confidence"]
                },
                
                # 处理信息
                "processingTime": result["processing_time"],
                "dataQuality": 0.95,
                
                # 额外的患者信息（云端会在createTriageRecordFromEdgeData中处理）
                "patientInfo": {
                    "name": self.name_entry.get().strip(),
                    "age": int(self.age_entry.get()),
                    "gender": self.gender_var.get(),
                    "idCard": self.idcard_entry.get().strip()
                }
            }
            
            # 发送MQTT消息到云端
            message = json.dumps(mqtt_message, ensure_ascii=False)
            self.mqtt_client.publish(config.MQTT_TOPIC_TRIAGE, message)
            
            patient_name = mqtt_message['patientInfo']['name'] or '未命名患者'
            self.log(f"✓ 数据已上报云端: {patient_name}")
            self.root.after(0, lambda name=patient_name: messagebox.showinfo(
                "成功", f"患者 {name} 数据已上报云端"))
            
        except Exception as e:
            error_msg = str(e)
            self.log(f"❌ 上报失败: {error_msg}")
            self.root.after(0, lambda msg=error_msg: messagebox.showerror("错误", f"上报失败: {msg}"))
        
        finally:
            self.root.after(0, lambda: self.upload_btn.config(
                state=NORMAL, text="[云] 上报云端"))
    
    def clear_all(self):
        """清空所有数据"""
        # 清空患者信息
        self.name_entry.delete(0, END)
        self.age_entry.delete(0, END)
        self.gender_var.set("MALE")
        self.idcard_entry.delete(0, END)
        self.symptom_text.delete("1.0", END)
        self.symptom_text.insert("1.0", "请输入或语音输入症状...")
        
        # 清空生命体征
        self.temp_entry.delete(0, END)
        self.hr_entry.delete(0, END)
        self.spo2_entry.delete(0, END)
        self.sbp_entry.delete(0, END)
        self.dbp_entry.delete(0, END)
        
        # 重置分诊结果
        self.triage_level_label.config(text="等待分诊", fg="#999999", bg="white")
        self.triage_desc_label.config(text="请输入患者信息并点击开始分诊", fg="#666666")
        self.confidence_label.config(text="")
        self.processing_time_label.config(text="")
        
        # 禁用上传按钮
        self.upload_btn.config(state=DISABLED)
        
        if hasattr(self, 'last_triage_result'):
            delattr(self, 'last_triage_result')
        
        self.log("已清空所有数据")
    
    def run(self):
        """运行GUI"""
        self.root.mainloop()


def main():
    """主函数"""
    app = EdgeTriageGUI()
    app.run()


if __name__ == "__main__":
    main()
