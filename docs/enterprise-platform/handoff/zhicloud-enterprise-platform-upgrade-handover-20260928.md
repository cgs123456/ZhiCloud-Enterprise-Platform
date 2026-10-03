# 智云企业数字化中台升级与交接总结报告

## 0. 文档信息

| 项目 | 内容 |
|---|---|
| 报告日期 | 2026-09-28（2026-09-29 状态更新） |
| 仓库 | `ZhiCloud-Enterprise-Platform` |
| 当前分支 | `upgrade/production-readiness` |
| 当前 HEAD | `d0ebc96c9f864450c9c293094e114aff0e4e8588` |
| 工作区状态 | 存在大量未提交修改和未跟踪证据文件，HEAD 不能单独代表当前已验证工作树 |
| 项目方向 | 企业数字化中台，覆盖 ERP、MES、WMS、QMS、CRM、HR、OA、TMS |
| 本轮结论 | 方案基线已形成，部分生产化工程件已落地；仍不可直接生产部署 |
| 生产标记 | `production_ready=false`、`gate3_passed=false` |
| 风险标记 | `R-011`、`R-029` 仍为 `OPEN` |

> 本文是升级工作和交接工作的汇总，不是生产上线验收报告。文中的模拟数据、H2 测试、隔离单实例 MySQL 8.4.x 沙箱测试（服务端精确版本以证据为准）、静态扫描和本地 Helm 渲染，均不得表述为生产环境验证。

## 1. 一句话结论

项目已经具备企业数字化中台的业务广度和继续生产化改造的基础，但当前仍处于“设计完成、部分工程件落地、生产关键证据未闭环”的阶段。

当前可以继续开发、测试、迁移预演和沙箱验证；不可以直接导入生产数据、上线生产环境，也不可以宣称 `production_ready=true` 或 `gate3_passed=true`。

## 2. 项目范围与目标

### 2.1 当前八大业务域

| 业务域 | 目标定位 |
|---|---|
| ERP | 财务、采购、销售、库存、成本、资产、税务、预算和合并 |
| MES | 工单、排产、报工、物料、设备、质量和能源 |
| WMS | 收货、上架、拣货、复核、包装、发运、盘点和批次管理 |
| QMS | IQC/IPQC/OQC、NCR、CAPA、8D、SPC、MSA、FMEA 和电子签名 |
| CRM | 客户、线索、商机、合同、回款、服务和 CPQ |
| HR | 组织、员工、考勤、薪酬、绩效、招聘、合同和社保 |
| OA | 审批、公文、知识库、会议、日程和公告 |
| TMS | 承运商、车辆、司机、运单、GPS、运费、对账和车队经营 |

后续可建议增加 SRM、EAM、PLM、BI，以及按实际业务选择的 CDP、IoT、ESG 和合规审计能力。当前优先级应是先补齐生产底座和核心业务正确性，不应先扩展新域。

### 2.2 本报告不包含的内容

- 不再按“亚马逊 ERP”方向设计。
- 不把开源项目名义授权等同于生产合规、数据合规或供应链合规。
- 不虚构生产数据、生产 Secret、生产证书或生产集群验收结果。
- 不把模拟数据、测试数据或沙箱数据当成真实业务数据。

## 3. 已完成的工作

### 3.1 项目审计与企业级升级设计

已形成 `docs/enterprise-platform/00-14` 方案文档，主要覆盖：

- 仓库、模块、迁移、测试和部署现状核验。
- 八大业务域与企业级目标的差距分析。
- 目标架构、模块边界、租户和数据一致性原则。
- 业务模块升级路线。
- 生产就绪门禁、P0/P1 风险和退出条件。
- 数据迁移、模拟数据、测试证据和回滚方案。
- 实施波次、资源估算、风险登记和任务分解。
- GitHub/开源资料对标及限制说明。

核验基线中，各业务模块的 Controller 数量约为：

| 模块 | Controller 数量 |
|---|---:|
| ERP | 70 |
| MES | 148 |
| WMS | 38 |
| QMS | 28 |
| CRM | 32 |
| HR | 13 |
| OA | 12 |
| TMS | 9 |

这些数字用于描述功能广度，不代表各模块都具备生产级测试覆盖率、并发正确性或完整业务闭环。

### 3.2 安全、账号与客户端凭据治理

已完成的工程侧工作：

- 新增 V115，按精确条件处理生产种子账号，避免按用户名误伤同名账号。
- 从 8 套安装脚本移除 96 条客户端明文凭据 INSERT。
- 新增 V116，为 MySQL 活跃账号建立租户级唯一约束，并在 clean 沙箱执行和重跑幂等。
- 安全扫描当前有效口径为：

```text
status = passed
scanned_files = 487
finding_count = 216
P0 = 0
P1 = 216
controls_failed = 0
```

但必须保持以下边界：

- `P0=0` 只表示当前静态规则未命中阻断项。
- 历史迁移、Git 历史和已部署库中可能存在旧账号、旧 OAuth2/社交客户端 Secret。
- 真实轮换、旧值失效、外部 Secret 注入、TLS 握手、依赖 CVE 和运行时权限仍未闭环。
- 当前扫描证据记录的是提交 `d0ebc96...`，但工作树仍有未提交改动，不能直接当作已冻结版本的最终生产扫描。

