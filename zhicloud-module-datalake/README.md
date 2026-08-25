# zhicloud-module-datalake 数据湖仓模块

> P2-5 Iceberg 数据湖仓基础架构

## 模块定位

基于 Apache Iceberg + Trino 实现数据湖仓基础架构，提供三大核心能力：

1. **历史数据冷归档**：MySQL 热数据 → Flink CDC / 批量 ETL → Iceberg 冷数据
2. **AI 训练数据源**：通过 MCP 工具暴露数据湖查询能力给 AI Agent
3. **BI 分析加速**：Trino 分布式查询，避免重查询压垮业务库

## 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        zhicloud-server（主应用）                        │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              zhicloud-module-datalake（本模块）                 │   │
│  │                                                             │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │   │
│  │  │ DataLake     │  │ Iceberg      │  │ DataLakeMcpTool  │  │   │
│  │  │ Controller   │  │ CatalogSvc   │  │ (@Tool 暴露)     │  │   │
│  │  │ (REST API)   │  │ (REST 代理)  │  │                  │  │   │
│  │  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │   │
│  │         │                 │                   │            │   │
│  │         │     ┌───────────┴───────────┐       │            │   │
│  │         │     │  DataArchivalService  │       │            │   │
│  │         │     │  （占位实现 + 日志）   │       │            │   │
│  │         │     └───────────────────────┘       │            │   │
│  └─────────┼────────────────┼───────────────────┼────────────┘   │
│            │                │                   │                │
│            │    REST API    │     @Tool         │                │
└────────────┼────────────────┼───────────────────┼────────────────┘
             │                │                   │
             ▼                ▼                   ▼
      ┌──────────┐     ┌──────────┐      ┌──────────────┐
      │  Trino   │     │  Trino   │      │  AI Agent    │
      │  :8080   │     │  :8080   │      │ (MCP Client) │
      │ /v1/     │     │ /v1/     │      └──────────────┘
      │ statement│     │ statement│
      └────┬─────┘     └────┬─────┘
           │                │
           ▼                ▼
      ┌─────────────────────────────┐
      │     Iceberg（表格式）        │
      │  ┌─────┐ ┌─────┐ ┌─────┐   │
      │  │ ods │ │ dwd │ │ dws │   │
      │  │     │ │     │ │     │   │
      │  └─────┘ └─────┘ └─────┘   │
      └─────────────┬───────────────┘
                    │
                    ▼
      ┌─────────────────────────────┐
      │  MinIO（S3 兼容存储）        │
      │  s3://zhicloud-warehouse        │
      │  :9000 (S3 API)              │
      │  :9001 (Console)             │
      └─────────────────────────────┘

数据流：
  MySQL（热数据）──Flink CDC / 批量 ETL──▶ Iceberg（冷数据）──Trino 查询──▶ MCP 工具──▶ AI Agent
```

## 部署指南

### 1. 启动 Trino + MinIO

```bash
cd script/docker
docker compose up -d trino minio
```

### 2. 配置 Trino Iceberg Catalog

Trino catalog 配置文件位于 `script/docker/trino/catalog/iceberg.properties`，
已通过 docker-compose 挂载到 Trino 容器的 `/etc/trino/catalog/` 目录。

关键配置：
- `connector.name=iceberg`：使用 Iceberg 连接器
- `iceberg.rest-catalog.warehouse=s3://zhicloud-warehouse`：Iceberg 仓库路径
- `iceberg.rest-catalog.s3.endpoint=http://zhicloud-minio:9000`：MinIO S3 端点

### 3. 初始化 MinIO Bucket

启动 MinIO 后，访问 `http://localhost:9001`（用户名/密码：minioadmin/minioadmin123），
创建 `zhicloud-warehouse` bucket。

### 4. 启用数据湖仓模块

在 `application-prod.yaml` 中设置：

```yaml
zhicloud:
  datalake:
    enabled: true
    catalog-uri: http://trino:8080
    warehouse-path: s3://zhicloud-warehouse
```

或通过环境变量：

```bash
DATALAKE_ENABLED=true
DATALAKE_CATALOG_URI=http://trino:8080
DATALAKE_WAREHOUSE_PATH=s3://zhicloud-warehouse
```

## 配置说明

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| `zhicloud.datalake.enabled` | `DATALAKE_ENABLED` | `false` | 是否启用数据湖仓模块 |
| `zhicloud.datalake.catalog-uri` | `DATALAKE_CATALOG_URI` | `http://trino:8080` | Trino REST API 地址 |
| `zhicloud.datalake.warehouse-path` | `DATALAKE_WAREHOUSE_PATH` | `s3://zhicloud-warehouse` | 归档存储路径 |
| `zhicloud.datalake.retention-days` | `DATALAKE_RETENTION_DAYS` | `365` | 默认归档保留天数 |
| `zhicloud.datalake.batch-size` | - | `10000` | 归档批处理大小 |
| `zhicloud.datalake.archive-tables` | - | `[]` | 需要归档的表列表 |

