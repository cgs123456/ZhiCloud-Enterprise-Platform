# 私有化交付：安装与启动手册（含本轮修掉的 prod 启动阻塞）

> 面向**单客户私有化试点**的交付人员。口径：本文只覆盖"能不能装起来、能不能起来、起来之后能不能用"。
> 生产就绪门禁（G4 部署/G5 可观测/G6 合规/G7 运营/G8 业务验收）与许可法务结论**不在本文范围**，
> 仍以 `docs/enterprise-platform/05-生产就绪门禁清单.md` 与风险登记册为准。

---

## 1. 两条交付路径

| 路径 | 组成 | 适用 |
| --- | --- | --- |
| A. Kubernetes（Helm + ArgoCD） | `deploy/helm/zhicloud`：PreSync 迁移 Job → PreSync 引导管理员 Job → Deployment（`SPRING_FLYWAY_ENABLED=false`，只在 Job 里迁移） | 客户已有 K8s |
| B. 单机 Docker Compose | `script/docker/docker-compose.yml`：MySQL/Redis/PostgreSQL + `db-migration`（一次性）+ `bootstrap-admin`（一次性）+ `server` | 单客户试点、单机交付 |

> 两条路径的**迁移链完全一致**（clean baseline → Flyway V2..V218 → 引导管理员），
> 差别只在编排方式。**不要**再往 MySQL 的 initdb.d 里挂 `sql/mysql/*.sql`：
> 那批模块 SQL 与 `zhicloud_platform.sql` 已经不同步（例如 `hr.sql` 往 `system_dict_type` 写 `tenant_id`，
> 而平台 SQL 建表时没有这一列），init 会直接失败、容器根本起不来。

两条路径的**关键约束**一致：

1. **迁移只做一次**：A 路径由 PreSync Job 执行；B 路径由 `db-migration` 一次性服务执行。
   两条路径的业务进程都通过 `SPRING_FLYWAY_ENABLED=false` 关闭自动迁移，不再"起来时顺手迁移"。
2. **引导管理员只做一次**：`scripts/production/init_bootstrap_admin.py`（脚本）或 `BootstrapAdminRunner`（K8s Job），
   幂等语义为 `ensure`（已存在则不动），口令由 Secret 注入，不写死在脚本或镜像里。
3. **prod profile 禁止自动 DDL**：`flowable.database-schema-update=false`、Flowable/Quartz 等引擎表由迁移链建好
   （见 V218）。这是本轮补上的缺口：**在此之前，全新安装 + prod profile 必然启动失败**。
4. **单机 compose 的服务顺序**：`mysql` → `db-migration`（跑完退出）→ `bootstrap-admin`（跑完退出）→ `server`。
   业务进程带 `SPRING_FLYWAY_ENABLED=false`，不会自己迁移。
5. **演示业务数据另行导入**：compose 默认只装平台 schema 与平台种子数据；
   需要演示数据时执行 `demo-data/sql/mysql/*_demo_seed.sql`（租户 9001 专用，独立于交付库）。

## 2. prod profile 必填环境变量

以下变量缺任意一个，服务**起不来**（占位符无默认值，直接 fail-closed）：

| 变量 | 说明 |
| --- | --- |
| `MASTER_DATASOURCE_URL` / `MASTER_DATASOURCE_USERNAME` / `MASTER_DATASOURCE_PASSWORD` | 主库连接 |
| `REDIS_PASSWORD` | Redis 口令（`REDIS_HOST`/`REDIS_PORT` 有默认值） |
| `MYBATIS_ENCRYPTOR_PASSWORD` | MyBatis-Plus 字段加解密密钥（`:?required`） |
| `MYSQL_TLS_TRUSTSTORE_URL` / `MYSQL_TLS_TRUSTSTORE_PASSWORD` | JDBC 连接属性；即使不用 MySQL TLS 也必须给值（否则占位符解析失败）。不使用 TLS 时给占位值即可，`sslMode` 不为 `VERIFY_IDENTITY` 时不会被使用 |
| `SBA_USERNAME` / `SBA_PASSWORD` | Spring Boot Admin 客户端；不接 SBA 也要给占位值 |

以下变量**建议**显式配置（有默认值，但默认值对生产不合适）：