### 3.3 数据迁移、Bootstrap 与沙箱验证

已完成或已有证据的工作：

- V115、V116、V117、V118 已在隔离 MySQL 8.4.x clean 沙箱执行并完成重跑幂等验证；服务端精确版本以各证据为准。V119/V120/V121 另有独立隔离 MySQL 8.4.11 沙箱/静态证据（V119 4/4；V120 首次/幂等重跑 exit=0、V120_verify 4/4、不兼容索引 fail-closed；V121 claim-token unit 4/0/0/0、真实链 fail-closed 2/0/0/0、10 并发 1/0/0/0、SQL lint 2 文件 0 违规），不属于 V115-V118 clean 全链；应用级 owner fencing 在最小 V100→V104→V107→V120 依赖链上 9/0/0/0，仅单节点、非生产；该最小依赖链不包含 V121。
- V116/V117 已覆盖隔离 MySQL 沙箱中的并发重复拒绝、软删除语义和 100,000 行合成 DDL 成本观测。
- MySQL bootstrap runner 已在沙箱验证创建、相同密码 noop、不同密码 fail-closed、受控 rotate、旧密码失效和新密码 noop。
- 只读审计确认账号、租户、角色、岗位、BCrypt 形态和活跃账号唯一索引一致。
- 本地 Helm 3/4 已完成 6 个正向渲染/lint、10 个负向 fail-closed、12 个离线 schema 校验；当前证据为 `helm-render-20260929_202001.json`（`server_side_dry_run=not_run`），旧 `helm-render-20260927_212935.json` 仅作历史追溯。
- 2026-09-30 迁移契约 canonical-byte 修复：`check_migration_contract.py` 对 verification SQL 先做 LF 规范化再计算 SHA-256，migration SQL 仍按原始字节比较（V118 迁移哈希 `f57dc75a5e731e17d3978a459d4f722b039c07d0d5d36b9f1bc1cec9652db8db` 未变更）；新增 `test_crlf_verification_matches_lf_contract`、`test_migration_hash_remains_byte_exact` 两个单测（`Ran 10 tests`、`OK`），`check_migration_contract.py --json` 为 scanned=128 / tracked=32 / findings=[] / passed，`check_commit_readiness.py --quiet` 为 PASS / required=17 / tracked=17。
- 2026-09-30 审计发现：提交 `63f99a81` 记录的 V110–V114 verification 旧哈希等于「把文件重新加 CRLF 后的哈希」，而 Git blob 实际已是 LF，属于提交当时记录的哈希与实际 blob 不自洽，不是文件内容被事后改动；V117 的实际 LF 哈希与 expected 一致，不存在 V117 不一致。该发现仅登记，未改写历史，因此不能写成“已修复”。

仍未闭环：

- 生产等价实例的 MDL、锁等待、复制延迟、磁盘空间、回滚和真实存量数据验证。
- 非 MySQL 方言的等价活跃账号唯一约束和存量 MFA 升级路径实库验证。
- K8s server-side dry-run、真实集群部署、滚动发布和回滚。
- `independent_review=false`，缺少独立复核。

### 3.4 CRM 回款与 BPM/Flowable 事务一致性

已完成的工程件：

- CRM 回款增加了应用层幂等、显式 CAS、行锁和计划原子抢占。
- 完成 V117 CRM 回款幂等迁移，并在 MySQL clean 沙箱执行和重跑幂等。
- 完成 V118 BPM 流程实例幂等登记表，唯一键为 `(tenant_id, process_definition_key, business_key)`。
- BPM DTO 重入时会返回既有流程实例，避免重复启动。
- V121 新增 `claim_token varchar(64) NULL DEFAULT NULL` 和 owner 校验：旧实现依赖 JDBC affected rows；Connector/J 默认 `useAffectedRows=false` 时，重复命中 `ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)` 返回 1，非 owner 线程会误判抢占成功。新实现重复命中不覆盖已有 token，`process_instance_id` 非空返回既有实例，token 不匹配（包括空白登记和 legacy NULL）fail-closed 为 `PROCESS_INSTANCE_IDEMPOTENCY_PENDING`。
- 2026-09-30 已用隔离 MySQL 8.4.11（`zhicloud-mysql-sandbox-18e0f782`，server 8.4.11）双会话探针实测上述 V118 路径的 REPEATABLE-READ 一致性读缺陷：隔离探针（V118 幂等登记结构 + V121 `claim_token`）实测结论：双会话 REPEATABLE-READ 下，loser 事务快照建立后的 winner 已提交行对普通一致性读不可见（COUNT(*) 始终为 0），duplicate-path upsert（`ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)`）不抛重复键异常，Connector/J `useAffectedRows=false` 时返回值也不能作为 ownership 判据；只有 `SELECT ... FOR UPDATE` 当前读返回真实 `claim_token`。证据为 `v118-rr-consistency-read-mysql-20260930_000517.json`（SHA-256 `ad4f07005878d802adeb725f8e72fb7e82f3ad5eb2d994e199b71e84f5931bdb`）与同名 `.log`（SHA-256 `d35631a440808b54c9cf5a62eb58e0b63a3bf735cda7293b804c98aeffd196f2`），均为隔离非生产证据。
- 补齐 Flowable 8.0.0 自动配置所需模块：
  - `flowable-engine-configurator`
  - `flowable-spring-configurator`
  - `flowable-app-engine-spring`
