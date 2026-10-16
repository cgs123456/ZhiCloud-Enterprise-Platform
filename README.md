# 智云·企业数字化平台（ZhiCloud Enterprise Platform）

<p align="center">
 <img src="https://img.shields.io/badge/JDK-21-orange.svg" alt="JDK">
 <img src="https://img.shields.io/badge/Spring%20Boot-3.5.16-blue.svg" alt="Spring Boot">
 <img src="https://img.shields.io/badge/Spring%20Modulith-1.3.5-green.svg" alt="Spring Modulith">
 <img src="https://img.shields.io/badge/Flowable-8.0.0-red.svg" alt="Flowable">
 <img src="https://img.shields.io/badge/MySQL-8.0-blue.svg" alt="MySQL">
 <img src="https://img.shields.io/badge/Redis-7-purple.svg" alt="Redis">
 <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
</p>

> 基于 JDK 21 + Spring Boot 3.5 的企业级全栈数字化平台，覆盖 ERP / MES / WMS / QMS / CRM / HR / OA / TMS 八大业务域，集成 AI 多智能体编排、RAG 混合检索（含可选 ONNX Cross-Encoder reranker，模型缺失时降级为 TF-IDF 打分）、数据湖仓三大 AI 模块，配套 DevOps 全套交付链。

## 📊 项目规模

| 指标 | 数值（2026-10-09 实测，git ls-files） |
|------|------|
| Java 源文件 | 5,882（主代码）+ 404（测试） |
| Controller | 513（`@RestController`/`@Controller` 注解判定，含 `*/controller/` 下 512 个 + IoT `/rpc-api` 设备端 1 个，与 CI 门禁口径一致） |
| Maven 子模块 | 22 个（20 个进构建；IM / MP 未进构建，Report 进构建但未接入 server，见下方「模块启用状态」） |
| Flyway 迁移脚本 | 158 个（最高 V151） |
| SQL 文件 | 341 个（含 Flyway 迁移、多方言安装脚本、demo-data 与审计证据） |
| 单元测试文件 | 404 个 |
| 框架 Starter | 18 个（zhicloud-common + 17 个 spring-boot-starter-*） |

## 🏗️ 技术架构

### 核心技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 21 (Eclipse Temurin 21.0.11) | 虚拟线程（Project Loom）、Pattern Matching、Record |
| Spring Boot | 3.5.16 | 应用开发框架 |
| Spring Modulith | 1.3.5 | 模块化单体架构（@ApplicationModule） |
| Spring Security | 6.5.11 | 认证鉴权、RBAC、SSO |
| MyBatis Plus | 3.5.16 | ORM 增强工具 |
| Redisson | 4.6.1 | 分布式锁、限流、缓存 |
| Flowable | 8.0.0 | BPM 工作流引擎 |
| Druid | 1.2.28 | 数据库连接池、SQL 监控 |
| Spring AI | 1.1.8 | AI 模型接入、ChatClient、Tool 调用 |
| MapStruct | 1.6.3 | Bean 转换 |
| Lombok | 1.18.46 | 代码简化 |
| Flyway | 11.x | 数据库版本管理 |

### 数据库与中间件

| 组件 | 版本 | 用途 |
|------|------|------|
| MySQL | 8.0 | 主业务数据库 |
| Redis | 7 (AOF, io-threads) | 缓存、分布式锁、限流 |
| PostgreSQL + pgvector | 16 | RAG 向量数据库 |
| Apache Iceberg + Trino | 450 | 数据湖仓（冷数据归档） |
| Nacos | 2.4.3 | 配置中心、服务注册 |
| MinIO | - | 对象存储 |
| RocketMQ | - | 消息队列 |

## 📦 业务模块

### 模块启用状态（重要）

根 pom.xml 的 <modules> 才是构建事实来源，README 旧版把未启用模块也算进了规模：

| 状态 | 模块 | 说明 |
|------|------|------|
| **已启用并入 server** | system / infra / bpm / crm / erp / iot / mes / wms / qms / tms / oa / hr / ai / ai-rag / ai-multiagent / datalake | zhicloud-server/pom.xml 中有对应依赖，随应用启动 |
| **进构建但未接入 server** | zhicloud-module-report | 根 pom 已启用、有 16 条测试，但 server 依赖被注释：会引入 jimureport / jimubi 自动配置而 server 无相关配置。启动日志会打印「报表模块 - 已禁用」 |
| **未进构建（预留源码）** | zhicloud-module-im、zhicloud-module-mp | 根 pom 中被注释。源码与测试仍在仓库（IM 21 个测试文件 / MP 0 个），但**不参与 mvn 构建、不随应用启动**，启动日志会打印「已禁用」 |