| 变量 | 默认值 | 建议 |
| --- | --- | --- |
| `CORS_ALLOWED_ORIGINS` | `https://localhost` | 改成客户域名；前后端同源部署时保持与 nginx 的 `server_name` 一致 |
| `LOG_FILE` | `/var/log/zhicloud-server/application.log` | 改成容器/主机内可写路径 |
| `RABBITMQ_HOST/PORT/USERNAME/PASSWORD` | `127.0.0.1:5672 guest/guest` | 未部署 MQ 时 IM 实时消息不可用；不要指向不存在的地址 |
| `DRUID_ENABLED` / `DRUID_USERNAME` / `DRUID_PASSWORD` | `false` / 空 | 默认关闭；需要时连同 IP 白名单一起开 |
| `WX_MP_APP_ID` / `WX_MP_SECRET` / `WX_MINIAPP_APPID` / `WX_MINIAPP_SECRET` | 空 | 不接微信可以留空，但**变量必须存在**（本轮已把 prod 的占位结构补齐） |
| `DATALAKE_ENABLED` | `false` | Iceberg/Trino，未部署保持关闭 |

**引导管理员口令的长度窗口是 12–16 位**：`BootstrapAdminRunner` 要求 ≥12 位（OWASP 基线），
登录接口 `AuthLoginReqVO` 又要求 ≤16 位。超长口令会被引导脚本接受、但登录时被参数校验拒绝，
表现为"账号建好了却登不进去"。本轮已在两处（`BootstrapAdminRunner`、
`scripts/production/init_bootstrap_admin.py`）加上限校验，并把脚本默认生成长度从 24 字节改成 12 字节
（`token_urlsafe(12)` = 16 字符，正好落在窗口内）。

## 3. 本轮修掉的 5 个"prod 起不来"阻塞

| # | 现象 | 根因 | 修复 |
| --- | --- | --- | --- |
| 1 | `BeanDefinitionOverrideException: Invalid bean definition with name 'vectorStore'` | `spring.autoconfigure.exclude` 是 **List 属性，profile 覆盖是整体替换不是合并**：base 排除了 pgvector，local/dev 又各自重写了整个列表（漏 pgvector），prod 干脆没写 → prod 下 Milvus 与 Qdrant 两个自动配置同时注册名为 `vectorStore` 的 bean | base/local/dev/prod 四个 profile 的排除列表统一写全 7 项，并在注释里写明"覆盖即替换，必须写全" |
| 2 | `WxRuntimeException: appid不能设置为null` | prod profile 完全没有 `wx.mp` / `wx.miniapp` 配置，WxJava starter 建 bean 时取不到 appid | prod 补齐与 local 同构的 `wx` / `justauth` 配置块，取值全部走环境变量 |
| 3 | `no flowable tables in db ... Table 'ACT_GE_PROPERTY' doesn't exist` | Flowable 的建表 DDL 只随 jar 发布，仓库里既没有 `flowable.sql` 也没有任何迁移会创建 `ACT_*`；prod 禁用自动 DDL | 新增迁移 **V218**：把 Flowable 8.0.0 官方 MySQL DDL 纳入迁移链并改写为幂等形式（`create table if not exists` + `information_schema` 判断 + `PREPARE` 执行） |
| 4 | 交付配置默认开启 AI-RAG，但 BGE 模型不在 jar 内 | `values.yaml` / `docker-compose.yml` 默认 `airag.enabled=true`，而模型文件要靠 `scripts/download-bge-model.sh` 下载，镜像里没有 → 启用即启动失败 | 两处默认值改为 `false`，并注释"启用需同时具备 PostgreSQL+pgvector 与 classpath:airag/ 下的模型文件" |
| 5 | 引导管理员口令超长 → 账号建好却登不进去 | `BootstrapAdminRunner` 只校验下限（≥12）与字节上限（72），登录接口 `AuthLoginReqVO` 限死 4–16 位；`init_bootstrap_admin.py` 默认生成 24 字节（32 字符）口令，**必然超限** | 两处补 16 位上限校验；脚本默认生成长度改 12 字节（16 字符） |