- 显式关闭未使用的 Flowable App Engine：`flowable.app.enabled=false`；该开关只关闭未使用的 App Engine，**不覆盖** Process Engine 自动配置的副作用。2026-09-30 已实测依赖树（`mvn -o -pl zhicloud-module-bpm dependency:tree "-Dincludes=org.flowable"` BUILD SUCCESS）：`flowable-spring-boot-starter-process:8.0.0:compile` → `flowable-spring:8.0.0:compile`（starter 另传递提供 autoconfigure / engine）、`flowable-spring-boot-starter-actuator:8.0.0:compile`，`flowable-engine-configurator`、`flowable-spring-configurator`、`flowable-app-engine-spring` 均为 compile 直接依赖，必须与 starter 处于同一提交边界。
- 新增并通过以下集成验证：
  - `FlowableSpringTransactionIntegrationTest`：2/2 通过，提交后历史可见，回滚后历史不存在。
  - `CrmReceivableBpmTransactionIntegrationTest`：1/1 通过，H2 本地事务回滚后 CRM 状态和 Flowable 历史均回滚。
  - 历史 MySQL 8.4.9 沙箱：`CrmReceivableBpmTransactionMysqlIntegrationTest` 1/1，回滚后关键表零残留。
  - 当前隔离 MySQL 8.4.11：`CrmReceivableBpmTransactionMysqlIntegrationTest` 2/2，已接入真实 `CrmReceivableServiceImpl → BpmProcessInstanceApiImpl → BpmProcessInstanceServiceImpl.createProcessInstance → 真实 MyBatis 幂等 Mapper → Flowable`，证明同一 Spring 事务提交一致及 BPM 返回后共同回滚。
  - 当前隔离 MySQL 8.4.11：`BpmProcessInstanceIdempotencyRealChainConcurrencyTest` 1/0/0/0，同 `businessKey` 10/10 并发请求成功并返回同一实例，仅一条幂等登记和一条 Flowable 历史；历史失败复现证据已保留。
  - 当前隔离 MySQL 8.4.11：claim-token 定向单元 `4/0/0/0`、真实链 fail-closed `2/0/0/0`，覆盖重复命中 ownership 和空白登记 fail-closed。
  - 当前隔离 MySQL 8.4.11：`CrmReceivableBpmDynamicDataSourceMysqlIntegrationTest` 3/3，`DynamicRoutingDataSource` 仅暴露 master/slave 两个显式路由，`transactionManager` 绑定动态数据源，Flowable `TransactionAwareDataSourceProxy` 指向同一动态数据源；`CONNECTION_ID()`/`DATABASE()` 验证同一 Spring 事务连接，CRM/BPM 幂等/Flowable 提交与回滚一致且 slave 无业务写入；master/slave 是同实例两个逻辑库，不是生产物理主从。
- 当前模块回归结果包括：BPM 模块 63 tests（0 失败 / 9 skipped）、CRM 模块 48 tests（0 失败 / 5 skipped）；server 模块 86 tests（0 失败 / 3 skipped）为历史证据口径。claim-token 相关 7 组运行均为 `BUILD SUCCESS`、exit 0。

必须保留的限制：

- 当前隔离证据已覆盖真实 `BpmProcessInstanceApiImpl → BpmProcessInstanceServiceImpl.createProcessInstance → 真实 MyBatis 幂等 Mapper → Flowable` 链路和同 `businessKey` 10 并发幂等；流程定义查询等非目标依赖仍为 mock。
- 隔离 MySQL 8.4.11 已验证 `DynamicRoutingDataSource` 的 master/slave 路由和事务绑定 3/3，但 master/slave 是同实例两个逻辑库；生产 `DynamicRoutingDataSource`、物理主从/真实读写分离、多租户与事务管理器的生产连接一致性仍未验证。
- 尚未完成生产等价数据库瞬时故障、网络超时、进程中断、提交前后异常和生产规模并发。
- 尚未完成生产规模 MDL/复制影响、Outbox/补偿、幂等残留清理及 CRM/Flowable 运行时与历史对账。
- claim-token 修复后仍无 lease/expiry/recovery/reconciliation；空白登记会永久阻塞，legacy NULL 不回填，软删除逻辑删除过滤可能使唯一键命中后的 select 返回 null，V121 `ADD COLUMN` 的生产规模表重建/MDL 风险未验证。
- V118 幂等登记路径的 REPEATABLE-READ 一致性读缺陷尚未修复，V121 的 `claim_token` + `FOR UPDATE` 当前读只是工程缓解；`d0ebc96c` 的定向回归（6 tests / 0 failures / 0 errors / 0 skipped、BUILD SUCCESS）不覆盖该一致性读路径，只有上述 SQL 级隔离探针验证了它。
- 因此 `R-011`、`R-029` 仍必须保持 `OPEN`。

### 3.5 模拟数据与验收准备

已完成：

- 生成 474 张表结构快照生成器和多种数据产物。
- 提供主数据、业务数据、支撑数据、SQL/JSON/manifest、清理脚本和只读不变式校验。
- 当前口径包括 118 张表、2,725 行，在本机隔离 MySQL 8.4.x 临时库（服务端精确版本以证据为准）完成导入、逐表计数、跨域关联和 cleanup 清零。
- 已生成可用于演示、测试和沙箱初始化的模拟数据。