> 历史提示：商业域模块（Trade / Pay / Promotion / Member / Product）已在 V140 整体移除（74 张表 + 3 个后端模块），本 README 不再列出。

### 八大业务域

| 模块 | Controller | 核心能力 |
|------|-----------|----------|
| **ERP** 企业资源计划 | 70 | 财务全链路（总账/预算/合并/成本/资产/税务/资金/外币）、供应链（采购/销售/库存）、VMI、CPFR、MRP、DDD 销售订单试点 |
| **MES** 制造执行系统 | 148 | 8 子域：cal 排产 / dv 设备点检 / md 主数据 / pro 生产 / qc 质量 / tm 工具 / wm 仓储 / energy 能源；APS+MRP 智能排产、Andon 安灯、OEE、SCADA、PDA 报工、BOM 替代料 |
| **WMS** 仓库管理 | 38 | FEFO 效期管理、SN 序列号追踪、3PL 计费、越库、ABC 分析、PDA 全场景、波次拣货、安全库存预警 |
| **QMS** 质量管理 | 28 | IQC/IPQC/OQC 全流程检验、FMEA、8D 报告、CAPA 闭环、SPC 过程能力、MSA 量测系统、NCR、SCAR、质量追溯、电子签名 |
| **CRM** 客户关系 | 32 | 客户/线索/商机/合同/回款全生命周期、公海池、数据权限、电子签 |
| **HR** 人力资源 | 13 | 员工全生命周期、部门、考勤、薪资、绩效、合同、社保、招聘（职位/简历/面试） |
| **OA** 办公自动化 | 12 | 报销（流程审批）、会议室预约（冲突检测）、公文、知识库（版本控制）、公告、日程管理 |
| **TMS** 运输管理 | 9 | 承运商/车辆/司机/运单/跟踪、运费结算（5 种计费）、运费对账（自动差异计算）、GPS 定位、车队运营（成本利润核算） |

### AI 三件套

| 模块 | 核心能力 |
|------|----------|
| **AI-RAG** 检索增强生成 | Tika 文档解析 + TokenTextSplitter 分块 + PgVector 向量存储 + BM25+向量混合检索 + ONNX Cross-Encoder 重排（可选，`zhicloud.airag.reranker.enabled` 开关；模型文件缺失时自动降级为 TF-IDF `SimpleReranker`，不影响主链路）+ RAG 评估体系 |
| **AI-MultiAgent** 多智能体 | ReAct（Reasoning+Acting）循环编排、Supervisor+Worker 拓扑、Spring AI ChatClient + @Tool 自动收集、步数/Token/超时熔断 |
| **DataLake** 数据湖仓 | Apache Iceberg + Trino 冷数据归档、MCP 工具暴露（datalake_list_tables/query_table/get_archive_status）、SQL 注入 4 重白名单防护 |

### 系统基础设施

| 模块 | 核心能力 |
|------|----------|
| **System** 系统管理 | 用户/角色/菜单/部门/岗位、SaaS 多租户、RBAC 按钮级权限、OAuth2 SSO、TOTP 两步验证、操作日志哈希链 |
| **Infra** 基础设施 | 代码生成器、Swagger 接口文档、数据库文档、定时任务、文件服务（S3/MinIO/本地）、WebSocket、API 日志、MySQL/Redis 监控 |
| **BPM** 工作流 | Flowable 8.0、仿钉钉/飞书设计器 + BPMN 设计器、会签/或签/加签/驳回、表单权限、超时审批、父子流程 |

## 🔧 DevOps 交付链

| 能力 | 工具/实现 |
|------|----------|
| 容器化 | Dockerfile（非 root 用户、HEALTHCHECK、多阶段构建） |
| 编排 | docker-compose（14 服务：MySQL/Redis/PG/Nacos/MinIO/Trino/Prometheus/Grafana/Loki/Jaeger/Alertmanager/Promtail/Server/Backup 等） |
| K8s 部署 | Helm Chart（11 模板文件，不含 values）+ values-dev/prod/staging、ArgoCD GitOps、金丝雀发布 |
| CI/CD | Jenkins Pipeline（测试→安全扫描→SonarQube→构建→部署/回滚） |
| 安全 | OWASP Dependency-Check（CVSS≥7 高危阻断；需配置 NVD API Key，未配置时 CI 跳过并告警）、CycloneDX SBOM、pre-commit 密钥扫描、security-check.sh 部署前检查 |
| 质量门禁 | JaCoCo 覆盖率门禁（当前基线 30%，WMS/MES 已纳入阻断，BPM 暂排除待补齐单测；目标分阶段提升至 60%→80%）+ 7 道 Python 脚本门禁（错误码唯一性/PreAuthorize 全仓（含 WMS 严格）/电子签名/裸抛/事务原子性/迁移契约/重复建表收敛）、JUnit + Mockito 单元测试 |
| 监控 | Prometheus + Grafana（4 Dashboard）+ Loki 日志 + Jaeger 链路追踪 + AlertManager 告警 |
| 压测 | JMeter（zhicloud-load-test.jmx）+ Gatling（ZhiCloudLoadTest.scala） |
| 灾备 | 备份脚本、异地容灾、disaster-recovery-drill.sh 演练脚本 |

