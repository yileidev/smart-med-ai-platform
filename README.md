# 医疗急诊分诊管理系统

> 基于 Spring Boot + Vue 3 + LangChain4j + Jetson边缘计算的端云协同智能分诊系统

## 系统架构

本系统采用**端云协同**架构，由三个核心子系统组成：

- **云端服务**（backend）：Spring Boot 2.7.14 后端，提供AI诊断、业务逻辑、数据持久化
- **前端应用**（frontend）：Vue 3 单页应用，提供多角色工作台界面
- **边缘设备**（edge）：Jetson Orin Nano 端侧程序，负责传感器采集、本地预分诊、数据上报

端云之间通过 MQTT 协议实现实时双向通信。

## 核心特性

- **AI智能分诊**：LangChain4j + 百川大模型（Baichuan2-Turbo-192k），结合 RAG 医疗知识库
- **规则引擎**：Drools 8.44 规则引擎，实现五级分诊（濒危/危急/急症/次急症/非急症）
- **边缘AI推理**：BERT-Tiny 本地预分诊模型 + TensorRT 加速 + 卡尔曼滤波信号处理
- **实时通信**：WebSocket + MQTT（Eclipse Mosquitto），边缘设备与云端实时数据同步
- **离线语音识别**：Vosk 中文离线语音模型，支持边缘端语音录入
- **多角色管理**：医生、护士、管理员三权分立 + JWT + RBAC 权限控制
- **容器化部署**：Docker Compose 一键编排五个服务容器

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端编译运行 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 16+ | 前端构建 |
| Docker | 20.10+ | 容器化部署 |
| Docker Compose | 3.8+ | 服务编排 |
| Python | 3.8+ | 边缘端（Jetson） |

## 快速开始

### Windows 一键启动

```bash
快速启动.bat
```

### 本地开发

#### 后端（默认使用H2内存数据库，无需安装MySQL）
```bash
cd backend
mvn spring-boot:run
```

#### 前端
```bash
cd frontend
npm install
npm run dev
```

#### 边缘端（需要Jetson Orin Nano或Linux环境）
```bash
cd edge
pip install -r requirements.txt
python main.py
```

### 容器化部署

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 填写百川API密钥、数据库密码等

# 2. 构建并启动所有服务
docker-compose up --build -d

# 3. 查看服务状态
docker-compose ps

# 4. 查看后端日志
docker-compose logs -f backend
```

## 项目结构

```
medical_web/
├── backend/                    # Spring Boot 后端服务
│   ├── src/main/java/com/medical/
│   │   ├── config/            # 配置类（Security, Drools, MQTT, AI等）
│   │   ├── controller/        # REST控制器
│   │   ├── entity/            # JPA实体
│   │   ├── repository/        # 数据仓库
│   │   ├── service/           # 业务服务层
│   │   ├── security/          # JWT认证过滤器
│   │   └── dto/               # 数据传输对象
│   ├── src/main/resources/
│   │   ├── rules/             # Drools分诊规则文件(.drl)
│   │   ├── application.yml    # 主配置
│   │   └── data-h2.sql        # H2初始化数据
│   ├── chroma_data/           # Chroma向量数据库本地存储
│   ├── Dockerfile             # 多阶段安全构建
│   └── pom.xml
├── frontend/                   # Vue 3 前端应用
│   ├── src/
│   │   ├── views/             # 页面组件（医生/护士/管理员工作台）
│   │   ├── components/        # 公共组件（设备监控、体征表单）
│   │   ├── api/               # 接口封装
│   │   ├── stores/            # Pinia状态管理
│   │   ├── router/            # 路由配置
│   │   └── composables/       # WebSocket组合式函数
│   ├── Dockerfile             # Nginx多阶段构建
│   └── package.json
├── edge/                       # Jetson Orin Nano 边缘端
│   ├── sensors/               # 传感器驱动（DS18B20/MAX30102/USB麦克风）
│   ├── preprocessing/         # 信号预处理（卡尔曼滤波/语音识别/OCR）
│   ├── models/                # AI模型（BERT-Tiny分诊/规则引擎）
│   ├── mqtt_client/           # MQTT数据上报
│   ├── config.py              # 边缘端配置
│   ├── main.py                # 主程序入口
│   └── requirements.txt       # Python依赖
├── deploy/                     # 部署脚本（阿里云/云端）
├── docker-compose.yml          # 容器编排（MySQL+Redis+MQTT+Backend+Frontend）
├── init.sql                    # 数据库初始化脚本
├── .env.example                # 环境变量模板
└── 快速启动.bat                # Windows一键启动
```

## 默认账号

| 角色 | 用户名 | 密码 | 权限 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 系统管理、用户管理、数据统计 |
| 医生 | doctor | doctor123 | 患者诊断、治疗方案、处方管理 |
| 护士 | nurse | nurse123 | 分诊管理、患者登记、体征录入 |

## 技术栈

### 后端（Java 17）

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 2.7.14 |
| 安全认证 | Spring Security + JWT (jjwt) | 0.11.5 |
| 数据持久化 | Spring Data JPA + MySQL | 8.0.33 |
| 缓存 | Spring Data Redis + Lettuce | 7 |
| AI框架 | LangChain4j + 百川智能 | 0.24.0 |
| 向量数据库 | ChromaDB (all-minilm-l6-v2嵌入) | - |
| 规则引擎 | Drools | 8.44.0 |
| 实时通信 | Spring WebSocket + MQTT (Eclipse Paho) | 1.2.5 |
| 文档导出 | Apache POI + iText7 | 5.2.3 / 7.2.5 |
| API文档 | SpringDoc OpenAPI | 1.7.0 |
| 监控 | Spring Boot Actuator | - |

### 前端

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 + Composition API | 3.3.4 |
| 构建工具 | Vite | 4.4.5 |
| UI组件库 | Element Plus | 2.3.9 |
| 状态管理 | Pinia | 2.1.6 |
| 路由 | Vue Router | 4.2.4 |
| 数据可视化 | ECharts + vue-echarts | 5.4.3 |
| 实时通信 | STOMP.js + SockJS | 7.2.1 |
| HTTP客户端 | Axios | 1.5.0 |

### 边缘端（Jetson Orin Nano）

| 类别 | 技术 | 说明 |
|------|------|------|
| 运行环境 | Python 3.x + PyTorch (CUDA) | GPU加速推理 |
| 预分诊模型 | BERT-Tiny + TensorRT (fp16) | 本地轻量级推理 |
| 语音识别 | Vosk (vosk-model-small-cn-0.22) | 完全离线中文识别 |
| 信号处理 | 卡尔曼滤波 (SciPy/NumPy) | 传感器噪声过滤 |
| 传感器 | DS18B20体温 + MAX30102心率血氧 | I2C/1-Wire驱动 |
| 通信 | paho-mqtt | MQTT数据上报 |
| 本地缓存 | SQLite | 断网数据暂存 |

### 部署

| 类别 | 技术 | 说明 |
|------|------|------|
| 容器化 | Docker + Docker Compose 3.8 | 五容器编排 |
| 反向代理 | Nginx (alpine) | 前端静态资源+API代理 |
| 消息代理 | Eclipse Mosquitto 2 | MQTT Broker |
| 数据库 | MySQL 8.0 | 主数据库 |
| 缓存 | Redis 7-alpine | 会话和数据缓存 |

## 核心功能

### 1. AI智能分诊
- 百川大模型（Baichuan2-Turbo-192k）病情智能评估
- RAG增强：ChromaDB医疗知识库检索增强生成
- Drools规则引擎五级分诊（濒危/危急/急症/次急症/非急症）
- 护士复核确认机制

### 2. 边缘设备采集
- DS18B20体温传感器 + 卡尔曼滤波降噪
- MAX30102心率血氧传感器
- USB麦克风 + Vosk离线语音识别
- BERT-Tiny本地预分诊（TensorRT加速）
- 断网SQLite缓存 + 恢复后批量上传

### 3. 实时通信
- WebSocket推送分诊状态变更
- MQTT端云双向通信（设备心跳/分诊数据/最终结果）
- 边缘设备在线状态监控

### 4. 多角色工作台
- 护士：患者登记、体征录入、分诊确认
- 医生：患者诊断、治疗方案、处方管理
- 管理员：用户管理、数据统计、系统配置

## 开发指南

### 后端开发
```bash
cd backend