仍未完成：

- 模拟数据尚未通过 API、页面、审批、权限、并发、幂等、财务过账和质量放行等应用层端到端验收。
- 目前只覆盖 MySQL 单方言。
- dm、HighGo、Kingbase、OpenGauss、Oracle、PostgreSQL、SQL Server 等方言仍需适配。
- 模拟数据只能进入 `demo-data/`、演示库、测试库或沙箱，禁止导入生产。

### 3.6 测试、证据与文档同步（2026-09-29 状态更新）

当前已更新或新增的证据和文档包括：

- `docs/enterprise-platform/05-生产就绪门禁清单.md`
- `docs/enterprise-platform/07-模拟数据与验收方案.md`
- `docs/enterprise-platform/08-测试与证据.md`
- `docs/enterprise-platform/10-最终Review与交付审计.md`
- `docs/enterprise-platform/12-风险登记册.md`
- `docs/enterprise-platform/13-安全基线扫描证据.md`
- `docs/enterprise-platform/14-实施任务分解与执行顺序.md`
- `docs/enterprise-platform/evidence/production/security-baseline-t000-20260929_095135.json`
- `docs/enterprise-platform/evidence/production/tenant-isolation-t000-20260929_095135.json`
- `docs/enterprise-platform/evidence/production/constraint-semantics-mysql-sandbox-t000-20260929_101404.json`
- `docs/enterprise-platform/evidence/production/migration-contract-t000-20260929_204602.log`
- `docs/enterprise-platform/evidence/production/python-full-t000-20260929_095135.log`（历史 T-000：Ran 473 tests / OK，仅作追溯）
- `docs/enterprise-platform/evidence/production/current-worktree-validation-20260929_200332/MANIFEST.md`（当前工作树验证包：Python 474 tests OK、PowerShell portability passed、preflight ready_with_warnings、backup-restore selftest passed；生产等价环境探测为外部阻塞，不是通过）
- `docs/enterprise-platform/evidence/production/mysql-history-lock-20260929_094909.json`
- `docs/enterprise-platform/evidence/production/maven-t000-20260929_095633.exit.txt`
- `docs/enterprise-platform/evidence/production/demo-data-mysql-20260928_133559.json`（历史沙箱数据级证据）
- `docs/enterprise-platform/evidence/production/bpm-idempotency-mysql-20260928_150000.json`（历史沙箱回滚证据）
- `docs/enterprise-platform/handoff/flowable-crm-bpm-transaction-20260928.md`
- `docs/enterprise-platform/evidence/production/mysql-physical-replication-sandbox-20260929_193600-965821fa/evidence.json`（当前隔离单机容器 GTID 复制近似；93/93、0 failed、exit 0；非生产）
- `docs/enterprise-platform/evidence/production/mysql-physical-replication-sandbox-20260929_165217-6c092992/SUMMARY.md`（早期 probe-only 23/23 复制近似证据；仅作追溯）

## 4. 修改前后对比

| 维度 | 修改前 | 修改后 | 仍不能说明什么 |
|---|---|---|---|
| Flowable 启动 | 8.0.0 starter 缺少配置模块，Spring 上下文启动失败 | 补齐依赖并关闭未使用 App Engine，server 可打包启动 | 不代表生产 BPM 全链已通过 |
| Flowable 事务 | 缺少直接事务边界证据 | H2、历史 MySQL 8.4.9 和当前隔离 MySQL 8.4.11 已证明 CRM/真实 BPM/Flowable 提交一致与共同回滚 | 未证明生产物理主从/真实读写分离/多租户数据源拓扑、生产故障、规模与补偿对账 |
| CRM 回款 | 并发、重复提交和余额保护不足 | 增加幂等、CAS、行锁和原子抢占，V117 沙箱执行通过 | 未完成生产等价规模的 DDL/MDL/复制及资金链路补偿/对账 |
| BPM 流程实例 | 旧实现依赖 JDBC affected rows 判定 ownership，缺少可靠 owner 校验 | V118 建立唯一索引和重入返回；V121 引入 `claim_token` ownership 和空白登记 fail-closed；隔离 MySQL 8.4.11 真实服务链、claim-token unit 4/0/0/0、fail-closed 2/0/0/0、同 businessKey 10 并发 1/0/0/0 通过 | 未证明生产数据源、生产故障、规模、lease/recovery/reconciliation 与补偿对账 |
| 安全基线 | 历史扫描曾出现 P0，账号和客户端凭据风险突出 | 清理工程件、沙箱执行、当前扫描 P0=0 | 历史 Secret、生产注入、轮换、TLS、CVE 仍未闭环 |
| 数据迁移 | 缺少 clean 沙箱证据 | V115-V118 在隔离 MySQL 8.4.x 沙箱（服务端版本以证据为准）执行、重跑幂等和部分 DDL 观测；V119/V120/V121 另在独立隔离 MySQL 8.4.11 沙箱/静态验证（V119 4/4；V120 迁移/幂等/fail-closed；V121 claim-token unit 4/0/0/0、真实链 fail-closed 2/0/0/0、10 并发 1/0/0/0、SQL lint 2 文件 0 违规）；应用级 owner fencing 9 tests / 0 failures / 0 errors / 0 skipped 仅覆盖最小依赖链单节点沙箱 | 不等于生产规模 MDL/复制/回滚验证 |
| Bootstrap | 主要依赖静态契约 | MySQL 沙箱完成 ensure/noop/fail-closed/rotate 和只读审计 | 其余 7 套数据库、独立复核和生产目标库未完成 |
| 模拟数据 | 缺少可重复生成、清理和计数验证 | 已生成并完成 MySQL 单方言数据级验收 | 未完成应用层六条主流程验收 |
| 交付文档 | 缺少统一生产门禁和任务边界 | 00-14 方案、风险、证据和任务分解已形成 | 文档完成不等于生产门禁完成 |

