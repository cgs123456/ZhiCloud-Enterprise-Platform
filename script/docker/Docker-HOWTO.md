# Docker Build & Up

目标: 快速部署体验系统，帮助了解系统之间的依赖关系。
依赖：docker compose v2，删除`name: zhicloud-system`，降低`version`版本为`3.3`以下，支持`docker-compose`。

## 功能文件列表

```text
script/docker/
├── Docker-HOWTO.md
├── docker-compose.yml
├── docker-compose-langfuse.yml
├── .env.example                    <-- 环境变量模板；先 cp 成 .env 再填真实密码
├── grafana/ loki/ nacos/ prometheus/ trino/   <-- 各中间件挂载配置
└── (server 镜像由 ../../zhicloud-server/Dockerfile 构建)

说明：本 compose 不包含前端 admin UI 服务。前端请按根 README 的
「前端启动（Vue3 + Vite）」在 zhicloud-ui/zhicloud-ui-admin-vue3 下用 pnpm dev 启动。
```

## 构建 jar 包

```shell
# 创建maven缓存volume
docker volume create --name zhicloud-maven-repo

docker run -it --rm --name zhicloud-maven \
    -v zhicloud-maven-repo:/root/.m2 \
    -v $PWD:/usr/src/mymaven \
    -w /usr/src/mymaven \
    maven mvn clean install package '-Dmaven.test.skip=true'
```

## 构建启动服务

```shell
cp .env.example .env        # 把模板里的占位密码全部改成真实强密码
docker compose up -d
```

首次运行会自动构建容器。可以通过`docker compose build [service]`来手动构建所有或某个docker镜像

`.env` 不是可选装饰：`docker-compose.yml` 对 **11 个**变量使用 `${VAR:?required}` 形式（无默认值）——`MYSQL_ROOT_PASSWORD` / `REDIS_PASSWORD` / `POSTGRES_PASSWORD` / `GRAFANA_PASSWORD` / `MASTER_DATASOURCE_PASSWORD` / `SLAVE_DATASOURCE_PASSWORD` / `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` / `NACOS_AUTH_TOKEN` / `NACOS_AUTH_IDENTITY_KEY` / `NACOS_AUTH_IDENTITY_VALUE`，**不提供就会直接报错退出**。`.env.example` 已包含全部 11 项。

## 服务器的宿主机端口映射

- api server: http://localhost:48080（未认证时返回 JSON，不是管理界面）
- actuator 管理端口: http://localhost:48090
- mysql: root/${MYSQL_ROOT_PASSWORD}, port: 3306（密码通过 .env 注入）
- redis: port: 6379（密码通过 .env 注入）
- postgres(pgvector): port: 5432
- minio: http://localhost:9000（控制台 9001）
- nacos: http://localhost:8848（gRPC 9848）
- trino: http://localhost:8080
- prometheus: http://localhost:9090 / grafana: http://localhost:3000
- jaeger UI: http://localhost:16686 / alertmanager: http://localhost:9093 / loki: http://localhost:3100

> 前端管理界面由 Vite 提供（默认 http://localhost/），**不在本 compose 内**；
> 历史文档写的 "admin ui: http://localhost:8080" 是错的——8080 实际映射给 Trino。
