# 私有化交付彩排证据（单机本地沙箱，非生产验证）

> 对应交接文档：`docs/enterprise-platform/handoff/pilot-deployment-20261020.md`。
> 本目录记录"全新安装 → prod profile 启动 → 登录可用"的完整彩排过程与失败定位证据。
> **边界**：单机本地沙箱，不证明生产拓扑、备份回滚、容量、复制与故障切换。

## 1. 彩排环境

| 项 | 值 |
| --- | --- |
| 数据库 | 干净库 `zhicloud_clean_pilot`（MySQL 8.4 容器 `zc-live-mysql`） |
| Redis | `127.0.0.1:16380` |
| 后端 | `zhicloud-server.jar`（本地 `-pl zhicloud-server -am package` 构建产物） |
| profile | `--spring.profiles.active=prod`（与交付一致） |
| 迁移入口 | `DatabaseMigrationRunner`（K8s PreSync Job 用的同一个入口） |
| 引导入口 | `BootstrapAdminRunner`（K8s PreSync Job 用的同一个入口） |

## 2. 执行序列与结果

| # | 步骤 | 结果 |
| --- | --- | --- |
| 1 | `DatabaseMigrationRunner`（`MIGRATION_BASELINE_MODE=clean`） | clean 基线 + **221 个迁移**成功，版本到 v217；迁移后语义校验 8 个脚本全 PASS |
| 2 | `BootstrapAdminRunner`（`BOOTSTRAP_ADMIN_MODE=ensure`） | `action=created username=pilotadmin tenantId=1 roleId=1 postId=4` |
| 3 | `DatabaseMigrationRunner`（`existing` 模式） | 应用 **V218**（Flowable 引擎表），语义校验扩到 **9 个脚本全 PASS** |
| 4 | prod profile 启动 | `Started ZhiCloudServerApplication in 149.637 seconds` |
| 5 | 登录 | `POST /admin-api/system/auth/login`（带 `tenant-id` 头）→ `code=0`，拿到 accessToken |
| 6 | 权限接口 | `GET /admin-api/system/auth/get-permission-info` → `code=0`，**顶级菜单 29、权限 1646** |

## 3. 过程中定位并修掉的 prod 启动阻塞（按发现顺序）

| 序 | 失败信息 | 根因 | 证据文件 |
| --- | --- | --- | --- |
| 1 | `BeanDefinitionOverrideException: Invalid bean definition with name 'vectorStore'` | Milvus 与 Qdrant 两个自动配置都注册同名 bean；prod profile 没排除它们 | `prod-boot-run1-vectorstore-conflict.log` |
| 2 | `WxRuntimeException: appid不能设置为null` | prod profile 缺 `wx.mp` / `wx.miniapp` 占位配置，WxJava starter 建 bean 即失败 | `prod-boot-run2-wx-appid-null.log` |
| 3 | `Failed to configure a DataSource: 'url' attribute is not specified` | 开启 AI-RAG 后 `zhicloud.airag.datasource.url` 未给（交付默认值却是 `airag.enabled=true`），且 BGE 模型不在 jar 内 | `prod-boot-run3-multi-doc-datasource.log` |
| 4 | `no flowable tables in db ... Table 'ACT_GE_PROPERTY' doesn't exist` | prod 禁用自动 DDL，而仓库里没有任何脚本/迁移会建 Flowable 引擎表 | `prod-app-log-minimal-env.log` |

## 4. 最终一次彩排日志

`prod-boot-run-final.log`：prod 启动 → 登录 `code=0` → 权限接口 `code=0`（29 顶级菜单 / 1646 权限）。
`prod-app-log-full-env.log`：prod 实例完整启动日志。

## 5. 单机 compose 路径（开箱即用）彩排

| 步骤 | 结果 |
| --- | --- |
| 空库（compose 不再挂 initdb 脚本） | 0 张表 |
| `db-migration` 一次性服务 | clean baseline + **222 个迁移**成功，版本到 v218；9 个语义校验脚本全 PASS |
| `bootstrap-admin` 一次性服务 | `action=created username=zhicloudadmin tenantId=1 roleId=1 postId=4` |
| `server` 服务（`SPRING_FLYWAY_ENABLED=false`） | 启动成功，`flyway_schema_history` 停在 218 |
| 登录 | `code=0`，拿到 accessToken |
| 权限接口 | `code=0`，顶级菜单 29、权限 1646 |

> 彩排是**代码级**验证：按 compose 的服务顺序执行同一批入口（`DatabaseMigrationRunner` /
> `BootstrapAdminRunner` / 应用启动），没有构建 Docker 镜像，也没有起 compose 的其余中间件。
> 证据：`compose-path-rehearsal.log`（1-3 步）、`compose-path-rehearsal-final.log`（4-5 步）、`compose-probe-server.log`（应用日志）。

## 6. 其它随附证据

| 文件 | 内容 |
| --- | --- |
| `server-module-tests.log` | `zhicloud-server` 模块单测：104 tests / 0 failures / 0 errors / 13 skipped（含迁移校验目录与 SQL 的一致性测试） |
| `build-final.log` | 最终构建日志（BUILD SUCCESS） |
| `build-v218-flowable.log` | 引入 V218 时的构建日志 |
