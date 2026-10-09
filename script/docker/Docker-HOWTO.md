# Docker Build & Up

目标: 快速部署体验系统，帮助了解系统之间的依赖关系。
依赖：docker compose v2，删除`name: zhicloud-system`，降低`version`版本为`3.3`以下，支持`docker-compose`。

## 功能文件列表

```text
.
├── Docker-HOWTO.md                 
├── docker-compose.yml              
├── .env.example                    <-- 环境变量模板；先 cp 成 .env 再填真实密码
├── zhicloud-server
│   └── Dockerfile
└── zhicloud-ui-admin
    ├── .dockerignore
    ├── Dockerfile
    └── nginx.conf                  <-- 提供基础配置，gzip压缩、api转发
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
cp .env.example .env        # 先把三个必填密码改成真实强密码
docker compose up -d
```

首次运行会自动构建容器。可以通过`docker compose build [service]`来手动构建所有或某个docker镜像

`.env` 不是可选装饰：`docker-compose.yml` 对 `MYSQL_ROOT_PASSWORD` / `REDIS_PASSWORD` / `POSTGRES_PASSWORD` 用的是 `${VAR:?required}` 形式（无默认值），**不提供就会直接报错退出**。其余变量才有默认值。

## 服务器的宿主机端口映射

- admin ui: http://localhost:8080
- api server: http://localhost:48080
- mysql: root/${MYSQL_ROOT_PASSWORD}, port: 3306（密码通过 .env 注入）
- redis: port: 6379（密码通过 .env 注入）