## 5. 当前生产门禁状态

| 门禁 | 当前状态 | 结论 |
|---|---|---|
| G0 代码与构建 | 部分通过 | 当前工作树 Maven/Python 和专项测试通过，尚缺冻结提交和独立复核 |
| G1 安全 | 部分通过 | 当前静态 P0=0；真实 Secret、历史凭据轮换、TLS、MFA、依赖 CVE 未闭环 |
| G2 数据正确性 | 未通过 | 库存单一真值、QMS 门禁、MES 状态机、WMS 闭环、ERP 过账等未完成 |
| G3 跨域集成 | 部分通过 | BPM 本地/单实例事务有证据；Outbox、DLQ、补偿、生产物理主从/真实读写分离/多租户数据源路由未闭环 |
| G4 部署 | 未通过 | 本地 Helm 通过；K8s server-side dry-run 和真实集群部署未完成 |
| G5 可观测性 | 未通过 | SLO、指标、告警、追踪和 on-call 演练未完成 |
| G6 合规 | 未通过 | 等保、个人信息保护、电子签名和审计证据未形成 |
| G7 运营 | 未通过 | 备份、PITR、灾备、RTO/RPO、容量和长稳未验证 |
| G8 业务验收 | 未通过 | 六条企业主流程尚未完成应用层端到端验收 |

当前总状态：

```text
production_ready = false
gate3_passed   = false
independent_review = false
R-011          = OPEN
R-029          = OPEN
Go/No-Go       = No-Go
```

迁移契约门禁通过（`scanned=128 / tracked=32 / findings=[]`）只证明工作树文件与 manifest 中 canonical LF 字节的 SHA-256 一致，**不等于 MySQL 实际执行成功、不等于生产迁移已经完成、也不等于 Gate3 通过**。

## 6. 当前主要风险和未完成项

1. 生产物理拓扑和事务边界未证明：真实 BPM 服务链、10 并发幂等和动态数据源路由/事务绑定 3/3 已在隔离 MySQL 8.4.11 通过，但生产物理主从/真实读写分离、多租户和 Flowable 物理连接一致性未验证；隔离 master/slave 只是同实例两个逻辑库。
2. 生产故障注入不足：Flowable 成功后本地失败已在隔离链路覆盖，但网络超时、数据库瞬时故障、进程中断、提交边界异常和生产规模并发未覆盖。
3. 补偿和对账不足：Outbox/Inbox、DLQ、异常流程回收、幂等残留清理和 CRM/Flowable 跨域对账未闭环；claim-token 修复后空白登记仍会永久阻塞，且无 lease/expiry/recovery/reconciliation。
4. 生产迁移规模未证明：生产规模 MDL、复制延迟、锁等待、磁盘、回滚和真实存量库未验证。
5. 非 MySQL 方言未闭环：活跃账号唯一约束、MFA schema、幂等约束和实库语法仍需验证。
6. 安全底座未闭环：外部 Secret 注入、历史 Secret 轮换、真实 TLS、依赖 CVE、运行时权限和供应链治理未完成。
7. 部署与运营未闭环：K8s 服务端 dry-run、生产发布、回滚、备份恢复、灾备、SLO、告警和压测未完成。
8. 核心业务正确性未闭环：库存单一真值、QMS 门禁、MES 超产/超发料、WMS 旁路和 ERP 财务过账仍待修复。
9. 工作区未冻结：存在大量未提交修改和未跟踪证据，交接前需要分组 review、提交并做独立复核。
10. claim-token 残余：V121 不回填 legacy NULL，软删除逻辑删除过滤可能造成 select null/`IllegalStateException`，大表 `ADD COLUMN` 的 MDL/表重建影响未验证。
11. V118 REPEATABLE-READ 一致性读缺陷：loser 事务快照内看不到 winner 已提交行、duplicate-path upsert 不抛异常，`claim_token` + `FOR UPDATE` 当前读只是缓解；仍需人工决定接受并发约束还是补 lease/expiry/recovery/reconciliation，且生产等价环境的真实并发与锁等待未验证。

## 7. 后续工作方向

### 第一优先级：在生产等价环境完成 BPM 数据源、故障、规模与补偿验证

已覆盖链路：

```text
CrmReceivableServiceImpl.submitReceivable
→ BpmProcessInstanceApiImpl
→ BpmProcessInstanceServiceImpl.createProcessInstance
→ BpmProcessInstanceIdempotencyMapper
→ Flowable RuntimeService
```

隔离 MySQL 8.4.11 已验证事务回滚、共同提交、同 `businessKey` 10 并发幂等，以及动态数据源显式 master/slave 路由、`transactionManager` 绑定和 Flowable 同一数据源 3/3。另有隔离单机容器 GTID 复制近似 93/93，但仅是同一 Docker host 的两个容器，不是生产物理主从，也不证明真实读写分离、多租户拓扑、HA/failover 或故障切换。剩余验证点：

