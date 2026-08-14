# 智能医疗急诊分诊管理平台

> 基于 Spring Boot、Vue 3、LangChain4j、Drools、MQTT 与 Jetson 边缘计算的端云协同智能急诊分诊系统。

本项目面向急诊分诊、患者接诊、医生诊断、医疗资源调度与边缘设备采集场景，提供从端侧生命体征采集、边缘预分诊、云端 AI 辅助诊断、护士复核、医生接诊到管理员监控的完整业务闭环。

## 项目定位

智能医疗急诊分诊管理平台用于提升急诊分诊效率和诊疗协同能力，主要解决以下问题：

- 通过边缘设备采集体温、心率、血氧、语音等多模态数据，降低人工录入成本。
- 通过边缘规则模型与云端 AI 模型结合，实现急诊分诊等级辅助判断。
- 通过 Drools 规则引擎固化医疗分诊规则、医生分配规则与资源调度规则。
- 通过护士复核机制保证 AI 分诊结果可追溯、可修正、可审核。
- 通过医生工作台提供待诊队列、AI 诊断建议、诊断记录保存与会诊申请能力。
- 通过管理员后台提供用户管理、资源管理、规则管理、系统监控、日志审计与统计分析能力。

## 系统架构

项目采用端云协同架构，由三个核心子系统组成：

```text
Jetson 边缘端 / Windows 模拟端
  ├─ 传感器采集：DS18B20、MAX30102、USB 麦克风
  ├─ 数据预处理：卡尔曼滤波、离线语音识别、OCR 扩展
  ├─ 本地推理：BERT-Tiny 分诊模型、规则引擎
  └─ MQTT 上报：分诊数据、设备心跳、设备状态
          │
          ▼
云端后端服务 Spring Boot
  ├─ 认证授权：JWT、Spring Security、RBAC
  ├─ 分诊业务：患者登记、分诊确认、医生队列、诊断提交
  ├─ AI 能力：百川大模型、LangChain4j、RAG 诊断、向量检索
  ├─ 规则引擎：Drools 分诊优先级、医生分配、资源调度
  ├─ 数据持久化：Spring Data JPA、H2 / MySQL
  ├─ 实时消息：WebSocket、STOMP、MQTT
  └─ 管理后台：用户、资源、日志、监控、统计、规则管理
          │
          ▼
Vue 3 前端应用
  ├─ 登录认证
  ├─ 护士工作台
  ├─ 医生工作台
  └─ 管理员后台
```

## 核心能力

### 急诊智能分诊

- 支持边缘设备上报分诊数据。
- 支持护士录入患者信息、生命体征与主诉信息。
- 支持五级分诊等级判断。
- 支持 AI 辅助分诊、规则分诊、护士确认与批量确认。
- 支持分诊队列、待确认列表、分诊统计与分诊状态更新。

### 医生诊断工作台

- 查看待诊患者队列。
- 开始诊断并维护诊断状态。
- 获取 AI 辅助诊断与深度分析建议。
- 保存诊断结论、治疗方案、处方与随访建议。
- 查看个人患者列表并发起会诊请求。

### 护士工作台

- 患者登记与患者搜索。
- 生命体征录入与更新。
- 分诊创建、修改、确认与医生分配。
- 急救呼叫与重新评估申请。
- 查看可用医生与分诊队列。

### 管理员后台

- 系统概览与诊断统计。
- 用户管理、用户状态维护。
- 医疗资源管理。
- 系统配置、系统日志、实时监控。
- Drools 规则文件查看、编辑、测试与热重载。
- AI 健康状态、边缘设备状态与技术栈状态检查。
- 已确诊患者查询、详情与统计。

### AI 与知识库能力

- 通过 LangChain4j 接入百川智能模型。
- 支持智能分诊、深度诊断、科室推荐、设备推荐。
- 支持 RAG 医疗知识库增强诊断。
- 支持向量检索、知识库初始化、知识条目添加与知识统计。
- 支持 HL7 消息生成与解析，便于医疗系统集成。

### 边缘计算能力

