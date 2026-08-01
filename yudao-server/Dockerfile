## AdoptOpenJDK 停止发布 OpenJDK 二进制，而 Eclipse Temurin 是它的延伸，提供更好的稳定性


# ===== 第一阶段：构建阶段 =====
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

## 先复制 pom.xml 加速依赖缓存
COPY pom.xml .
COPY yudao-dependencies/pom.xml yudao-dependencies/
COPY yudao-framework/ yudao-framework/

## 下载依赖（利用 Docker 层缓存）
RUN mvn dependency:go-offline -B -q || true

## 复制全部源码
COPY . .

## 构建项目（跳过测试）
RUN mvn clean package -DskipTests -q

# ===== 第二阶段：运行阶段 =====
FROM eclipse-temurin:21-jre

## 创建目录，并使用它作为工作目录
RUN mkdir -p /yudao-server
WORKDIR /yudao-server

## 从构建阶段复制 jar
COPY --from=builder /workspace/yudao-server/target/yudao-server.jar app.jar

## 设置 TZ 时区
ENV TZ=Asia/Shanghai
## 设置 JAVA_OPTS 环境变量，可通过 docker run -e "JAVA_OPTS=" 进行覆盖
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

## 应用参数
ENV ARGS=""

## 暴露后端项目的 48080 端口
EXPOSE 48080

## N1 修复：创建非 root 用户并切换，提升容器运行安全性（指定 UID/GID=1000，与 K8s runAsUser/fsGroup 对齐）
RUN groupadd -r -g 1000 yudao && useradd -r -u 1000 -g yudao -d /yudao-server -s /sbin/nologin yudao \
    && chown -R yudao:yudao /yudao-server
USER yudao

## N2 修复：健康检查，使用 Actuator health 端点（容器内 127.0.0.1 可达）
## 注意：Actuator 配置在 48090 端口（绑定 127.0.0.1），Docker 容器内 127.0.0.1 即容器自身
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://127.0.0.1:48090/actuator/health || exit 1

## 启动后端项目
CMD java ${JAVA_OPTS} -jar app.jar $ARGS