- 生产物理主从/真实读写分离/多租户拓扑下是否使用同一物理连接和同一事务管理器。
- 网络超时、数据库瞬时故障、进程中断和提交边界异常下是否保持一致。
- 生产规模并发、锁等待、MDL/复制影响和恢复能力。
- Outbox/补偿、对账和异常流程安全回收。

### 第二优先级：生产等价数据与故障验证

- 在生产等价 MySQL 实例验证 V116-V121 的并发、软删除、重跑、MDL、复制和回滚；V121 还需验证 `ADD COLUMN` 的表重建/MDL/磁盘/复制影响、空白登记/legacy NULL 回收、逻辑删除语义和 lease/expiry/recovery/reconciliation。
- 为非 MySQL 方言补齐并实库验证等价约束。
- 完成同 businessKey 并发、数据库瞬时故障、提交前后异常、网络超时和进程中断测试。
- 完成 Outbox/Inbox、DLQ、重放、补偿和对账演练。

### 第三优先级：安全与部署底座

- 外部 Secret 注入、对象存在性、RBAC、权限、挂载和轮换验证。
- 历史账号和客户端 Secret 的轮换、禁用和旧值失效。
- MySQL/PostgreSQL/Redis/消息/对象存储的真实 TLS 握手、证书链和主机名验证。
- 依赖 CVE、SBOM、许可证、镜像和构建链治理闭环。
- K8s server-side dry-run、预发部署、迁移 Job、滚动发布和回滚演练。

### 第四优先级：核心业务正确性

- ERP 财务过账、冲销、期间关闭和对账。
- QMS 质量门禁接入收货、流转和发运。
- MES 工单状态机、超产和超发料保护。
- WMS 流程防旁路、库存流水和盘点可用量。
- CRM 回款、服务、合同和 CPQ 的完整业务闭环。

### 第五优先级：运营、合规与最终验收

- 压测、长稳、容量、故障注入和降级预案。
- SLO、指标、日志、追踪、告警和 on-call。
- 备份、PITR、恢复演练和灾备 RTO/RPO。
- 等保、个人信息保护、行业合规和审计证据。
- 六条企业主流程应用层端到端验收。
- 独立复核完成后，再考虑 Go/No-Go 评审。

## 8. 接手交接清单

接手人应先做：

1. 核对当前分支、HEAD、未提交改动和未跟踪证据，避免把旧提交结果当成当前代码结果。
2. 先阅读 `00-README.md`、`05`、`08`、`10`、`12`、`13`、`14`，再按本报告顺序处理。
3. 将当前工作区改动按领域分组 review，至少分为：安全迁移、CRM/BPM、模拟数据、迁移 runner、文档证据。
4. 在完成分组提交和独立复核前，不改变 `production_ready=false`、`gate3_passed=false`、`R-011=OPEN`、`R-029=OPEN`。
5. 将沙箱或集成测试结果标记为对应环境，不得改写成生产验收。
6. 下一步不要把工作重点继续扩大为新的 H2 用例；隔离真实 BPM 服务链、10 并发幂等和动态数据源路由/事务绑定 3/3 已完成，应优先做生产物理主从/真实读写分离/多租户拓扑、故障/补偿/对账和规模验证。

## 9. 当前证据入口