- 支持 Jetson Orin Nano、Linux 与 Windows 开发测试。
- Linux / Jetson 环境可接入真实传感器；Windows 环境使用模拟数据。
- 支持 DS18B20 体温传感器、MAX30102 心率血氧传感器、USB 麦克风。
- 支持 Vosk 中文离线语音识别。
- 支持 BERT-Tiny 本地预分诊模型。
- 支持 SQLite 本地缓存、断网暂存与批量上传配置。
- 支持 MQTT 心跳和分诊数据上报。

## 技术栈

### 后端

| 类型 | 技术 | 版本 / 说明 |
| --- | --- | --- |
| 基础框架 | Spring Boot | 2.7.14 |
| 语言 | Java | 17 |
| Web | Spring MVC | REST API |
| 安全 | Spring Security | JWT 无状态认证、方法级权限控制 |
| 认证令牌 | jjwt | 0.11.5 |
| ORM | Spring Data JPA / Hibernate | 支持 H2 与 MySQL |
| 数据库 | H2、MySQL | H2 默认开发模式；MySQL 用于持久化部署 |
| 缓存 | Spring Cache、Redis | 可通过配置启用 Redis 缓存 |
| 实时通信 | Spring WebSocket、STOMP、SockJS | 前后端实时消息推送 |
| MQTT | Eclipse Paho | 云端与边缘端消息通信 |
| AI 编排 | LangChain4j | 0.24.0 |
| 大模型 | 百川智能 | 默认 Baichuan2-Turbo-192k |
| 向量检索 | ChromaDB、AllMiniLmL6V2 Embedding | 384 维向量，可选启用 |
| 规则引擎 | Drools | 8.44.0.Final |
| API 文档 | SpringDoc OpenAPI | 1.7.0 |
| 导出 | Apache POI、iText 7 | Excel / PDF 导出 |
| 监控 | Spring Boot Actuator | 健康检查 |
| 构建 | Maven | maven-compiler-plugin + spring-boot-maven-plugin |

### 前端

| 类型 | 技术 | 版本 / 说明 |
| --- | --- | --- |
| 框架 | Vue 3 | 3.3.4 |
| 构建工具 | Vite | 4.4.5 |
| UI 组件 | Element Plus | 2.3.9 |
| 路由 | Vue Router | 4.2.4 |
| 状态管理 | Pinia | 2.1.6 |
| HTTP 客户端 | Axios | 1.5.0 |
| 实时通信 | STOMP.js、SockJS Client | WebSocket 消息订阅 |
| 图表 | ECharts、vue-echarts | 管理端统计与监控展示 |
| 样式 | Sass | 全局样式与组件样式 |

### 边缘端

| 类型 | 技术 | 说明 |
| --- | --- | --- |
| 运行环境 | Python 3.8+ | 支持 Jetson / Linux / Windows |
| 模型推理 | PyTorch、Transformers | BERT-Tiny 分诊模型 |
| 语音识别 | Vosk | 中文离线语音识别 |
| 数据处理 | NumPy、SciPy、Pandas | 传感器数据处理与分析 |
| 信号处理 | 卡尔曼滤波 | 降低体征采集噪声 |
| MQTT | paho-mqtt | 分诊数据和心跳上报 |
| 传感器 | smbus2、w1thermsensor | Linux / Jetson 硬件接口 |
| GUI | Python GUI 程序 | `gui_main.py` 推荐入口 |

## 项目结构