# 编译项目
mvn clean compile

# 本地运行（H2数据库模式）
mvn spring-boot:run

# 使用MySQL运行
mvn spring-boot:run -Dspring.profiles.active=mysql

# 打包
mvn clean package -DskipTests
```

### 前端开发
```bash
cd frontend

# 安装依赖
npm install

# 开发模式（自动代理到localhost:8080）
npm run dev

# 构建生产版本
npm run build
```

### 边缘端开发
```bash
cd edge

# 安装依赖
pip install -r requirements.txt

# 训练BERT-Tiny模型
python train_model.py

# 运行主程序
python main.py

# GUI模式
python gui_main.py
```

## 容器服务说明

| 服务 | 容器名 | 端口 | 镜像 |
|------|--------|------|------|
| MySQL | medical_web_mysql | 3306 | mysql:8.0 |
| Redis | medical_web_redis | 6379 | redis:7-alpine |
| MQTT | medical_web_mqtt | 1883 | eclipse-mosquitto:2 |
| 后端 | medical_web_backend | 8080 | openjdk:17-jre-slim |
| 前端 | medical_web_frontend | 80 | nginx:alpine |

## 环境变量配置

复制 `.env.example` 为 `.env`，关键配置项：

```bash
# 数据库
MYSQL_ROOT_PASSWORD=your_password
MYSQL_PASSWORD=your_password

# JWT密钥（至少256位）
JWT_SECRET=your_jwt_secret_key

# 百川AI API密钥
BAICHUAN_API_KEY=your_api_key

# 向量数据库
CHROMA_URL=http://localhost:8000
CHROMA_COLLECTION=medical-knowledge
```

## 服务访问

| 服务 | 地址 |
|------|------|
| 前端应用 | http://localhost |
| 后端API | http://localhost:8080/api |
| API文档 | http://localhost:8080/api/swagger-ui.html |
| 健康检查 | http://localhost:8080/api/actuator/health |
| MQTT Broker | tcp://localhost:1883 |

## 故障排除

| 问题 | 解决方案 |
|------|----------|
| 端口冲突 | 修改 docker-compose.yml 中对应的端口映射 |
| 后端启动失败 | 检查 JDK 17 是否安装，确认数据库连接配置 |
| AI服务不可用 | 检查 BAICHUAN_API_KEY 是否正确配置 |
| 边缘设备连接失败 | 确认 MQTT Broker 地址和端口，检查网络连通性 |
| 前端代理404 | 确认后端已启动在8080端口 |

### 日志查看
```bash
# 后端日志
docker-compose logs -f backend

# 所有服务日志
docker-compose logs -f

# 本地后端日志文件
logs/medical-web.log
```