## 🗄️ 数据库版本管理

采用 Flyway 管理 **158 个迁移脚本（V1–V151）**，覆盖：

- V1 基线 → V6 期间结转 → V8 TOTP → V9 操作日志哈希链
- ERP：多币种/预算/合并/固定资产/总账/采购询价/VMI/CPFR/MRP/信用管理
- MES：MRP 低层/APS/OEE/BOM 替代料/SCADA/PDA/ECN/能源管理
- WMS：FEFO/SN/3PL/盘点扩展/波次/安全库存
- QMS：FMEA/8D/CAPA/SPC/MSA/培训/投诉/SQM/质量成本
- CRM：补丁/销售订单/乐观锁
- HR：合同/请假/社保/招聘
- OA：初始化/日程管理
- TMS：运费结算/GPS 定位/运费对账/车队运营
- V115–V121：生产种子账号清理、BPM businessKey 幂等登记与 claim-token ownership
- V126–V140：业务报表视图、商业域（Trade/Pay/Promotion/Member/Product）整体移除
- V141–V151：默认 OAuth2 客户端引导、V77 恢复路径与对账告警等生产就绪修复

> 迁移契约门禁：python scripts/production/check_migration_contract.py（已审核迁移的字节哈希、版本唯一性、验证 SQL 与 lint）。
> 该门禁只证明文本契约，**不替代**真实 MySQL 影子库 / 预生产执行。

## 🧪 测试覆盖

按 git ls-files '*src/test/java/*.java' 实测（**参与构建的模块**与**预留模块**分开列）：

| 模块 | 测试文件数 | 参与构建 |
|------|-----------|----------|
| IoT | 52 | 是 |
| MES | 48 | 是 |
| System | 40 | 是 |
| Framework | 37 | 是 |
| AI | 33 | 是 |
| WMS | 28 | 是 |
| Infra | 27 | 是 |
| BPM | 27 | 是 |
| ERP | 24 | 是 |
| Server | 20 | 是 |
| CRM | 14 | 是 |
| AI-RAG | 6 | 是 |
| QMS | 6 | 是 |
| OA | 5 | 是 |
| AI-MultiAgent | 5 | 是 |
| TMS | 5 | 是 |
| HR | 4 | 是 |
| Report | 2 | 进构建、未接入 server |
| IM | 21 | **否（根 pom 已注释）** |
| MP | 0 | **否（根 pom 已注释）** |
| **合计** | **404** | — |

> Trade / Pay / Promotion / Member / Product 的测试行已随 V140 商业域移除一并删除。

## 📁 项目结构