```text
smart-med-ai-platform/
├── backend/                                  # Spring Boot 后端服务
│   ├── Dockerfile                            # 后端多阶段容器构建文件
│   ├── pom.xml                               # Maven 项目配置
│   ├── init_chroma_db.py                     # Chroma 知识库初始化脚本
│   ├── expand_knowledge.py                   # 医疗知识库扩展脚本
│   ├── chroma_data/                          # 本地 Chroma 数据目录
│   └── src/main/
│       ├── java/com/medical/
│       │   ├── MedicalWebApplication.java    # 后端启动入口
│       │   ├── config/                       # 安全、Web、Redis、MQTT、Drools、AI 配置
│       │   ├── controller/                   # REST API 控制器
│       │   ├── dto/                          # 请求响应 DTO
│       │   ├── entity/                       # JPA 实体模型
│       │   ├── enums/                        # 业务枚举
│       │   ├── exception/                    # 自定义异常
│       │   ├── repository/                   # 数据访问层
│       │   ├── security/                     # JWT 认证过滤器与工具
│       │   └── service/                      # 核心业务服务
│       └── resources/
│           ├── application.yml               # 公共配置
│           ├── application-h2.yml            # H2 开发配置
│           ├── application-mysql.yml         # MySQL 配置
│           ├── application-docker.yml        # Docker 环境配置
│           ├── application-prod.yml          # 生产环境配置
│           ├── application-secure.yml        # 安全增强配置
│           ├── data-h2.sql                   # H2 初始化数据
│           ├── db/init-mysql.sql             # MySQL 初始化脚本
│           └── rules/                        # Drools 规则文件
├── frontend/                                 # Vue 3 前端应用
│   ├── Dockerfile                            # 前端容器构建文件
│   ├── nginx.conf                            # Nginx 静态资源与代理配置
│   ├── package.json                          # 前端依赖与脚本
│   ├── vite.config.js                        # Vite 配置与本地代理
│   └── src/
│       ├── api/                              # 按角色拆分的接口封装
│       ├── components/                       # 通用组件
│       ├── composables/                      # WebSocket 等组合式逻辑
│       ├── layout/                           # 管理端布局
│       ├── router/                           # 路由与权限守卫
│       ├── stores/                           # Pinia 状态管理
│       ├── utils/                            # 请求、WebSocket 工具
│       └── views/                            # 登录、医生、护士、管理员页面
├── edge/                                     # Jetson / Linux / Windows 边缘端程序
│   ├── README.md                             # 边缘端专项说明
│   ├── config.py                             # 边缘端配置
│   ├── main.py                               # 命令行入口
│   ├── gui_main.py                           # GUI 入口
│   ├── deploy.sh                             # Linux / Jetson 部署脚本
│   ├── requirements.txt                      # Python 依赖
│   ├── sensors/                              # 传感器驱动
│   ├── preprocessing/                        # 预处理、语音识别、OCR
│   ├── models/                               # BERT-Tiny 与规则引擎
│   └── mqtt_client/                          # MQTT 发布模块
├── docker-compose.yml                        # MySQL、Redis、MQTT、后端、前端编排
├── init.sql                                  # Docker MySQL 初始化脚本
├── mosquitto.conf                            # MQTT Broker 配置
└── README.md                                 # 项目总览文档
```

## 环境要求

| 组件 | 建议版本 | 用途 |
| --- | --- | --- |
| JDK | 17+ | 后端编译和运行 |
| Maven | 3.8+ | 后端依赖管理与构建 |
| Node.js | 16+ | 前端依赖安装与构建 |
| npm | 8+ | 前端包管理 |
| Python | 3.8+ | 边缘端程序运行 |
| Docker | 20.10+ | 容器化部署 |
| Docker Compose | v2 或兼容 3.8 配置 | 服务编排 |
| MySQL | 8.0 | 持久化数据库，可选 |
| Redis | 7 | 缓存，可选 |
| MQTT Broker | Eclipse Mosquitto 2 | 端云通信，可选 |

## 快速开始

### 本地开发模式

#### 启动后端

默认使用 H2 内存数据库，不依赖本地 MySQL。

```bash
cd backend
mvn spring-boot:run
```

后端地址：

```text
http://localhost:8080/api
```

H2 控制台：

```text
http://localhost:8080/api/h2-console
```

H2 默认连接信息：

```text
JDBC URL: jdbc:h2:mem:testdb
User Name: sa
Password: 空
```

#### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发地址：

```text
http://localhost:5173
```

Vite 已将 `/api` 请求代理到 `http://localhost:8080`。

#### 启动边缘端

Windows 开发环境可使用模拟传感器数据；Jetson / Linux 环境可接入真实传感器。