| 证据/文档 | 用途 |
|---|---|
| `docs/enterprise-platform/05-生产就绪门禁清单.md` | 查看 G0-G8 门禁和退出条件 |
| `docs/enterprise-platform/08-测试与证据.md` | 查看测试口径、证据分类和缺口 |
| `docs/enterprise-platform/10-最终Review与交付审计.md` | 查看最终 No-Go 审计结论 |
| `docs/enterprise-platform/12-风险登记册.md` | 查看 R-001 至 R-031 风险和状态 |
| `docs/enterprise-platform/14-实施任务分解与执行顺序.md` | 查看实施波次和下一批任务 |
| `.../evidence/production/security-baseline-t000-20260929_095135.json` | 当前静态安全扫描证据（487 files / P0=0 / P1=216；commit=`d0ebc96...`） |
| `.../evidence/production/tenant-isolation-t000-20260929_095135.json` | 当前租户隔离静态证据（331 DDL 表 / 298 含 `tenant_id`；不是生产 `information_schema`） |
| `.../evidence/production/constraint-semantics-mysql-sandbox-t000-20260929_101404.json` | 当前隔离 MySQL 8.4.x 约束语义证据（V116/V117 5/5；非生产） |
| `.../evidence/production/migration-contract-claim-token-20260929.json` | 当前迁移契约复核（scanned=128 / tracked=32 / findings=0；SHA-256 `8c8e9a8c38e9b13160f819c0cf408351d29142d2438204768b9a654498541c99`） |
| `.../evidence/production/migration-contract-t000-20260929_204602.log` | 历史迁移契约复核（scanned=127 / tracked=31 / findings=0；SHA-256 `29BB17701B462B281D4BCC5610DD4F2A2CB66C86A373E49990EDCF22B54F1B7D`），仅作追溯 |
| `.../evidence/production/inventory-tenant-isolation-fix-20260929_160657/v119-mysql-evidence.json` | V119 在隔离 MySQL 8.4.11 沙箱 4/4 通过（唯一约束、租户隔离、重复数据 fail-closed）；非生产 |
| `.../evidence/production/v120-mysql-sandbox-20260929_185543.json` | V120 在隔离 MySQL 8.4.11 沙箱首次/幂等重跑 exit=0、V120_verify 4/4、不兼容索引 fail-closed；其内部 owner fencing 结论早于后续 owner fencing 证据，且该文件明确 `production_validation=false` |
| `.../evidence/production/event-owner-fencing-mysql-20260929_191149.json` | 应用级 Outbox/Inbox owner fencing 在最小 V100→V104→V107→V120 依赖链上 9 tests / 0 failures / 0 errors / 0 skipped；仅单节点沙箱、非生产 |
| `.../evidence/production/current-worktree-validation-20260929_200332/MANIFEST.md` | 当前工作树验证包：Python 474 tests OK、PowerShell portability passed、preflight ready_with_warnings、backup-restore selftest passed；生产等价环境探测为外部阻塞 |
| `.../evidence/production/python-full-t000-20260929_095135.log` | 前一版 T-000 Python 全量门禁（Ran 473 tests / OK，skipped=3），仅作追溯 |
| `.../evidence/production/mysql-history-lock-20260929_094909.json` | 隔离 MySQL 8.4.11 的 Flyway history 锁验证；非生产 |
| `.../evidence/production/maven-t000-20260929_095633.exit.txt` | 定向 Maven 测试与打包 exit=0；不是全量 Java 测试 |
| `.../evidence/production/demo-data-mysql-20260928_133559.json` | 历史 MySQL 沙箱模拟数据数据级验收；不是应用层端到端验收 |
| `.../evidence/production/bpm-idempotency-mysql-20260928_150000.json` | 历史 V118/Flowable/CRM 回滚沙箱证据（历史口径） |
| `.../evidence/production/crm-bpm-real-chain-mysql-20260929_124704.json` | 当前隔离 MySQL 8.4.11 的 CRM→真实 BPM→Flowable 事务链 2/2；非生产 |
| `.../evidence/production/bpm-idempotency-real-chain-rerun-20260929_124758.json` | 历史真实 BPM 并发幂等复跑 1/1（10/10 成功），仅作追溯；非生产 |
| `.../evidence/production/bpm-idempotency-real-chain-claim-token-20260929_215500.json` | 当前 claim-token ownership/fail-closed 汇总证据；unit 4/0/0/0、真实链 fail-closed 2/0/0/0、10 并发 1/0/0/0；SHA-256 `8fc10375e4118c3f2310abc7acb9c6b1f3f92e83996628aac22918e0899437fa`；非生产 |
| `.../evidence/production/v121-sql-lint-20260929.json` | V121 迁移/验证 SQL lint 2 文件、0 违规；SHA-256 `84c925aef1fc77705de38a7ccc6d8d2eebd933410c35440edeb3b3f4df18f8ca` |
| `.../evidence/production/v118-rr-consistency-read-mysql-20260930_000517.json` | 当前隔离 MySQL 8.4.11（server 8.4.11）双会话 REPEATABLE-READ 一致性读缺陷探针，6 步；SHA-256 `ad4f07005878d802adeb725f8e72fb7e82f3ad5eb2d994e199b71e84f5931bdb`；非生产 |
| `.../evidence/production/v118-rr-consistency-read-mysql-20260930_000517.log` | 上条探针的执行日志；SHA-256 `d35631a440808b54c9cf5a62eb58e0b63a3bf735cda7293b804c98aeffd196f2`；非生产 |
| `.../evidence/production/crm-bpm-dynamic-datasource-mysql-20260929_141551.json` | 历史隔离 MySQL 8.4.11 动态数据源路由/事务绑定 3/3，仅作追溯；非生产物理主从 |
| `.../evidence/production/crm-bpm-dynamic-datasource-mysql-20260929_214517.json` | 当前动态数据源路由/事务绑定 3/0/0/0；master 45 张 Flowable 表人工复制、`ACT_GE_PROPERTY` 7 行、slave 0 张表；连接一致性由 `CONNECTION_ID()`/`DATABASE()` 和提交/回滚验证，不是网络抓包；SHA-256 `f1cb55420c93eb26a0e2693cc6e861ec22cbe5e1fccc1e35fdd67d79772b84a5`；非生产 |
| `.../evidence/production/mysql-physical-replication-sandbox-20260929_193600-965821fa/evidence.json` | 当前隔离单机容器 MySQL 8.4 GTID 复制近似；93/93、0 failed、exit 0；含最小 V100→V104→V107→V120、Outbox/Inbox 复制、停源/单向提升和直接 SQL owner fencing 37/37；非生产 |
| `.../evidence/production/mysql-physical-replication-sandbox-20260929_165217-6c092992/SUMMARY.md` | 早期 probe-only 23/23 复制近似证据，仅作追溯；非生产 |
| `.../evidence/production/bpm-idempotency-real-chain-20260929_121828.json` | 历史失败复现证据，必须保留 |
| `.../evidence/production/maven-test-bpm-module-claim-token-20260929.log` | 当前 BPM 模块 63/0/0/9；SHA-256 `91b7c275be953e16f338fe8da54c201256937278a34c1d7a9820062ed921fba0` |
| `.../evidence/production/maven-test-crm-module-claim-token-20260929.log` | 当前 CRM 模块 48/0/0/5；SHA-256 `cbbc77c6f3210bd72d045def4d05f002e28590e8d83aa78f8d85a41f5b412f1d` |
| `.../evidence/production/maven-test-bpm-module-20260929_124931.log` | 历史 BPM 模块 61/0/0/7，仅作追溯 |
| `.../evidence/production/maven-test-crm-module-20260929_125103.log` | 历史 CRM 模块 45/0/0/2，仅作追溯 |
| `.../handoff/flowable-crm-bpm-transaction-20260928.md` | Flowable/CRM/BPM 专项交接说明 |