```
zhicloud/
├── zhicloud-dependencies/          # Maven BOM 依赖版本管理
├── zhicloud-framework/             # 18 个 Starter（zhicloud-common + 17 个 spring-boot-starter-*）
│   ├── zhicloud-common/            # 通用工具、POJO、枚举
│   ├── starter-web/             # Web 配置、全局异常、Swagger
│   ├── starter-security/        # 认证鉴权、RBAC、多租户
│   ├── starter-mybatis/         # MyBatis Plus、数据权限、分页
│   ├── starter-redis/           # Redisson、分布式锁、限流
│   ├── starter-mq/              # 消息队列（Redis Stream/Pub-Sub）
│   ├── starter-job/             # Quartz 定时任务
│   ├── starter-test/            # 单元测试基类（BaseDbUnitTest）
│   ├── starter-biz-tenant/      # SaaS 多租户
│   ├── starter-biz-data-permission/  # 数据权限
│   ├── starter-biz-ip/          # IP 地区库
│   ├── starter-excel/           # Excel 导入导出
│   ├── starter-monitor/         # 监控
│   ├── starter-protection/      # 限流、幂等
│   └── starter-websocket/       # WebSocket
├── zhicloud-server/                # 启动模块（配置、Flyway 脚本）
├── zhicloud-module-system/         # 系统管理
├── zhicloud-module-infra/          # 基础设施
├── zhicloud-module-bpm/            # 工作流（Flowable 8.0）
├── zhicloud-module-erp/            # ERP（70 controller，DDD 试点）
├── zhicloud-module-mes/            # MES（148 controller，8 子域）
├── zhicloud-module-wms/            # WMS（38 controller）
├── zhicloud-module-qms/            # QMS（28 controller，IATF 16949 对标）
├── zhicloud-module-crm/            # CRM（32 controller）
├── zhicloud-module-hr/             # HR（13 controller）
├── zhicloud-module-oa/             # OA（12 controller）
├── zhicloud-module-tms/            # TMS（9 controller）
├── zhicloud-module-iot/            # IoT（19 controller，设备/物模型/告警/设备消息）
├── zhicloud-module-report/         # Report（2 controller，进构建但未接入 server）
├── zhicloud-module-im/             # IM（30 controller，预留：根 pom 已注释，不参与构建）
├── zhicloud-module-mp/             # MP（12 controller，预留：根 pom 已注释，不参与构建）
├── zhicloud-module-ai/             # AI 大模型平台
├── zhicloud-module-ai-rag/         # RAG 检索增强生成
├── zhicloud-module-ai-multiagent/  # 多智能体编排
├── zhicloud-module-datalake/       # 数据湖仓（Iceberg + Trino）
├── zhicloud-ui/                    # 前端 monorepo 目录（Vue3 管理后台）
│   └── zhicloud-ui-admin-vue3/     # 管理后台前端（Vite dev 端口 80，代理后端 48080；本地覆盖见 .env.dev.local）
├── deploy/                      # ArgoCD + Helm Chart
├── k8s/                         # K8s 部署（含金丝雀）
├── script/                      # Docker/Jenkins/JMeter/Gatling/安全/灾备
├── docs/                        # OAuth2 接入指南 + 企业平台生产就绪审计台账
└── sql/                         # 多数据库初始化脚本（MySQL/Oracle/PG/达梦/金仓/openGauss）
```

## 🚀 快速启动

### 环境要求

- JDK 21（推荐 Eclipse Temurin 21.0.11+）
- MySQL 8.0+
- Redis 7+
- Maven 3.9+

### 后端启动（Docker Compose + Maven）

> 重要：`zhicloud-server` 依赖 16 个本地 Maven 模块（system/infra/bpm/crm/erp/iot/mes/wms/qms/tms/oa/hr/ai/ai-rag/ai-multiagent/datalake）。根目录只执行 `mvn clean compile` **不会**把模块安装到本地仓库；在 `zhicloud-server` 子目录单独运行前，必须先 `mvn install`，或直接运行 `start-backend.bat`（内部使用 `-am` 构建）。

```bash
# 1. 启动中间件（首次需先从模板生成 .env）
cd script/docker
cp .env.example .env          # 然后把模板里的占位密码全部换成真实强密码
docker compose up -d mysql redis postgres
cd ../..

# 2. 安装本地模块（首次或模块依赖变化后）
mvn -B -DskipTests install -T 1C

# 3. 启动后端
#    注意：.env 只被 docker compose 读取，宿主机的 mvn/java 进程**不会**自动加载它。
#    必须把同名变量导出到当前 shell，否则 application-local.yaml 会退回内置默认口令
#    （MySQL/Redis 均为 123456 / redis@2026），与第 1 步设置的强密码不一致而连接失败。
export MASTER_DATASOURCE_PASSWORD='<与 script/docker/.env 的 MYSQL_ROOT_PASSWORD 相同>'
export SLAVE_DATASOURCE_PASSWORD='<同上>'
export REDIS_PASSWORD='<与 script/docker/.env 的 REDIS_PASSWORD 相同>'
cd zhicloud-server
mvn spring-boot:run
```

> Windows CMD/PowerShell 请改用 `set MASTER_DATASOURCE_PASSWORD=...` / `$env:MASTER_DATASOURCE_PASSWORD="..."`。
> `start-backend.bat` **同样不读取 `.env`**：它继承当前 shell 的环境变量，读不到时回退到 `application-local.yaml`
> 的内置默认值（MySQL `123456`、Redis `redis@2026`）。所以要么先设好上面几个环境变量再运行它，
> 要么把 `script/docker/.env` 里的 `MYSQL_ROOT_PASSWORD` / `REDIS_PASSWORD` 保持为这两个默认值。

#### 数据库不在 `127.0.0.1:3306` 时：连 URL 一起覆盖

`application-local.yaml` 的数据源是 `${MASTER_DATASOURCE_URL:jdbc:mysql://127.0.0.1:3306/zhicloud_platform?...}`，
即 host/port/库名同样可被环境变量覆盖。**只设密码不够**——宿主机 3306 未必是你的 MySQL：