```bash
cd edge
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python gui_main.py
```

Windows PowerShell：

```powershell
cd edge
python -m venv .venv
.venv\Scripts\activate
pip install --index-url https://download.pytorch.org/whl/cpu torch
pip install -r requirements.txt
python gui_main.py
```

### MySQL 本地运行

先准备 MySQL 8.0 数据库，并执行根目录 `init.sql` 或后端资源目录中的初始化脚本。

```bash
cd backend
mvn spring-boot:run -Dspring.profiles.active=mysql
```

可通过环境变量覆盖数据库连接：

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=medical_web
export DB_USERNAME=root
export DB_PASSWORD=your_password
mvn spring-boot:run -Dspring.profiles.active=mysql
```

### Docker Compose 部署

根目录 `docker-compose.yml` 编排 MySQL、Redis、Mosquitto、后端与前端服务。

根目录当前未提供 `.env.example`，首次运行前请手动创建 `.env` 文件：

```bash
MYSQL_ROOT_PASSWORD=change_me_root_password
MYSQL_PASSWORD=change_me_mysql_password
JWT_SECRET=change_me_to_a_long_random_secret_at_least_32_bytes
BAICHUAN_API_KEY=your_baichuan_api_key
```

启动服务：

```bash
docker compose up --build -d
```

查看服务状态：

```bash
docker compose ps
```

查看后端日志：

```bash
docker compose logs -f backend
```

停止服务：

```bash
docker compose down
```

## 配置说明

### 后端核心配置

后端主配置位于 `backend/src/main/resources/application.yml`。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | 后端监听端口 |
| `server.servlet.context-path` | `/api` | API 统一上下文路径 |
| `SPRING_PROFILES_ACTIVE` | `h2` | 默认启用 H2 开发模式 |
| `JWT_SECRET` | 内置开发密钥 | JWT 签名密钥，生产环境必须覆盖 |
| `JWT_EXPIRATION` | `86400000` | JWT 过期时间，默认 24 小时 |
| `ENCRYPTION_KEY` | 内置开发密钥 | 数据库字段加密密钥，生产环境必须覆盖 |
| `CACHE_TYPE` | `none` | 可配置为 `redis` 启用 Redis 缓存 |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `BAICHUAN_API_KEY` | `your_baichuan_api_key_here` | 百川智能 API Key |
| `BAICHUAN_MODEL_NAME` | `Baichuan2-Turbo-192k` | 百川模型名称 |
| `CHROMA_ENABLE` | `false` | 是否启用 Chroma RAG 知识库 |
| `CHROMA_URL` | `http://localhost:8000` | Chroma 服务地址 |
| `CHROMA_COLLECTION` | `medical-knowledge` | 医疗知识库集合名 |

### Spring Profile

| Profile | 文件 | 适用场景 |
| --- | --- | --- |
| `h2` | `application-h2.yml` | 默认开发与快速演示，使用内存数据库 |
| `mysql` | `application-mysql.yml` | 本地或服务器 MySQL 持久化运行 |
| `docker` | `application-docker.yml` | Docker Compose 环境 |
| `prod` | `application-prod.yml` | 生产运行配置 |
| `secure` | `application-secure.yml` | 安全增强配置 |

### 前端配置

前端配置位于 `frontend/vite.config.js`：

- 开发服务端口：`5173`
- 本地代理：`/api` → `http://localhost:8080`
- 路径别名：`@` → `frontend/src`
- 构建输出目录：`frontend/dist`

### 边缘端配置