## 10. 最终交接结论

当前交付物可以作为后续生产化改造的审计基线和任务基线，但不能作为生产上线包。

下一步真正的关键路径是：生产物理主从/真实读写分离/多租户数据源拓扑和事务、生产故障/补偿/对账、生产规模迁移，以及安全与部署底座。隔离真实 BPM 服务链、并发幂等和动态数据源路由/事务绑定证据不能替代上述生产验证；2026-09-30 新增的迁移契约 canonical-byte 修复、`Ran 10 tests OK`、契约 `scanned=128 / tracked=32 / findings=[]` 和 V118 REPEATABLE-READ 缺陷隔离证据同样不能替代上述生产验证。在上述证据闭环之前，任何“生产可部署”“安全已闭环”“迁移契约通过即迁移成功”或“Gate3 已通过”的表述都不成立。本轮仅推进隔离单机容器近似切片；V121 claim-token unit 4/0/0/0、真实链 fail-closed 2/0/0/0、10 并发 1/0/0/0 和 7 组 `BUILD SUCCESS`，以及应用级 owner fencing 9/0/0/0、复制近似 93/93、停源/单向提升和直接 SQL owner fencing 37/37 均不能替代生产等价验证。

## 11. 生产等价环境外部阻塞清单（2026-09-30）

本节把“生产等价环境验证”的当前阻塞显式登记为外部依赖，供接手人向环境、凭据、窗口和责任人推进。它不是通过项，也不是免责声明：在下列条目闭环前，任何生产等价验证（物理主从/真实读写分离/多租户物理拓扑、生产故障/补偿/对账、生产规模迁移、HA/故障切换、安全与部署底座）都无法在本工作区执行。

| 阻塞项 | 当前实测状态 | 需要的输入 | 责任角色（待指派） | 窗口/预算 |
|---|---|---|---|---|
| Kubernetes 集群（预生产或生产等价） | 无 kubeconfig；`kubectl current-context` 未设置；`kubectl cluster-info` 连接 `http://localhost:8080` 被拒绝 | 可用 kubeconfig + 目标集群 context + 命名空间/权限 | 平台/运维（未指定） | 未提供；需变更窗口 |
| 部署工具链 | `helm`、`kind`、`minikube`、`k3d` 均 MISSING | 安装或改由 CI 侧执行 Helm 渲染/部署 | 平台/运维（未指定） | 未提供 |
| 目标数据库与凭据 | 无预生产/生产 MySQL 地址、账号与放通；本地仅 Docker MySQL 沙箱 | 目标库地址、最小权限账号、网络放通、脱敏策略 | DBA/运维（未指定） | 未提供；需迁移窗口 |
| 数据库/缓存客户端 | `psql`、`sqlcmd`、`sqlplus`、`mongosh`、`redis-cli` 均 MISSING；本地无 MySQL 客户端 | 客户端或可执行 SQL 的作业通道 | DBA/运维（未指定） | 未提供 |
| Secret Manager | `vault` MISSING；无外部 Secret Manager 接入 | Secret 后端地址、鉴权与读取策略 | 安全/平台（未指定） | 未提供 |
| 生产 TLS 证书 | 未提供 | 证书与私钥来源、轮换机制 | 安全/平台（未指定） | 未提供 |
| 真实读写分离/物理主从/多租户物理拓扑 | 仅有单机双容器 GTID 近似（93/93）与同实例两逻辑库（3/3） | 生产拓扑访问或登记在册的等价环境 | DBA/平台（未指定） | 未提供 |
| 故障注入/补偿/对账/规模 | 无对应环境、无生产规模数据 | 故障注入通道、生产规模或合规脱敏等价数据 | 业务/DBA/平台（未指定） | 未提供；需演练窗口 |

证据锚点：`docs/enterprise-platform/evidence/production/current-worktree-validation-20260929_200332/production-environment-probe.txt`（SHA-256 `40DE1493C7CCA160F35B062FD7D9DDD9CBE9E939514DBFD1C505C6F02DD3A90D`），以及同目录 `MANIFEST.md`、`preflight.json`、`powershell-portability.json`、`python-full.log`。本地工具链仅 `java`、`mvn`、`docker` 可用（`kubectl` 存在但无集群）。

边界与风险：本清单只登记“当前工作区无法获取生产等价环境”这一事实；“责任角色/窗口未指定”本身即风险——若无人认领，G0/迁移/HA/安全等门禁将长期停留在隔离单机沙箱证据上，Go/No-Go 不会自动解除。不得以 H2、单机沙箱或隔离 MySQL 结果替代上述环境，也不得把本清单写成已解决。