- 容器化了而发布端口不是 3306 时，用 `docker port <容器名> 3306/tcp` 查出真实端口再覆盖。
  （实测踩过：本机 3306 的监听者是 Docker Desktop 自己的 `com.docker.backend`，属于**另一个**MySQL 容器，
  连上去是 `Access denied for user 'root'@'172.17.0.1'`，而不是"服务没起来"。）
- 要指向另一个库（例如装了夹具的 `zhicloud_sandbox_v140`），也必须覆盖 URL——换库名会改默认值里的路径部分。

```bash
export MASTER_DATASOURCE_URL='jdbc:mysql://127.0.0.1:<真实端口>/<库名>?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true'
export SLAVE_DATASOURCE_URL='jdbc:mysql://127.0.0.1:<真实端口>/<库名>?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&nullCatalogMeansCurrent=true'
```

> URL 里的参数是 Connector/J 的口径，改 host/port/库名时请照抄上面整串，不要只替换片段：
> `nullCatalogMeansCurrent=true` 是 MyBatis-Plus 多租户/`selectJoinPage` 解析 catalog 的前提。

后端 API 地址：`http://localhost:48080`。该地址返回未认证 JSON 是正常行为，不是前端管理界面。
Docker Compose 的 `server` 服务会覆盖 `zhicloud.airag.enabled=true` 并提供 PostgreSQL+pgvector；
上面第 1 步只启动了 mysql/redis/postgres 三个中间件，宿主机直跑时 `local` profile 的 `zhicloud.airag.enabled` 仍为默认 `false`。

### 前端启动（Vue3 + Vite）

环境要求：Node.js >= 20.19.0，pnpm >= 8.6.0。

```bash
cd zhicloud-ui/zhicloud-ui-admin-vue3
pnpm install
pnpm dev
```

访问 `http://localhost/`。

> **首次登录必须先初始化管理员账号。** 旧文档写的 `admin / admin123` **已经不可用**：
> V115 迁移会按 `(id, username, password_hash, tenant_id)` 精确匹配并软禁用 V1.1 遗留的
> **20 个公开种子账号**（`admin`、`zhicloud`、`admin123` …），同时撤销其角色/岗位/OAuth2 会话。
> 这是 Issue #1 的根因类别（默认弱口令 = 后门），**不要**再通过导入 `demo-data/sql/**/*_demo_seed.sql` 来获得登录能力——
> 那些文件仅供演示，且已被 V115 清理。

全新库获取登录凭据（随机口令，明文只打印一次、不落盘）：

> **顺序很重要**：`system_users` 表由 Flyway V1.1 创建，所以请**先按上面第 2–3 步启动过后端一次**
> （Flyway 跑完迁移），再执行下面的初始化命令。库还不存在时脚本会失败。

```bash
pip install bcrypt            # 脚本依赖
# MYSQL_ROOT_PASSWORD 来自上一步生成的 script/docker/.env
export MYSQL_ROOT_PASSWORD='<script/docker/.env 里填的 MYSQL_ROOT_PASSWORD>'
python scripts/production/init_bootstrap_admin.py --dialect mysql \
  --host 127.0.0.1 --port 3306 --database zhicloud_platform \
  --user root --password-env MYSQL_ROOT_PASSWORD --apply \
  --mysql-client "C:/tools/mysql8/bin/mysql.exe"
```

脚本默认创建 `zhicloud_admin`（BCrypt cost=12，24 字节随机口令，禁用已知种子用户名），
**请立即保存 stdout 打印的明文口令**。之后用 `zhicloud_admin` + 该口令登录，租户选 `智云`（tenant_id=1）。

- 不想用 `--apply` 直连时，去掉 `--apply`/`--mysql-client`，用 `--out bootstrap_rendered.sql` 只渲染 SQL 再手工执行。
- K8s 部署走 `BootstrapAdminRunner`（PreSync 入口，`ensure`/`rotate`），不需要手工执行上面的命令。
- 若目标账号已存在需轮换口令，加 `--force`。
- 前端默认端口 80，Vite 将 `/admin-api` 代理到 `http://127.0.0.1:48080`；本地覆盖配置在 `.env.dev.local`。

### Windows 一键脚本

```cmd
start-backend.bat
start-frontend.bat
```

日志分别见 `server.log`、`frontend.log`。脚本使用项目相对路径，不再依赖 D 盘目录。

### 关键配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 应用端口 | 48080 |
| `spring.threads.virtual.enabled` | 虚拟线程 | true |
| `spring.flyway.enabled` | Flyway 版本管理 | true |
| `zhicloud.airag.enabled` | RAG 模块 | local=false；Docker Compose=true |
| `zhicloud.datalake.enabled` | 数据湖仓 | false |
| `management.server.port` | 管理端口 | 48090 |

## 📈 性能特性

