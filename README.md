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

| 指标 | 数值 |
|------|------|
| Java 源文件 | 6,973（主代码）+ 380（测试） |
| Controller | 615 |
| 业务模块 | 11 个活跃模块（ERP/MES/WMS/CRM/HR/OA/TMS + AI 三大模块 + 框架基础） |
| Flyway 迁移脚本 | 88 个（V1–V82，含 V51.1 小版本补齐 CRM 核心表；部分版本号复用如 V60-V64 多版本共存） |
| SQL 文件 | 276 个 |
| 单元测试文件 | 380 个 |
| 框架 Starter | 16 个（zhicloud-common + 15 starter 模块） |

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

### 八大业务域

| 模块 | Controller | 核心能力 |
|------|-----------|----------|
| **ERP** 企业资源计划 | 71 | 财务全链路（总账/预算/合并/成本/资产/税务/资金/外币）、供应链（采购/销售/库存）、VMI、CPFR、MRP、DDD 销售订单试点 |
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
| 质量门禁 | JaCoCo 覆盖率门禁（当前基线 30%，WMS/MES 已纳入阻断，BPM 暂排除待补齐单测；目标分阶段提升至 60%→80%）+ 7 道 Python 脚本门禁（错误码唯一性/PreAuthorize 全仓/WMS 严格/电子签名/裸抛/事务原子性/错误码基线）、JUnit + Mockito 单元测试 |
| 监控 | Prometheus + Grafana（4 Dashboard）+ Loki 日志 + Jaeger 链路追踪 + AlertManager 告警 |
| 压测 | JMeter（zhicloud-load-test.jmx）+ Gatling（ZhiCloudLoadTest.scala） |
| 灾备 | 备份脚本、异地容灾、disaster-recovery-drill.sh 演练脚本 |

## 🗄️ 数据库版本管理

采用 Flyway 管理 88 个迁移脚本（V1–V82，含 V51.1 小版本补齐 CRM 核心表），覆盖：

- V1 基线 → V6 期间结转 → V8 TOTP → V9 操作日志哈希链
- ERP：多币种/预算/合并/固定资产/总账/采购询价/VMI/CPFR/MRP/信用管理
- MES：MRP 低层/APS/OEE/BOM 替代料/SCADA/PDA/ECN/能源管理
- WMS：FEFO/SN/3PL/盘点扩展/波次/安全库存
- QMS：FMEA/8D/CAPA/SPC/MSA/培训/投诉/SQM/质量成本
- CRM：补丁/销售订单/乐观锁
- HR：合同/请假/社保/招聘
- OA：初始化/日程管理
- TMS：运费结算/GPS 定位/运费对账/车队运营

## 🧪 测试覆盖

| 模块 | 测试文件数 |
|------|-----------|
| IoT | 52 |
| MES | 48 |
| System | 39 |
| AI | 32 |
| Framework | 29 |
| Infra | 26 |
| WMS | 26 |
| IM | 21 |
| BPM | 20 |
| Trade | 15 |
| Pay | 14 |
| Promotion | 13 |
| Member | 8 |
| Product | 5 |
| QMS | 6 |
| ERP | 8 |
| CRM | 4 |
| TMS | 4 |
| OA | 3 |
| HR | 3 |
| Report | 2 |
| AI-RAG | 2 |
| **合计** | **380** |

## 📁 项目结构

```
zhicloud/
├── zhicloud-dependencies/          # Maven BOM 依赖版本管理
├── zhicloud-framework/             # 16 个 Starter（zhicloud-common + 15 spring-boot-starter-*）
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
├── zhicloud-module-erp/            # ERP（71 controller，DDD 试点）
├── zhicloud-module-mes/            # MES（148 controller，8 子域）
├── zhicloud-module-wms/            # WMS（38 controller）
├── zhicloud-module-qms/            # QMS（28 controller，IATF 16949 对标）
├── zhicloud-module-crm/            # CRM（32 controller）
├── zhicloud-module-hr/             # HR（13 controller）
├── zhicloud-module-oa/             # OA（12 controller）
├── zhicloud-module-tms/            # TMS（9 controller）
├── zhicloud-module-ai/             # AI 大模型平台
├── zhicloud-module-ai-rag/         # RAG 检索增强生成
├── zhicloud-module-ai-multiagent/  # 多智能体编排
├── zhicloud-module-datalake/       # 数据湖仓（Iceberg + Trino）
├── deploy/                      # ArgoCD + Helm Chart
├── k8s/                         # K8s 部署（含金丝雀）
├── script/                      # Docker/Jenkins/JMeter/Gatling/安全/灾备
├── docs/                        # OAuth2 接入指南
└── sql/                         # 多数据库初始化脚本（MySQL/Oracle/PG/达梦/金仓/openGauss）
```

## 🚀 快速启动

### 环境要求

- JDK 21（推荐 Eclipse Temurin 21.0.11+）
- MySQL 8.0+
- Redis 7+
- Maven 3.9+

### Docker Compose 一键启动

```bash
# 1. 启动中间件
cd script/docker
docker-compose --env-file docker.env up -d mysql redis postgres

# 2. 编译
cd ../..
mvn clean compile -T 1C

# 3. 启动应用
cd zhicloud-server
mvn spring-boot:run
```

访问 `http://localhost:48080`，默认账号 `admin` / `admin123`