另外：`.env.prod` 里原本硬编码了研发用的穿透域名 `https://f9979cc73c50.hz-1.aicp.space`，
私有化交付若不覆盖，前端会打到别人家的隧道域名 → 改为默认**同源**（留空），需要分离部署时用
`VITE_BASE_URL=https://api.customer.com pnpm build:prod` 覆盖。

## 4. 部署期硬门禁（迁移 Job 会在这些不通过时直接失败）

`DatabaseMigrationRunner` 的"迁移后语义校验"现在覆盖 9 个脚本，任一项非 PASS 就阻断发布：

| 脚本 | 检查内容 |
| --- | --- |
| V116/V117/V118/V119/V120/V121 | 各自的租户/幂等约束结构与数据不变量 |
| V216 | crm_admin 角色的 CRM 菜单绑定（不再为空）、MES 误绑已清理 |
| V217 | MES BOM 菜单组件路径大小写已修正 |
| V218 | Flowable 引擎核心表与 schema 版本行已就位 |

## 5. 反向代理（nginx）必须满足的点

1. `/admin-api` → 后端 48080，**保留 Host 头**（`proxy_set_header Host $host;`），
   否则后端会把同源请求判成跨域，POST/PUT/DELETE 全部返回 `Invalid CORS request`。
2. `/infra/ws` → 后端 48080，并且要带 WebSocket 升级头：
   ```nginx
   location /infra/ws {
       proxy_pass http://backend;
       proxy_http_version 1.1;
       proxy_set_header Upgrade $http_upgrade;
       proxy_set_header Connection "upgrade";
       proxy_set_header Host $host;
       proxy_read_timeout 3600s;
   }
   ```
   后端只接受**同源**握手（带跨域 `Origin` 的握手返回 403），因此前端必须与接口同域。
3. 前端静态资源 + SPA 回退（`try_files $uri /index.html`）。
4. 仓库里现存的 nginx 样例（`docs/enterprise-platform/evidence/production/prodlike-e2e-20261009/nginx.conf`）
   **没有** WebSocket 转发，直接照抄会导致 IM 实时消息不可用。

## 6. 本地彩排证据（单机沙箱，非生产验证）

| 步骤 | 命令 / 入口 | 结果 |
| --- | --- | --- |
| 干净库建表 | `DatabaseMigrationRunner`（`MIGRATION_BASELINE_MODE=clean`） | 221 个迁移全部成功，版本到 v217 |
| 迁移后语义校验 | 同上 | 8 个脚本全 PASS（V216/V217 当时已加入） |
| 引导管理员 | `BootstrapAdminRunner`（`BOOTSTRAP_ADMIN_MODE=ensure`） | `action=created username=pilotadmin tenantId=1 roleId=1 postId=4` |
| 增量迁移 | `DatabaseMigrationRunner`（`existing` 模式） | 应用 V218，语义校验扩到 9 个脚本全 PASS |
| prod profile 启动 | `--spring.profiles.active=prod` + 完整环境变量 | `Started ZhiCloudServerApplication in 149.637 seconds` |
| 登录 + 权限 | `POST /admin-api/system/auth/login`（带 `tenant-id` 头） | `code=0`，拿到 accessToken；`get-permission-info` → `code=0`，顶级菜单 29、权限 1646 |

单机 compose 路径（空库 → db-migration → bootstrap-admin → server）的代码级彩排同样跑通：
222 个迁移 + 9 个语义校验全 PASS、账号创建成功、登录 `code=0`、权限接口 29 顶级菜单 / 1646 权限。

证据文件：`docs/enterprise-platform/evidence/production/business-pilot-deploy-20261020/`。

> 边界：以上是**单机本地沙箱**证据，不证明生产拓扑、备份回滚、容量、复制与故障切换。

## 7. 仍然需要部署/运维侧做的事

- 部署 MySQL/Redis（+ 需要时的 PostgreSQL/TDengine），并把口令放进 Secret，不要落进镜像或仓库。
- nginx 按 §5 配置（含 WebSocket）。
- 首次上线前跑一次备份恢复演练（G7 未闭环）。
- 可观测性（指标/日志/告警/on-call）属于 G5，本轮未涉及。
- 许可合规属于法务，本轮未涉及。
