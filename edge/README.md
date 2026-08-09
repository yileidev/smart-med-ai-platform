# 边缘端代码 - Jetson Orin Nano Super

## 📝 支持平台

| 平台 | 说明 | GUI支持 | 传感器 |
|------|------|---------|--------|
| **Jetson Orin Nano** | 生产环境，完整功能 | ✅ | ✅ 真实硬件 |
| **Ubuntu 20.04+** | 测试环境 | ✅ | ⚠️ 需要硬件 |
| **Windows 10/11** | 开发测试 | ✅ | 💻 模拟数据 |

## 🚀 快速开始

### Jetson/Linux 部署
```bash
# 1. 进入项目目录
cd edge/

# 2. 执行部署脚本（自动安装所有依赖）
chmod +x deploy.sh
./deploy.sh

# 3. 激活虚拟环境并运行
source .venv/bin/activate
python gui_main.py
```

### Windows 开发测试
```powershell
# 1. 创建虚拟环境
python -m venv .venv
.venv\Scripts\activate

# 2. 安装依赖
pip install --index-url https://download.pytorch.org/whl/cpu torch
pip install -r requirements.txt

# 3. 运行GUI界面
python gui_main.py
```

> ⚠️ Windows下传感器使用模拟数据，适合开发调试

## 📁 目录结构

```
edge/
├── sensors/                    # 传感器驱动模块
│   ├── ds18b20_driver.py      # DS18B20体温传感器
│   ├── max30102_driver.py     # MAX30102心率血氧传感器
│   └── usb_mic_driver.py      # USB麦克风语音采集
├── preprocessing/              # 数据预处理模块
│   ├── kalman_filter.py       # 卡尔曼滤波器（传感器去噪）
│   └── local_voice_recognizer.py  # Vosk本地语音识别（完全离线）
├── models/                     # AI模型模块
│   ├── bert_tiny_triage.py    # BERT-Tiny分诊模型
│   └── rule_engine.py         # 边缘规则引擎
├── mqtt_client/               # MQTT通信模块
│   └── mqtt_publisher.py      # MQTT消息发布
├── gui_main.py                # 🖥️ GUI主程序（推荐）
├── main.py                    # 命令行主程序
├── config.py                  # 配置文件
├── deploy.sh                  # 🚀 Linux部署脚本
└── requirements.txt           # Python依赖
```

## 🔧 硬件配置

### Jetson Orin Nano Super 4GB
- GPU: NVIDIA Ampere架构，1024核CUDA核心
- CPU: 6核 Arm Cortex-A78AE
- 内存: 4GB LPDDR5
- 存储: MicroSD卡（建议128GB以上）
- 系统: JetPack 6.0

### 传感器接口
| 传感器 | 接口 | 用途 |
|--------|------|------|
| DS18B20 | GPIO4 (1-Wire) | 体温测量 |
| MAX30102 | I2C (GPIO3/5) | 心率/血氧 |
| USB麦克风 | USB 3.0 | 语音输入 |

---

## 📦 依赖安装

### 方法一：使用部署脚本（推荐）
```bash
chmod +x deploy.sh
./deploy.sh
```
部署脚本会自动：
- 创建 Python 虚拟环境 (.venv)
- 安装系统依赖（音频库、I2C工具等）
- 安装中文字体（GUI显示需要）
- 安装 PyTorch（根据是否有CUDA自动选择版本）
- 安装所有 Python 依赖
- 配置系统服务（可选）

### 方法二：手动安装

#### 1. 系统依赖 (Linux)
```bash
# 更新系统
sudo apt update

# 安装开发工具
sudo apt install -y python3-pip python3-dev python3-venv git cmake

# 安装I2C和GPIO工具
sudo apt install -y python3-smbus i2c-tools

# 安装音频处理库
sudo apt install -y portaudio19-dev python3-pyaudio alsa-utils

# 安装中文字体（GUI显示需要）
sudo apt install -y fonts-wqy-microhei fonts-noto-cjk fonts-noto-color-emoji
sudo fc-cache -fv
```