边缘端配置位于 `edge/config.py`，常用环境变量如下：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MQTT_BROKER` | `192.168.71.7` | 云端 MQTT Broker 地址 |
| `MQTT_PORT` | `1883` | MQTT 端口 |
| `MQTT_CLIENT_ID` | `jetson-orin-nano-01` | 边缘设备客户端 ID |
| `MQTT_USERNAME` | 空 | MQTT 用户名，可选 |
| `MQTT_PASSWORD` | 空 | MQTT 密码，可选 |

## 默认账号

H2 初始化数据和 MySQL 初始化脚本提供以下测试账号：

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `admin123` | 用户管理、系统监控、资源管理、规则管理 |
| 医生 | `doctor` | `doctor123` | 待诊队列、AI 诊断、诊断记录、会诊申请 |
| 护士 | `nurse` | `nurse123` | 患者登记、分诊录入、分诊确认、医生分配 |

生产环境部署后应立即修改默认账号密码，并使用强随机密钥覆盖 `JWT_SECRET` 与 `ENCRYPTION_KEY`。

## 前端页面与角色权限

前端通过 `frontend/src/router/index.js` 配置路由守卫，登录后按角色进入对应工作台。

| 路由 | 页面 | 角色 |
| --- | --- | --- |
| `/login` | 登录页 | 公开 |
| `/doctor-dashboard` | 医生工作台 | `DOCTOR` |
| `/nurse-dashboard` | 护士工作台 | `NURSE` |
| `/admin-dashboard/overview` | 管理员系统概览 | `ADMIN` |
| `/admin/resources` | 医疗资源管理 | `ADMIN` |
| `/admin/users` | 用户管理 | `ADMIN` |
| `/admin/logs` | 系统日志 | `ADMIN` |
| `/admin/monitoring` | 系统监控 | `ADMIN` |
| `/admin/config` | 系统配置 | `ADMIN` |
| `/admin/rules` | Drools 规则管理 | `ADMIN` |
| `/admin/patients` | 已确诊患者 | `ADMIN` |

## 后端接口概览

后端统一上下文路径为 `/api`。下表列出主要业务模块与控制器基路径。

| 模块 | 基础路径 | 主要能力 |
| --- | --- | --- |
| 认证 | `/api/auth` | 登录、登出、当前用户、认证检查 |
| 管理后台 | `/api/admin` | 概览、用户、资源、日志、监控、配置、规则、统计、边缘设备 |
| 医生工作台 | `/api/doctor` | 医生统计、患者队列、AI 诊断、诊断保存、会诊申请 |
| 护士工作台 | `/api/nurse` | 护士统计、患者登记、分诊队列、生命体征、医生分配、急救呼叫 |
| 分诊业务 | `/api/triage` | 边缘数据接入、待分诊患者、分诊确认、诊断队列、资源分配 |
| 分诊确认 | `/api/triage-confirmation` | 待确认列表、确认、驳回、详情、批量确认、统计 |
| AI 集成 | `/api/ai` | 智能分诊、深度诊断、科室推荐、设备推荐、HL7、RAG、向量检索 |
| RAG 医疗 AI | `/api/rag-medical-ai` | RAG 状态、向量检索、RAG 诊断、添加知识、知识统计 |
| 医疗 AI 测试 | `/api/medical-ai` | AI 状态、诊断测试、分诊测试、药物相互作用测试 |
| 边缘设备 | `/api/edge` | 设备状态、数据统计、未处理数据、质量报告、失败重处理 |
| 边缘数据 | `/api/edge-data` | 边缘数据详情查询 |
| 端云协同 | `/api/edge-cloud` | 协同诊断、性能指标、架构信息、健康检查 |
| 缓存 | `/api/cache` | 缓存统计、清理指定缓存、清理全部缓存 |
| 健康检查 | `/api/actuator/health` | Spring Boot Actuator 健康检查 |

认证接口以外的大多数接口需要携带 JWT：

```http
Authorization: Bearer <token>
```

## 边缘端说明

边缘端位于 `edge/`，详细说明见 `edge/README.md`。

### 支持平台

| 平台 | 用途 | 传感器模式 |
| --- | --- | --- |
| Jetson Orin Nano | 生产部署 | 真实硬件 |
| Ubuntu / Linux | 联调与测试 | 真实硬件或部分模拟 |
| Windows 10 / 11 | 开发调试 | 模拟数据 |

### 数据采集与处理流程

```text
体温 / 心率 / 血氧 / 语音采集
        │
        ▼
卡尔曼滤波、离线语音识别、结构化预处理
        │
        ▼
