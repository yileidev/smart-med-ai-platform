#!/bin/bash

# 边缘AI模型部署脚本
# 用于Jetson Orin Nano Super部署边缘分诊模型

set -e

echo "=========================================="
echo "边缘AI医疗分诊模型部署脚本"
echo "适用于: Jetson Orin Nano Super 4GB"
echo "=========================================="

# 检查系统环境
check_environment() {
    echo "🔍 检查系统环境..."
    
    # 检查操作系统
    echo "✅ 操作系统: $(uname -s) $(uname -r)"
    
    # 检查是否是Jetson平台
    if [ -f "/etc/nv_tegra_release" ]; then
        echo "✅ 检测到Jetson平台"
        IS_JETSON=true
    else
        echo "ℹ️ 普通Linux系统（非Jetson）"
        IS_JETSON=false
    fi
    
    # 检查CUDA（可选）
    if command -v nvcc &> /dev/null; then
        cuda_version=$(nvcc --version | grep "release" | awk '{print $6}' | cut -c2-)
        echo "✅ CUDA版本: $cuda_version"
        HAS_CUDA=true
    else
        echo "ℹ️ CUDA未安装，将使用CPU模式"
        HAS_CUDA=false
    fi
    
    # 检查TensorRT（可选）
    if [ "$HAS_CUDA" = true ]; then
        python3 -c "import tensorrt; print('✅ TensorRT版本:', tensorrt.__version__)" 2>/dev/null || {
            echo "ℹ️ TensorRT未安装"
        }
    fi
    
    # 检查Python版本
    python_version=$(python3 --version)
    echo "✅ $python_version"
    
    # 检查可用内存
    memory=$(free -h | grep "Mem:" | awk '{print $2}')
    echo "✅ 可用内存: $memory"
    
    # 检查GPU内存（如果有GPU）
    if [ "$HAS_CUDA" = true ]; then
        gpu_memory=$(nvidia-smi --query-gpu=memory.total --format=csv,noheader,nounits 2>/dev/null || echo "Unknown")
        echo "✅ GPU内存: ${gpu_memory}MB"
    fi
}

# 安装系统依赖
install_system_dependencies() {
    echo "📦 安装系统依赖..."
    
    # 更新包列表
    sudo apt update
    
    # 安装编译工具
    sudo apt install -y \
        python3-pip \
        python3-dev \
        python3-venv \
        build-essential \
        cmake \
        git \
        wget \
        curl
    
    # 安装I2C和GPIO工具
    sudo apt install -y \
        python3-smbus \
        i2c-tools \
        gpio
    
    # 安装音频依赖
    sudo apt install -y \
        portaudio19-dev \
        python3-pyaudio \
        alsa-utils \
        libsndfile1
    
    # 安装MQTT客户端
    sudo apt install -y mosquitto-clients
    
    # 安装中文字体（GUI显示需要）
    echo "📝 安装中文字体..."
    sudo apt install -y \
        fonts-wqy-microhei \
        fonts-wqy-zenhei \
        fonts-noto-cjk \
        fonts-noto-color-emoji
    
    # 更新字体缓存
    sudo fc-cache -fv
    
    echo "✅ 系统依赖安装完成"
}

# 安装Python依赖
install_python_dependencies() {
    echo "🐍 安装Python依赖..."
    
    # 创建虚拟环境
    if [ ! -d ".venv" ]; then
        echo "创建虚拟环境..."
        python3 -m venv .venv
    fi
    
    # 激活虚拟环境
    source .venv/bin/activate
    
    # 升级pip
    python3 -m pip install --upgrade pip
    
    # 安装PyTorch
    echo "安装PyTorch..."
    if [ "$HAS_CUDA" = true ]; then
        # 有CUDA，安装GPU版本
        echo "检测到CUDA，安装GPU版本PyTorch..."
        python3 -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121
    else
        # 无CUDA，安装CPU版本
        echo "未检测到CUDA，安装CPU版本PyTorch..."
        python3 -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu
    fi
    
    # 安装其他依赖
    echo "安装其他依赖..."
    python3 -m pip install -r requirements.txt
    
    # 安装Jetson特有的GPIO库（如果是Jetson平台）
    if [ "$IS_JETSON" = true ]; then
        echo "检测到Jetson平台，安装Jetson.GPIO库..."
        python3 -m pip install Jetson.GPIO
    fi
    
    echo "✅ Python依赖安装完成"
}

# 下载预训练模型
download_pretrained_models() {
    echo "📥 下载预训练模型..."
    
    # 创建模型目录
    mkdir -p models
    cd models
    
    # 下载BERT-base-chinese模型
    if [ ! -d "bert-base-chinese" ]; then
        echo "下载BERT-base-chinese模型..."
        git clone https://huggingface.co/bert-base-chinese
    fi
    
    # 检查是否有预训练的分诊模型
    if [ -f "bert_tiny_triage.pth" ]; then
        echo "✅ 找到预训练分诊模型"
    else
        echo "⚠️ 未找到预训练分诊模型，将在首次运行时训练"
    fi
    
    cd ..
    echo "✅ 模型准备完成"
}