## MCP 工具列表

本模块通过 `@Tool` 注解暴露以下工具，供 AI Agent 调用：

| 工具名 | 描述 | 权限码 |
|--------|------|--------|
| `datalake_list_tables` | 列出数据湖中指定命名空间下的所有表 | `datalake:query` |
| `datalake_query_table` | 对数据湖执行只读 SELECT 查询（白名单校验） | `datalake:query` |
| `datalake_get_archive_status` | 查询指定业务表的归档状态 | `datalake:query` |

### SQL 注入防护

`datalake_query_table` 工具执行严格的 SQL 白名单校验：

1. **语句前缀检查**：仅允许 `SELECT` / `WITH` 开头的语句
2. **危险字符检查**：拒绝分号（`;`）、注释符号（`--`、`/*`、`*/`）
3. **禁止关键字检查**：拒绝所有 DDL/DML 关键字（`INSERT`/`UPDATE`/`DELETE`/`DROP`/`ALTER`/`CREATE`/`TRUNCATE` 等）
4. **多语句防护**：拒绝分号，防止多语句注入

## REST API

| 接口 | 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|------|
| 列出命名空间 | GET | `/datalake/namespaces` | `datalake:query` | 列出所有 Iceberg 命名空间 |
| 列出表 | GET | `/datalake/tables?namespace=ods` | `datalake:query` | 列出指定命名空间的表 |
| 表结构 | GET | `/datalake/table-schema?namespace=ods&table=mes_pro_work_order` | `datalake:query` | 获取表 schema |
| 归档状态 | GET | `/datalake/archive-status?tableName=mes_pro_work_order` | `datalake:query` | 查询归档状态 |
| 触发归档 | POST | `/datalake/archive?tableName=mes_pro_work_order&beforeDate=2025-01-01` | `datalake:archive` | 触发归档（占位） |

## 模块结构

```
zhicloud-module-datalake/
├── pom.xml
├── README.md
└── src/main/java/cn/zhicloud/zhicloud/module/datalake/
    ├── DataLakeApplication.java          # 模块入口标记类
    ├── package-info.java                 # Spring Modulith 模块声明
    ├── config/
    │   ├── DataLakeProperties.java       # 配置属性（zhicloud.datalake.*）
    │   └── DataLakeAutoConfiguration.java # 自动配置（@ConditionalOnProperty）
    ├── service/
    │   ├── IcebergCatalogService.java     # Iceberg Catalog 管理接口
    │   ├── IcebergCatalogServiceImpl.java # 实现（Trino REST API 代理）
    │   ├── DataArchivalService.java       # 历史数据归档接口（SPI）
    │   └── DataArchivalServiceImpl.java   # 占位实现 + 日志
    ├── mcp/
    │   ├── DataLakeMcpTool.java           # MCP 工具暴露（@Tool）
    │   └── DataLakeMcpToolRequiresPermission.java # 权限注解
    └── controller/admin/
        └── DataLakeController.java        # 管理接口
```

## 未来扩展方向

1. **Flink CDC 实时入湖**：通过 Flink CDC 监听 MySQL binlog，实时同步到 Iceberg
2. **物化视图**：基于 Trino 物化视图加速常用查询
3. **数据血缘**：接入 Apache Atlas / DataHub 实现数据血缘追踪
4. **分区策略**：按时间分区（日/月），优化查询性能
5. **数据压缩**：Iceberg 表压缩与合并（compaction）
6. **多租户隔离**：按租户分命名空间，实现数据隔离
7. **归档任务调度**：对接 zhicloud-spring-boot-starter-job / XXL-Job
8. **Schema 演进**：Iceberg schema 演进（添加列、重命名列等）

## 重要说明

- **当前为基础架构**：仅包含配置管理、查询代理、MCP 工具暴露，**不包含数据迁移逻辑**
- **默认关闭**：`enabled=false`，不影响现有功能
- **编译期不依赖 Iceberg/Trino/Flink**：运行时通过 REST API 与独立部署的服务交互
- **数据迁移时机**：需在数据量达到瓶颈（单表千万级）后启动
- **归档实现为占位**：`DataArchivalServiceImpl` 仅记录日志，实际归档由 Flink CDC 或批量 ETL 完成