BERT-Tiny 本地预分诊 + 边缘规则引擎
        │
        ▼
MQTT 上报云端：medical/triage/data
        │
        ▼
云端 AI / Drools / 护士复核 / 医生接诊
```

### 主要 MQTT Topic

| Topic | 方向 | 说明 |
| --- | --- | --- |
| `medical/triage/data` | 边缘端 → 云端 | 分诊采集数据 |
| `medical/device/heartbeat` | 边缘端 → 云端 | 设备心跳 |
| `medical/device/status` | 边缘端 → 云端 | 设备状态 |
| `medical/triage/final` | 云端 → 边缘端 | 最终分诊结果 |

## 容器化部署

`docker-compose.yml` 编排以下服务：

| 服务 | 容器名 | 端口 | 说明 |
| --- | --- | --- | --- |
| MySQL | `medical_web_mysql` | `3306` | 主数据库 |
| Redis | `medical_web_redis` | `6379` | 缓存服务 |
| Mosquitto | `medical_web_mqtt` | `1883` | MQTT Broker |
| Backend | `medical_web_backend` | `8080` | Spring Boot 后端 |
| Frontend | `medical_web_frontend` | `80` | Nginx 前端服务 |

后端镜像采用多阶段构建：

- 构建阶段：`maven:3.9.4-openjdk-17-slim`
- 运行阶段：`openjdk:17-jre-slim`
- 非 root 用户运行
- 内置 Actuator 健康检查
- JVM 默认使用 G1GC 和容器内存参数

### Docker 访问地址

| 服务 | 地址 |
| --- | --- |
| 前端应用 | `http://localhost` |
| 后端 API | `http://localhost:8080/api` |
| 健康检查 | `http://localhost:8080/api/actuator/health` |
| MQTT Broker | `tcp://localhost:1883` |

## 数据库与初始化数据

### H2 开发数据库

默认 `h2` Profile 使用内存数据库：

- 数据库 URL：`jdbc:h2:mem:testdb`
- 初始化脚本：`backend/src/main/resources/data-h2.sql`
- 启动策略：`create-drop`，每次启动重建数据结构

### MySQL 持久化数据库

MySQL 初始化脚本位于根目录 `init.sql`，包含：

- `users`：用户与角色
- `patients`：患者信息
- `triage_records`：分诊记录
- `diagnosis_records`：诊断记录
- `medical_resources`：医疗资源
- `edge_device_data`：边缘设备数据
- `system_logs`：系统日志
- `hl7_message_mapping`：HL7 消息映射
- `vector_knowledge_base`：向量知识库映射

后端还包含迁移脚本目录：

```text
backend/src/main/resources/db/migration/
```

当前包含护士修正记录表相关迁移脚本。

## AI 与 RAG 知识库

### 大模型配置

后端通过 LangChain4j 调用百川智能接口，默认配置：

```yaml
medical:
  ai:
    provider: baichuan
    model:
      base-url: https://api.baichuan-ai.com/v1
      name: Baichuan2-Turbo-192k
      temperature: 0.2
      max-tokens: 512
```

生产环境必须通过环境变量设置：

```bash
BAICHUAN_API_KEY=your_baichuan_api_key
```

### RAG 知识库

向量知识库默认关闭：

```bash
CHROMA_ENABLE=false
```

如需启用 RAG，需要启动 ChromaDB 服务并配置：

```bash
CHROMA_ENABLE=true
CHROMA_URL=http://localhost:8000
CHROMA_COLLECTION=medical-knowledge
```

项目提供以下脚本辅助初始化和扩展知识库：

```text
backend/init_chroma_db.py
backend/expand_knowledge.py
```

## 实时通信与端云协同

### WebSocket

后端 WebSocket 配置：

- STOMP 端点：`/api/websocket`
- SockJS：已启用
- 消息代理前缀：`/topic`、`/queue`
- 应用消息前缀：`/app`
- 用户消息前缀：`/user`

前端通过 `frontend/src/composables/useWebSocket.js` 与 `frontend/src/utils/websocket.js` 封装实时消息能力。

### MQTT

