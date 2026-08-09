# 🛡️ 医疗急诊分诊系统 - 安全多阶段构建
# 阶段1: 构建阶段
FROM maven:3.9.4-openjdk-17-slim AS build

# 设置工作目录
WORKDIR /app

# 复制pom.xml和下载依赖（利用Docker层缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建应用
COPY src ./src
RUN mvn clean package -DskipTests -B

# 阶段2: 运行时阶段
FROM openjdk:17-jre-slim AS runtime

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 安装必要的系统工具
RUN apt-get update && apt-get install -y \
    curl \
    netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

# 🛡️ 创建非root用户
RUN groupadd -r medical && useradd --no-log-init -r -g medical medical

# 设置工作目录
WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=build /app/target/*.jar app.jar

# 🛡️ 设置文件权限
RUN chown -R medical:medical /app

# 🛡️ 切换到非root用户
USER medical

# 🛡️ 暴露端口
EXPOSE 8080

# 🛡️ JVM优化和安全参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m \
    -XX:+UseG1GC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+DisableExplicitGC \
    -XX:+PrintGCDetails \
    -XX:+PrintGCTimeStamps \
    -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod,secure \
    -Dlogging.file.name=/app/logs/application.log"

# 🛡️ 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 🛡️ 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]