#### 2. Python依赖
```bash
# 创建虚拟环境
python3 -m venv .venv
source .venv/bin/activate

# 安装PyTorch
# Jetson/CUDA版本:
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121

# 或 CPU版本:
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu

# 安装其他依赖
pip install -r requirements.txt
```

#### 3. TensorRT安装（已包含在JetPack中）
```bash
# 验证TensorRT安装
python3 -c "import tensorrt; print(tensorrt.__version__)"
```

---

## 🚀 运行程序

### GUI模式（推荐）
```bash
source .venv/bin/activate
python gui_main.py
```

### 命令行模式
```bash
source .venv/bin/activate
python main.py
```

### 系统服务模式
```bash
# 启动服务
sudo systemctl start edge-triage

# 停止服务
sudo systemctl stop edge-triage

# 查看状态
sudo systemctl status edge-triage

# 查看日志
journalctl -u edge-triage -f
```

### 配置参数
编辑 `.env` 文件（从config.py读取）：
```bash
# MQTT配置
MQTT_BROKER=云端服务器IP  # 例如: 47.xxx.xxx.xxx
MQTT_PORT=1883
```

---

## 📊 数据流程

```
传感器采集 → 卡尔曼滤波 → BERT-Tiny分诊 → MQTT发送云端
    ↓
DS18B20(温度)
MAX30102(心率/血氧)
USB麦克风(语音)
```

---

## 🧪 测试说明

### 传感器测试
```bash
# 测试DS18B20
python3 sensors/ds18b20_driver.py

# 测试MAX30102
python3 sensors/max30102_driver.py

# 测试麦克风
python3 sensors/usb_mic_driver.py
```

### 模型测试
```bash
# 测试BERT-Tiny分诊模型
python3 models/bert_tiny_triage.py
```

---

## ⚙️ 性能优化

### TensorRT模型加速
```python
# BERT-Tiny转TensorRT
python3 convert_to_tensorrt.py \
  --model bert-tiny \
  --precision fp16 \
  --output bert_tiny.trt
```

### 预期性能指标
- 分诊推理延迟: < 100ms
- MQTT消息延迟: < 50ms
- 总体响应时间: < 200ms

---

## 🔍 故障排查

### 常见问题

**1. GUI显示中文乱码**
```bash
# 安装中文字体
sudo apt install -y fonts-wqy-microhei fonts-noto-cjk
sudo fc-cache -fv

# 检查字体是否安装
fc-list :lang=zh
```

**2. I2C设备未检测到**
```bash
# 检查I2C设备
sudo i2cdetect -y -r 1

# 启用I2C
sudo nano /boot/firmware/config.txt
# 添加: dtparam=i2c_arm=on
```

**3. GPIO权限错误**
```bash
# 添加用户到gpio组
sudo usermod -a -G gpio $USER
# 重新登录生效
```

**4. TensorRT导入失败**
```bash
# 检查CUDA版本
nvcc --version

# 重新安装PyTorch
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121
```

**5. 麦克风无法录音**
```bash
# 测试麦克风
arecord -d 5 -f S16_LE test.wav
aplay test.wav

# 检查音频设备
arecord -l
```

**6. Vosk模型下载失败**
```bash
# 手动下载模型到 preprocessing/models/vosk/ 目录
wget https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip
unzip vosk-model-small-cn-0.22.zip -d preprocessing/models/vosk/
```

---

## 📝 开发计划

- [x] 项目框架搭建
- [x] DS18B20驱动开发
- [x] MAX30102驱动开发
- [x] USB麦克风集成
- [x] 卡尔曼滤波器实现
- [x] Vosk本地语音识别（完全离线）
- [x] BERT-Tiny模型部署
- [x] GUI界面开发
- [x] 跨平台兼容 (Windows/Linux/Jetson)
- [ ] TensorRT模型加速
- [x] MQTT通信测试
- [x] 边缘-云端联调

---

**开发环境**: PyCharm + Python 3.8+  
**生产环境**: JetPack 6.0 + CUDA 12.2  
**目标性能**: 分诊延迟 < 200ms, 准确率 > 95%