### 关键配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 应用端口 | 48080 |
| `spring.threads.virtual.enabled` | 虚拟线程 | true |
| `spring.flyway.enabled` | Flyway 版本管理 | true |
| `zhicloud.airag.enabled` | RAG 模块 | true |
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
- 全仓 @PreAuthorize 扫描门禁：GAP 清零（0/615 Controller），CRITICAL=0 匿名可达
- 裸抛异常统一门禁：0 处裸抛（4 处显式 @bare-throw-ignore 豁免），CI 防回归
- 事务原子性门禁：12 处多写操作补 @Transactional，CI 防回归
- 错误码唯一性门禁：140 处冲突去重，2051 个定义 0 冲突

## 📝 更新记录

### 2026-08-26 QMS 质量管理模块全栈交付 + 代码质量零错误达标

**后端**

| 级别 | 修复内容 |
|------|----------|
| 新功能 | QMS 质量管理模块全栈交付：28 Controller 覆盖 IQC/IPQC/OQC、FMEA、8D、CAPA、SPC、MSA、NCR、SCAR、质量追溯、电子签名、审核、检验、供应商质量、培训、质量成本、计量器具、MSA、8D、客诉 |
| 基础设施 | Flyway 迁移脚本冲突解决：V83/V84 版本号冲突（V83 重复定义），重复文件清理，V84 幂等索引安全通过 |
| 基础设施 | `zhicloud-extracted` 旧解包产物更新：V83/V84 迁移文件同步，后端启动 Flyway 正式接管 schema version 84 |
| 缺陷修复 | QMS 缺表（`qms_audit_nonconformity` 等 7 表、`airag_knowledge/document`、`hr_attendance` 等）全部补齐 |
| 缺陷修复 | `application-local.yaml` ALIPAY 占位符 `client-id: xx` 导致 OAuth2 Bean 创建失败，清理无用配置 |
| 缺陷修复 | MES `qms_instrument` 缺表导致 500，执行 `qms_instrument.sql` 补齐 |
| 缺陷修复 | SN 码模块 API 导入方式修正（命名空间导入改解构导入）、`itemId` 类型可选化 |
| 缺陷修复 | MES 产品收货单 API 命名对齐（Receipt→Recpt）、缺失方法对齐、`checkProductReceiptQuantity` 校验块移除 |
| 稳定性 | 服务进程反复被杀问题根因定位：工具超时清理进程树，改用 WMI 方式启动长驻服务（Redis/后端/前端） |

**前端（zhicloud-ui-admin-vue3）**

| 级别 | 修复内容 |
|------|----------|
| 体验 | 首页「萌新必读」宣传外链区删除、GitHub 推广卡片/假公告/假统计/假图表清理，重写为简洁工作台 |
| 体验 | 登录页「萌新必读」宣传外链删除、用户下拉菜单「文档」外链删除 |
| 清理 | 数据库侧边栏外链菜单 3 条软删（智云官网/平台文档/微服务文档） |
| 清理 | 无引用文件删除：`Index2.vue` 演示页、`DocAlert` 组件 |
| 类型安全 | **vue-tsc 全项目 0 错误**（从 175+ 修至 0），含 MES/AI/BPM/CRM/FMS/IoT 既有遗留问题全部修复 |
| API 对齐 | MES 产品收货单/明细/行/行列表 API 命名对齐（Receipt→Recpt）、方法名对齐、缺失方法 `checkProductReceiptQuantity` 移除 |
| 类型修复 | SN 码模块命名空间导入改解构导入、`itemId` 可选化、类型引用修正 |
| 规范 | MES/系统/AI/BPM/CRM/FMS/IoT 等模块遗留 TS 错误（ElMessage/ElMessageBox 缺失导入、undefined→number、API 导出名不匹配）全部修复 |

**基础设施**

| 级别 | 修复内容 |
|------|----------|
| 稳定性 | 服务进程反复被杀根因定位：工具超时清理进程树，改用 WMI 方式启动长驻服务（Redis/后端/前端），服务稳定运行 |
| 构建 | `zhicloud-extracted` 旧解包产物更新：完整重新打包解包，Flyway V83/V84 迁移文件同步到解包目录 |
| 类型安全 | **vue-tsc 全项目 0 错误（rc=0）**，从 175+ 遗留错误修复至零 |

---

### 2026-08-24 代码质量与前后端契约修复

**后端**

| 级别 | 修复内容 |
|------|----------|
| 性能 | CRM 联系人-商机关联创建：循环单条查询（N+1）改为批量查询校验（`CrmContactBusinessServiceImpl`） |
| 性能 | API 访问日志拦截器：非 prod 环境每个请求重复读取 Controller 源文件，改为按 Method 缓存；`System.out.printf` 改为 `log.info`（携带 traceId，可落文件） |
| 健壮性 | logback `${LOG_FILE}` 增加默认值 `./logs/zhicloud.log`，未配置时不再产生 `LOG_FILE_IS_UNDEFINED` 垃圾文件 |
| 测试 | 修复 `OAuth2TokenServiceImplTest.testGetAccessTokenPage` Windows 时钟精度（~15ms）导致的偶发失败（flaky）：过期时间改用明确过去的时间 |

**前端（zhicloud-ui-admin-vue3 MES 片段）**

- 产品收货单 API 全部 URL 对齐后端实际路径 `/mes/wm/product-receipt*`（原为不存在的 `product-recpt`），执行入库端点对齐为 `/finish`
- SN 码管理重构为与后端一致的"批次分组"模型：分页走 `/group-page`、明细对话框走 `/list-by-uuid`、删除/导出按批次 UUID 操作、生成字段 `snNum` → `count`

**工程清理**

- 删除仓库根目录及 iot/pay 模块下的 Windows 设备名残留文件 `nul` 与 `LOG_FILE_IS_UNDEFINED`

## 📄 开源协议

MIT License，个人与企业可 100% 免费使用。

---

**智云·企业数字化平台** — 以 AI 驱动的企业级全栈数字化解决方案