# 训练模型
train_model() {
    echo "🏋️ 开始训练边缘分诊模型..."
    
    # 检查GPU可用性
    python3 -c "import torch; print('GPU可用:', torch.cuda.is_available())"
    
    # 执行训练
    python3 train_model.py
    
    echo "✅ 模型训练完成"
}

# 配置系统服务
setup_systemd_service() {
    echo "⚙️ 配置系统服务..."
    
    # 创建服务文件
    sudo tee /etc/systemd/system/edge-triage.service > /dev/null <<EOF
[Unit]
Description=Edge Medical Triage System
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$PWD
Environment=PYTHONPATH=$PWD
Environment=DISPLAY=:0
ExecStart=$PWD/.venv/bin/python gui_main.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

    # 重载systemd配置
    sudo systemctl daemon-reload
    
    # 启用服务（不立即启动）
    sudo systemctl enable edge-triage.service
    
    echo "✅ 系统服务配置完成"
    echo "   启动服务: sudo systemctl start edge-triage"
    echo "   停止服务: sudo systemctl stop edge-triage"
    echo "   查看状态: sudo systemctl status edge-triage"
    echo "   查看日志: journalctl -u edge-triage -f"
}

# 配置环境变量
setup_environment() {
    echo "🔧 配置环境变量..."
    
    # 创建.env文件
    if [ ! -f ".env" ]; then
        cp .env.example .env
        echo "⚠️ 请编辑 .env 文件配置API密钥和服务器地址"
    fi
    
    # 设置GPIO权限
    sudo usermod -a -G gpio $USER
    echo "✅ GPIO权限已配置，需要重新登录生效"
    
    # 设置I2C权限
    sudo usermod -a -G i2c $USER
    
    echo "✅ 环境变量配置完成"
}

# 性能测试
run_benchmark() {
    echo "📊 运行性能基准测试..."
    
    python3 -c "
from models.bert_tiny_triage import BERTTinyTriage
from models.rule_engine import EdgeRuleEngine
import time

print('测试BERT-Tiny模型...')
bert_model = BERTTinyTriage()
benchmark_results = bert_model.benchmark(100)
print(f'BERT-Tiny性能: {benchmark_results}')

print('\\n测试规则引擎...')
rule_engine = EdgeRuleEngine()
test_vitals = {'temperature': 38.2, 'heartRate': 120, 'systolicBP': 150, 'diastolicBP': 90, 'bloodOxygen': 94}

start_time = time.time()
for _ in range(1000):
    result = rule_engine.evaluate(test_vitals, '胸痛，呼吸困难')
end_time = time.time()

avg_latency = (end_time - start_time) / 1000 * 1000
print(f'规则引擎平均延迟: {avg_latency:.2f}ms')

if benchmark_results['meets_requirement'] and avg_latency < 10:
    print('\\n✅ 性能测试通过，满足毕业设计要求')
else:
    print('\\n⚠️ 性能需要进一步优化')
"
    
    echo "✅ 性能基准测试完成"
}

# 主函数
main() {
    echo "开始部署..."
    
    # 检查是否以root权限运行
    if [ "$EUID" -eq 0 ]; then
        echo "❌ 请不要以root权限运行此脚本"
        exit 1
    fi
    
    # 执行部署步骤
    check_environment
    install_system_dependencies
    install_python_dependencies
    download_pretrained_models
    setup_environment
    
    # 询问是否训练模型
    read -p "是否现在训练模型？(y/N): " train_choice
    if [[ $train_choice =~ ^[Yy]$ ]]; then
        train_model
    fi
    
    # 询问是否配置系统服务
    read -p "是否配置为系统服务？(y/N): " service_choice
    if [[ $service_choice =~ ^[Yy]$ ]]; then
        setup_systemd_service
    fi
    
    # 询问是否运行性能测试
    read -p "是否运行性能测试？(y/N): " benchmark_choice
    if [[ $benchmark_choice =~ ^[Yy]$ ]]; then
        run_benchmark
    fi
    
    echo ""
    echo "=========================================="
    echo "🎉 边缘AI医疗分诊模型部署完成！"
    echo "=========================================="
    echo ""
    echo "下一步："
    echo "1. 编辑 .env 文件配置API密钥和MQTT服务器地址"
    echo "2. 连接传感器硬件"
    echo "3. 运行GUI主程序: source .venv/bin/activate && python gui_main.py"
    echo "4. 或运行命令行版本: source .venv/bin/activate && python main.py"
    echo "5. 或启动系统服务: sudo systemctl start edge-triage"
    echo ""
    echo "故障排查："
    echo "- 检查硬件连接: i2cdetect -y 1"
    echo "- 测试麦克风: arecord -d 5 -f S16_LE test.wav"
    echo "- 查看系统日志: journalctl -u edge-triage -f"
    echo "- 检查字体: fc-list :lang=zh"
    echo ""
}

# 执行主函数
main "$@"