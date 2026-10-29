# 使用 Maven 镜像进行构建
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建项目（跳过测试以加快构建速度）
RUN mvn clean package -DskipTests -B -X

# 验证构建结果
RUN echo "=== Checking build output ==="
RUN find / -name "excel*.jar" 2>/dev/null || echo "No excel jar found in /"
RUN ls -la /app/target/*.jar 2>/dev/null || echo "No JAR files in /app/target/"
RUN test -f /app/target/excel-1.0.0.jar && echo "SUCCESS: /app/target/excel-1.0.0.jar exists" || echo "FAIL: /app/target/excel-1.0.0.jar not found"

# 使用 OpenJDK 17 运行时镜像
FROM openjdk:17-jdk-slim

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 JAR 包（明确指定 Spring Boot 可执行 jar）
COPY --from=builder /app/target/excel-1.0.0.jar excel.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 参数优化
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar excel.jar"]
