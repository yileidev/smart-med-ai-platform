#!/bin/bash

# ================================================
# 医疗急诊分诊系统 - 阿里云一键部署脚本
# ================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================"
echo "  医疗急诊分诊系统 - 阿里云部署"
echo -e "========================================${NC}"

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用 root 用户运行此脚本${NC}"
    echo "使用: sudo bash aliyun-deploy.sh"
    exit 1
fi

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

echo -e "${YELLOW}[1/8] 检查系统环境...${NC}"

# 检查系统
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$NAME
    echo "操作系统: $OS"
else
    echo -e "${RED}无法识别操作系统${NC}"
    exit 1
fi

# 安装Docker（如果未安装）
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}[2/8] 安装 Docker...${NC}"
    
    # 阿里云镜像安装Docker
    curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
    
    # 启动Docker服务
    systemctl start docker
    systemctl enable docker
    
    echo -e "${GREEN}Docker 安装完成${NC}"
else
    echo -e "${GREEN}[2/8] Docker 已安装: $(docker --version)${NC}"
fi

# 安装Docker Compose（如果未安装）
if ! command -v docker-compose &> /dev/null; then
    echo -e "${YELLOW}[3/8] 安装 Docker Compose...${NC}"
    
    # 使用国内镜像下载
    curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
    
    echo -e "${GREEN}Docker Compose 安装完成${NC}"
else
    echo -e "${GREEN}[3/8] Docker Compose 已安装: $(docker-compose --version)${NC}"
fi

# 配置Docker镜像加速（阿里云）
echo -e "${YELLOW}[4/8] 配置 Docker 镜像加速...${NC}"
mkdir -p /etc/docker
cat > /etc/docker/daemon.json << EOF
{
    "registry-mirrors": [
        "https://mirror.ccs.tencentyun.com",
        "https://registry.docker-cn.com",
        "https://docker.mirrors.ustc.edu.cn"
    ],
    "log-driver": "json-file",
    "log-opts": {
        "max-size": "100m",
        "max-file": "3"
    }
}
EOF
systemctl daemon-reload
systemctl restart docker
echo -e "${GREEN}镜像加速配置完成${NC}"

# 检查并配置环境变量
echo -e "${YELLOW}[5/8] 配置环境变量...${NC}"
if [ ! -f .env ]; then
    if [ -f .env.aliyun ]; then
        cp .env.aliyun .env
        echo -e "${YELLOW}已从 .env.aliyun 创建 .env 文件${NC}"
        echo -e "${RED}请编辑 .env 文件，修改所有 CHANGE_THIS 开头的配置项！${NC}"
        echo "编辑命令: nano .env 或 vim .env"
        read -p "配置完成后按 Enter 继续..." 
    else
        echo -e "${RED}找不到 .env.aliyun 配置文件${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}.env 文件已存在${NC}"
fi

# 创建必要的目录
echo -e "${YELLOW}[6/8] 创建必要目录...${NC}"
mkdir -p mysql logs

# 创建MySQL配置文件
mkdir -p mysql
cat > mysql/my.cnf << EOF
[mysqld]
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-time-zone='+8:00'
max_connections=500
max_allowed_packet=64M
innodb_buffer_pool_size=256M

[client]
default-character-set=utf8mb4
EOF
echo -e "${GREEN}MySQL配置创建完成${NC}"

# 配置防火墙
echo -e "${YELLOW}[7/8] 配置防火墙规则...${NC}"
if command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=80/tcp 2>/dev/null || true
    firewall-cmd --permanent --add-port=8080/tcp 2>/dev/null || true
    firewall-cmd --permanent --add-port=1883/tcp 2>/dev/null || true
    firewall-cmd --reload 2>/dev/null || true
    echo -e "${GREEN}防火墙规则已添加${NC}"
elif command -v ufw &> /dev/null; then
    ufw allow 80/tcp
    ufw allow 8080/tcp
    ufw allow 1883/tcp
    echo -e "${GREEN}UFW防火墙规则已添加${NC}"
else
    echo -e "${YELLOW}未检测到防火墙，请手动配置阿里云安全组${NC}"
fi

echo -e "${YELLOW}请确保阿里云安全组已开放以下端口:${NC}"
echo "  - 80   (前端HTTP)"
echo "  - 8080 (后端API)"
echo "  - 1883 (MQTT，如需边缘设备连接)"

# 部署服务
echo -e "${YELLOW}[8/8] 启动服务...${NC}"

# 停止现有容器
echo "停止现有容器..."
docker-compose -f docker-compose.prod.yml down 2>/dev/null || true

# 清理旧镜像
echo "清理旧镜像..."
docker system prune -f

# 拉取基础镜像
echo "拉取基础镜像..."
docker pull mysql:8.0
docker pull redis:7-alpine
docker pull node:16-alpine
docker pull nginx:alpine
docker pull maven:3.9.4-openjdk-17-slim
docker pull openjdk:17-jre-slim

# 构建并启动
echo "构建并启动服务（首次可能需要10-15分钟）..."
docker-compose -f docker-compose.prod.yml up --build -d

# 等待服务启动
echo "等待服务启动..."
sleep 30

# 检查服务状态
echo -e "${GREEN}========================================"
echo "部署完成！检查服务状态:"
echo -e "========================================${NC}"
docker-compose -f docker-compose.prod.yml ps

# 获取公网IP
PUBLIC_IP=$(curl -s http://checkip.amazonaws.com 2>/dev/null || curl -s http://ipinfo.io/ip 2>/dev/null || echo "YOUR_SERVER_IP")

echo ""
echo -e "${GREEN}========================================"
echo "  访问地址"
echo -e "========================================${NC}"
echo -e "前端界面: ${GREEN}http://${PUBLIC_IP}${NC}"
echo -e "后端API:  ${GREEN}http://${PUBLIC_IP}:8080${NC}"
echo ""
echo -e "${YELLOW}默认账号:${NC}"
echo "  管理员: admin / admin123"
echo "  医生:   doctor / doctor123"
echo "  护士:   nurse / nurse123"
echo ""
echo -e "${YELLOW}常用命令:${NC}"
echo "  查看日志:   docker-compose -f docker-compose.prod.yml logs -f"
echo "  查看后端:   docker-compose -f docker-compose.prod.yml logs -f backend"
echo "  重启服务:   docker-compose -f docker-compose.prod.yml restart"
echo "  停止服务:   docker-compose -f docker-compose.prod.yml down"
echo ""
echo -e "${RED}重要提醒:${NC}"
echo "1. 请在阿里云控制台安全组中开放 80、8080、1883 端口"
echo "2. 建议配置HTTPS证书提升安全性"
echo "3. 生产环境请修改默认密码"
echo -e "${GREEN}========================================${NC}"