后端 MQTT 默认配置：

```yaml
mqtt:
  broker:
    url: tcp://localhost:1883
  client:
    id: medical-cloud-server
  topic:
    triage: medical/triage/data
    device:
      status: medical/device/status
    heartbeat: medical/device/heartbeat
    final: medical/triage/final
```

边缘端通过 `edge/mqtt_client/mqtt_publisher.py` 上报分诊数据与设备心跳，云端通过 `MqttMessageHandler` 处理消息。

## 开发与测试命令

### 后端

```bash
cd backend

# 编译
mvn clean compile

# 运行 H2 开发环境
mvn spring-boot:run

# 运行 MySQL 环境
mvn spring-boot:run -Dspring.profiles.active=mysql

# 打包
mvn clean package -DskipTests

# 运行测试
mvn test
```

### 前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务
npm run dev

# 构建生产包
npm run build

# 本地预览生产包
npm run preview
```

### 边缘端

```bash
cd edge

# 安装依赖
pip install -r requirements.txt

# 启动 GUI 程序
python gui_main.py

# 启动命令行程序
python main.py

# 训练 BERT-Tiny 模型
python train_model.py

# 测试模型
python test_model.py
```

### Docker

```bash
# 构建并启动全部服务
docker compose up --build -d

# 查看服务状态
docker compose ps

# 查看全部日志
docker compose logs -f

# 查看后端日志
docker compose logs -f backend

# 停止服务
docker compose down
```

## 常见问题

### 后端启动失败

检查 JDK 版本是否为 17+：

```bash
java -version
```

若使用 MySQL Profile，检查 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD` 是否正确。

### 前端请求后端失败

确认后端已启动在 `8080` 端口，并确认前端开发服务器代理配置仍为：

```text
/api -> http://localhost:8080
```

### 登录失败

确认数据库初始化脚本已执行，并确认默认账号存在。H2 模式每次重启都会重新初始化数据。

### Docker Compose 启动失败

确认根目录存在 `.env` 文件，并至少包含：

```bash
MYSQL_ROOT_PASSWORD=...
MYSQL_PASSWORD=...
JWT_SECRET=...
BAICHUAN_API_KEY=...
```

同时确认 `3306`、`6379`、`1883`、`8080`、`80` 端口未被占用。

### AI 服务不可用

检查：

- `BAICHUAN_API_KEY` 是否正确。
- 网络是否能访问百川智能 API。
- 后端日志 `logs/medical-web.log` 中是否存在 AI 调用错误。

### RAG 向量检索不可用

检查：

- `CHROMA_ENABLE` 是否为 `true`。
- ChromaDB 服务是否启动。
- `CHROMA_URL` 与 `CHROMA_COLLECTION` 是否正确。
- 是否已执行知识库初始化脚本。

### MQTT 设备无法连接

检查：

- Mosquitto 是否启动。
- `MQTT_BROKER` / `MQTT_PORT` 是否指向云端 Broker。
- 防火墙是否放行 `1883`。
- 边缘端 `config.py` 或环境变量是否配置正确。

### Windows 边缘端没有传感器数据

Windows 环境默认用于开发测试，真实传感器驱动主要面向 Linux / Jetson。Windows 下可使用模拟数据验证 GUI、模型推理和 MQTT 上报流程。

## 生产环境建议

- 使用 MySQL Profile 或 Docker Compose 部署，避免使用 H2 内存数据库保存正式数据。
- 使用强随机值覆盖 `JWT_SECRET` 与 `ENCRYPTION_KEY`。
- 修改默认账号密码，禁用不必要的测试账号。
- 为 MQTT Broker 配置认证和网络访问控制。
- 将 `BAICHUAN_API_KEY`、数据库密码等敏感信息放入环境变量或密钥管理系统，不提交到 Git。
- 生产环境建议关闭详细 SQL 输出与 DEBUG 日志。
- 定期备份 MySQL 数据、Redis 数据和 Chroma 向量库数据。
- 对医疗 AI 诊断结果保留人工复核流程，避免将模型输出作为唯一诊断依据。