- **虚拟线程**：JDK 21 Project Loom，`threads.virtual.enabled=true`，已修复 Druid pinning
- **连接池调优**：Druid max-active=50（配合虚拟线程）
- **优雅停机**：graceful shutdown + 30s 超时
- **SQL 防注入**：Druid wall multi-statement-allow=false
- **缓存策略**：Redis TTL 1h，LRU 淘汰，io-threads=4

## 🔐 安全加固

- TOTP 两步验证（Google Authenticator）
- 操作日志哈希链（防篡改）
- OWASP 依赖漏洞扫描（CVSS≥7 高危阻断；需配置 NVD API Key，未配置时 CI 跳过并告警）
- CycloneDX SBOM 物料清单
- pre-commit Git Hook 密钥泄露扫描
- 部署前安全检查脚本（6 项检查）
- Docker 非 root 用户运行（UID/GID 1000）
- K8s Secret 占位符 + securityContext
- QMS 电子签名（21 CFR Part 11）：8 个管控点（审批/驳回/关闭）全量覆盖，CI 门禁防回归
- 全仓 @PreAuthorize 扫描门禁：GAP 清零（0/513 Controller），CRITICAL=0 匿名可达（口径说明见下方）
- 裸抛异常统一门禁：0 处裸抛（81 处显式 @bare-throw-ignore 豁免），CI 防回归
- 事务原子性门禁：12 处多写操作补 @Transactional，CI 防回归
- 错误码唯一性门禁：140 处冲突去重，1830 个定义 0 冲突

> **@PreAuthorize 门禁口径（2026-10-10 已收敛，见 handoff §12.170）**：`scripts/check_missing_preauthorize.py` 改为**按类上是否真的标注 `@RestController`/`@Controller` 判定**（先剥离注释，避免 import/Javadoc 里的字样造成误收），不再要求路径含 `controller/`。扫描数由 512 → **513**，新增覆盖的就是此前在口径之外的 `zhicloud-module-iot/.../api/device/IoTDeviceApiImpl.java`（`/rpc-api/iot/device/**` 5 个 `@PermitAll` 写接口：auth / get / modbus config-list / register / register-sub）。
> 这 5 个接口调用方是**独立的 IoT 网关模块**（`zhicloud-module-iot-gateway`，走 HTTP 调 server），不是管理后台 RBAC 接口；每个都在业务层自校验凭证（`authDevice` 用 deviceSecret、`registerDevice` 用 productSecret 签名 + 产品须开启 `registerEnabled`、`registerSubDevices` 须网关已存在且校验 `gatewayId` 归属），因此 `@PermitAll` 属设计意图，已连同**逐条理由**登记到 `scripts/permitall-allowlist.txt`。
> 同时脚本补了**空扫 fail-closed**：`--module` 指到不存在的模块或扫到 0 个 Controller 时报错退出，不再把「0 文件」读成「全仓干净」。回归测试见 `scripts/production/tests/test_check_missing_preauthorize.py`（7 个用例）。



## 🔄 最近更新（2026-10-10）

- **代码 Review**：修复 BPM 状态回写 listener 同族 `Long.parseLong` 缺陷（HR/OA×3）、签名框架 timestamp 500、操作日志 bizNo 审计丢失、预算明细 N+1（§12.158）
- **前后端接口匹配**：扫描 2,435 前端调用 vs 2,974 后端端点；修复 IoT OTA 固件缺 DELETE 端点 + 短信渠道前导 `/`（§12.159）
- **测试基线**：hr+oa / protection+security / erp 全部通过
- **Issue #1 已闭环**（closed / completed）：Redis 空密码、前端本地启动、V77 恢复路径；修复提交 `f6b890395`，生产形状 E2E 与干净 Windows 安装证据见 `docs/enterprise-platform/evidence/production/clean-windows-bat-install-20261009/`
- **文档修正（本次）**：README 登录凭据改为 `init_bootstrap_admin.py` 随机口令流程（原 `admin/admin123` 已被 V115 软禁用）；规模数字、Flyway 版本、测试表、模块启用状态按实测重写
- **仓库清理**：移除误提交的 `zhicloud-extracted/`（806 个文件 / 517MB 解包 fat jar，迁移只到 V84）、根目录 `redis2_out.txt` 与 `runlog.txt`、`application-local.yaml` 的 IM 残留日志配置
- **完整性 review 收尾（本次）**：修复上一提交引入的**转义符号受损**（`docs/enterprise-platform/00-README.md` 与 `handoff/uncommitted-review-groups-20260928.md` 中 `\r`/`\a`/`\b`/`\0` 被当作转义序列写成真实控制字符，吞掉后一字符）；同类问题在 `evidence/production/schema-source-drift-20260928.md`（37 处）与 `script/docker/nacos/README.md`（`bash` 被写成 `\b`+`ash`）中也已修复；修复 README 三处过时数字（SQL 432→341、ERP Controller 71→70、Maven 子模块 20→22）与两处过时门禁描述（裸抛豁免 4→81、错误码定义 2051→1830）
- **遗留（不影响按文档启动）**：FMS(140) / HRM(303) 前端页面后端未开发，属规划范围（口径：前端 `url:` 唯一路径数，与全量前后端接口比对脚本一致）；IM/MP 为预留模块，不参与构建
- **完整性 review 收尾（2026-10-10，§12.169）**：删除 4 个无引用的前端 `.ts.bak` 死文件（其中一个还带已过时的 `machinerySpec` 字段）；修复 `script/docker/Docker-HOWTO.md` 三处与实际不符（「三个必填密码」实为 **11 个**、文件树里的 `zhicloud-ui-admin/` 目录不存在、「8080 = admin UI」实为 Trino）；修复 README 快速启动的**真实断点**——`.env` 只被 docker compose 读取，宿主机 `mvn spring-boot:run` / `start-backend.bat` 都不会自动加载，必须先把 `MASTER_DATASOURCE_PASSWORD` / `SLAVE_DATASOURCE_PASSWORD` / `REDIS_PASSWORD` 导出到当前 shell，否则会回退到内置默认口令（`123456` / `redis@2026`）而与容器强密码不一致
- **前后端接口一致性（全量脚本比对，2026-10-10）**：前端 2,440 条唯一路径 vs 后端 3,011 个 `(路径,方法)`；不匹配 451 条 = FMS 140 + HRM 303 + 8 条占位根路径，**除已登记的 FMS/HRM 规划缺口外无新增不一致**
- **@PreAuthorize 门禁口径收敛（2026-10-10，§12.170）**：门禁脚本从「路径含 `controller/`」改为「按类上 `@RestController`/`@Controller` 注解判定」，扫描数 512 → **513**，把此前长期在视野外的 IoT `/rpc-api` 5 个 `@PermitAll` 设备端接口纳入扫描并**逐条登记理由**；同时补 **空扫 fail-closed**（`--module` 写错时不再静默报 `[OK]`，实测修复前 `--module zhicloud-module-iot` 扫 0 个文件仍通过，修复后扫 19 个）；新增回归测试 7 个用例
- **CI fail-open 修复 + artifact 配额根因定位（2026-10-10，§12.171）**：`e41dc71a1` 的 CI 中 `Build with Maven` 与 8 道代码门禁全部 success，但两个 `Upload artifact` 步骤因**账号级存储配额**失败，**连带把第 15–25 步共 11 个步骤全部 skipped**（含 `Security Check`、`OWASP Dependency Check`、`Generate CycloneDX SBOM` 与整条许可证治理链），CI 却只表现为"上传失败"——属 **fail-open**，已给 5 个 upload 步骤加 `continue-on-error: true`（上传失败仍可见，但不再中止 job）。配额根因已用官方文档确证：**GitHub Free 的 500 MB artifact 额度是账号级、仅私有仓计入**，本仓清理 259 个陈旧 artifact（ 436.3 → 58.2 MB）后仍无法恢复，因为另一私有仓 `QAverify` 仅 6 个 artifact 就占 **3.44 GB**。该跨仓清理与本仓 retention 调整属不可逆/合规取舍，**仍 OPEN**。修复后 CI 结论已由 `failure` 转为 **success**（run `38014885897`），原被跳过的 `Security Check` / SBOM / 许可证治理链均真正执行；但同一 run 日志仍报 `Artifact storage quota has been hit`，仓库未新增任何本 run 产物，反向确证配额瓶颈不在本仓。另外 `continue-on-error` 会让“产物丢失”在结论里不可见，已补 `Artifact Upload Posture Notice` 步骤将丢失的产物名以 `::warning` + job summary 显式告警（不阻断但不隐瞒）。详见台账 §12.171。
- **artifact 配额本仓二次清理（2026-10-10，§12.172）**：按所有者指令只处理本仓——artifacts **40 → 12 个**、**58.2 → 17.5 MiB**，保留最近 3 次 run；逐 id 反查文档与脚本引用命中 0 条。当时账号级占用实测 3,706.1 MB = `QAverify` 3,442.0 + `AmazonERP` 205.9 + 本仓 58.2，**未动 QAverify**，故账号级 500 MB 额度瓶颈当时**仍 OPEN**。
- **QAverify artifact 清理（2026-10-10，§12.176）**：所有者授权「确保不影响使用即可清理」，删除 `QAverify` 全部 6 个 artifact（`qaverify-2.1.7/8/9/10/13/14-public`，共 3,442 MB）。清理后账号级私有 artifact **实际占用**降至 **17.5 MB / 500 MB**。清理前核实不影响在用版本：`QAverify-releases` 的 13 个 Release（v2.1.11–v2.1.26）与更新清单原样保留，最新安装包可下载；唯一代价是 2.1.7–2.1.10 旧包失去唯一留存。另更正 §12.172 口径：`AmazonERP` 是 **public** 仓，按 GitHub 文档 artifact 额度只对私有仓计算，**不计入**账号额度。**⚠️ 注意 GitHub 的配额计量有 6–12 小时延迟**：清理后紧接的一次 CI（run `38041727035`，同日）**仍报 `Artifact storage quota has been hit`**、产物数仍为 0——即「实际占用已降」与「配额门禁已放行」之间存在滞后，需等 GitHub 重算（每 6–12 小时）后上传才会恢复。
- **未跟踪项事实核查（2026-10-10，§12.172）**：`.blob-sha-ledger`（handoff 某版 blob SHA，已被 commit 取代、内容过期）、`.tree-sha`（空文件，无用途）、`Temp/`（763 MB：`e2e-prodlike/` 被 evidence 按路径引用**不可删**；`zc-live-server.env` 与 `zc-live-admin-password.txt` 含真实凭据，建议删除或确认已轮换）。三者**均未被 `.gitignore` 覆盖**，仅因未 `git add` 才显示为未跟踪——「按约定不入仓」是口头约定，不是 .gitignore 强制。
- **外层目录 `C:\Users\Administrator\Desktop\zhicloud`（2026-10-10，§12.172 → §12.177）**：曾是 2026-09 的审计交付外层仓（独立 git 仓库，18 提交，root `c1f5dd2f`，HEAD `01dfe6b35`，与内层无共同祖先，从未推送成功；334 tracked = 229 `script/` + 103 `docs/`，内层无等价物；其中 `Temp/win2025-clean/` 曾占 20.5 GB）。**2026-10-10 所有者判定该交付物不再需要，外层目录（`docs/`、`script/`、`Temp/`、`.tools/`、`archive/`、外层 `.git`、外层 `README.md`/`HANDOFF.md`）已整体删除**（1302 文件 / 167.7 MB），现 `Desktop\zhicloud\` 下**只剩本内层仓**；该删除**不可逆**（详见台账 §12.177）。
- **未跟踪项清理 + .gitignore 加固（2026-10-10，§12.173）**：按所有者指示删除已确认无用的本地文件，释放 **756.25 MB**：`Temp/e2e-prodlike/zhicloud-src-39fcebed.zip`(489.5 MB) 与 `zhicloud-src-only-39fcebed.zip`(28.3 MB) 实测为 `39fcebed` 提交的源码快照（抽样 `pom.xml`/`README.md`/`start-backend.bat` 与 `git show` 逐字节一致），而该提交已是远端 `master` 祖先，**可从 git 完整重建**；`downloads/`(238.4 MB) 为可由分发渠道重新下载的安装包；`Temp/zc-live-server.env` 与 `zc-live-admin-password.txt` 含真实凭据已删除（前者口令可从 `docker inspect` 重建）。另删 190 个一次性脚本/日志，`Temp/` 763 MB → 131.4 KB。**保留** `Temp/e2e-prodlike/` 的 55 个 VM 脚本与证书（131 KB）——它是干净机 E2E 的唯一可复现载体。
- **更正（2026-10-10，§12.173）**：§12.172 称「`e2e-prodlike/` 被 evidence 按路径引用」**不成立**。逐条 `git grep` 实测：`evidence/**` 对 `Temp` 只有 1 处**泛指**引用（`v151-default-oauth2-client-login-e2e-20261009.md:76`），对 `e2e-prodlike/` 下任何具体文件名命中 **0 条**。该句沿用了未经复核的上游陈述，已作废。
- **`.gitignore` 加固（2026-10-10，§12.173）**：新增 `/Temp/`，把「不入仓」从**口头约定**变成强制规则——此前三者均未被 `.gitignore` 覆盖，任何 `git add -A` 都会误纳入。`.gitignore:109` 的 `!docs/enterprise-platform/evidence/production/**/*.log` 反忽略规则逐字未动且实测继续生效。
- **状态未变**：`production_ready=false` / `gate3_passed=false` / `independent_review=false` / Go-No-Go = **No-Go**，本 README 的「可启动」不等于「生产就绪」

## 📄 开源协议

MIT License，个人与企业可 100% 免费使用。

---

**智云·企业数字化平台** — 以 AI 驱动的企业级全栈数字化解决方案
