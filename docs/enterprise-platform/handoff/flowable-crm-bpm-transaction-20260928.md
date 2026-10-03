# Flowable / CRM / BPM 事务一致性工作交接报告

## 1. 基本信息

| 项目 | 内容 |
|---|---|
| 项目方向 | 企业数字化中台，覆盖 ERP / MES / WMS / QMS / CRM / HR / OA / TMS |
| 当前分支 | `upgrade/production-readiness` |
| 报告日期 | 2026-09-28（2026-09-29 状态更新） |
| 当前阶段 | 生产化改造中的本地事务一致性验证 |
| 生产状态 | `production_ready=false`、`gate3_passed=false` |
| 风险状态 | `R-011`、`R-029` 仍为 `OPEN` |
| 当前结论 | 已验证 H2、历史 MySQL 8.4.9（历史沙箱版本）单实例 InnoDB 本地事务边界，以及隔离 MySQL 8.4.11 上的 CRM→真实 BPM→Flowable 事务链、同 `businessKey` 10 并发幂等、动态数据源路由/事务绑定 3/3 和 V121 claim-token ownership/fail-closed；生产物理主从/真实读写分离/多租户拓扑、生产故障、规模、补偿和对账仍未验证，不等于生产可部署 |

## 2. 本阶段完成内容

### 2.1 补齐 Flowable 8.0.0 自动配置依赖

**修改前：**

- `flowable-spring-boot-starter-process` 在 Flowable 8.0.0 下无法正常完成 Spring 自动配置。
- Spring 上下文启动时报：

```text
ClassNotFoundException: org.flowable.engine.configurator.ProcessEngineConfigurator
```

**修改后：**

- 显式补齐 Flowable 8.0.0 自动配置直接引用、但 starter 未传递的可选模块：

```text
org.flowable:flowable-engine-configurator
org.flowable:flowable-spring-configurator
org.flowable:flowable-app-engine-spring
```

已实测核验依赖树（`mvn -o -pl zhicloud-module-bpm dependency:tree "-Dincludes=org.flowable"`，BUILD SUCCESS）：`flowable-spring-boot-starter-process:8.0.0:compile` → `flowable-spring:8.0.0:compile`，starter 另传递提供 autoconfigure / engine；`flowable-spring-boot-starter-actuator:8.0.0:compile`；`flowable-engine-configurator:8.0.0:compile`、`flowable-spring-configurator:8.0.0:compile`、`flowable-app-engine-spring:8.0.0:compile` 为 compile 直接依赖，必须与 starter 处于同一提交边界，否则 8.0.0 下的自动配置会再次出现 `ClassNotFoundException: org.flowable.engine.configurator.ProcessEngineConfigurator`。

**涉及文件：**

- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\zhicloud-dependencies\pom.xml`
- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\zhicloud-module-bpm\pom.xml`

### 2.2 显式关闭未使用的 Flowable App Engine

**修改前：**

- 项目仅使用 BPMN Process Engine。
- 引入 `flowable-app-engine-spring` 后，App Engine 存在被自动装配的风险。
- 可能引入不必要的 `ACT_APP_*` 表和启动行为。

**修改后：**

在基础配置中显式关闭：

```yaml
flowable:
  app:
    enabled: false
```

边界：该开关**只关闭未使用的 App Engine**，不覆盖 Process Engine 自动配置的副作用，也不代表 `flowable-app-engine-spring` 依赖可以缺失（依赖仍需与 starter 同提交边界）。

**文件：**

- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\zhicloud-server\src\main\resources\application.yaml`

### 2.3 新增 Flowable Spring 本地事务集成测试

**测试文件：**

- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\zhicloud-module-bpm\src\test\java\cn\zhicloud\module\bpm\framework\flowable\FlowableSpringTransactionIntegrationTest.java`

**验证内容：**

- Flowable 流程启动在 Spring 本地事务提交后，历史流程实例可见。
- 本地事务抛异常回滚后，Flowable 历史流程实例不存在。

**结果：**

```text
2 tests, 0 failures, 0 errors
```

### 2.4 新增 CRM→Flowable H2 回滚集成测试

**测试文件：**

- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\zhicloud-module-crm\src\test\java\cn\zhicloud\module\crm\service\receivable\CrmReceivableBpmTransactionIntegrationTest.java`

**验证内容：**

- 使用真实 `CrmReceivableServiceImpl.submitReceivable`。
- BPM 测试实现启动真实 Flowable 流程后注入异常。
- 异常发生后：
  - CRM 回款状态回滚为 `DRAFT`。
  - Flowable 历史流程实例不存在。

**结果：**

```text
1 test, 0 failures, 0 errors
```

### 2.5 历史记录的 CRM→Flowable MySQL 8.4.9（历史沙箱版本） InnoDB 回滚集成测试

**测试文件：**

- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\zhicloud-module-crm\src\test\java\cn\zhicloud\module\crm\service\receivable\CrmReceivableBpmTransactionMysqlIntegrationTest.java`
- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\zhicloud-module-crm\src\test\resources\sql\crm_bpm_transaction_schema_mysql.sql`
- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\zhicloud-module-crm\src\test\resources\sql\crm_bpm_transaction_clean_mysql.sql`

**测试特点：**

- 使用本机 MySQL `8.4.9` 沙箱（历史执行版本；当前 T-000 沙箱版本以 `constraint-semantics-mysql-sandbox-t000-20260929_101404.json` 的 8.4.11 为准）。
- 使用 InnoDB。
- 通过系统属性 `zhicloud.mysql.integration=true` 显式启用。
- 默认构建中跳过该测试。
- 测试结束后临时数据库已删除。

**结果：**

```text
1 test, 0 failures, 0 errors
```

回滚后关键表计数：

```text
crm_receivable      = 0
ACT_HI_PROCINST     = 0
ACT_RE_DEPLOYMENT   = 0
```

### 2.5.1 当前真实 BPM 服务链验证（2026-09-29）

当前证据：

- `docs/enterprise-platform/evidence/production/crm-bpm-real-chain-mysql-20260929_124704.json`
- `docs/enterprise-platform/evidence/production/bpm-idempotency-real-chain-rerun-20260929_124758.json`
- `docs/enterprise-platform/evidence/production/crm-bpm-dynamic-datasource-mysql-20260929_141551.json`
- 历史失败复现：`docs/enterprise-platform/evidence/production/bpm-idempotency-real-chain-20260929_121828.json`

结果：

```text
CRM→真实 BPM→Flowable MySQL 8.4.11 事务链：2/2，BUILD SUCCESS
BPM 真实并发幂等复跑：1/1，10/10 请求成功，BUILD SUCCESS
CRM/BPM 动态数据源路由/事务绑定：3/3，BUILD SUCCESS
```

已覆盖：

- `CrmReceivableServiceImpl.submitReceivable → BpmProcessInstanceApiImpl → BpmProcessInstanceServiceImpl.createProcessInstance → 真实 MyBatis 幂等 Mapper → Flowable RuntimeService/HistoryService`。
- 同一 Spring 事务提交一致。
- BPM 返回后注入 CRM 外层事务故障，CRM 状态、BPM 登记和 Flowable 历史共同回滚。
- 同一 `businessKey` 的 10 并发请求返回同一实例编号，仅一条幂等登记和一条 Flowable 历史。
- `DynamicRoutingDataSource` 仅暴露 master/slave 两个显式路由，`transactionManager` 绑定动态数据源。
- Flowable `TransactionAwareDataSourceProxy` 指向同一动态数据源；`CONNECTION_ID()`/`DATABASE()` 验证 CRM/BPM/Flowable 使用同一 Spring 事务连接，提交/回滚一致且 slave 无业务写入。

边界：

- 隔离 MySQL 8.4.11 单实例 InnoDB / REPEATABLE-READ，非生产。
- 流程定义查询等非目标依赖仍为 mock。
- 历史失败证据必须保留：初版在 REPEATABLE-READ 下 10 并发出现 1 成功、9 个 PENDING 失败。根因不仅是当前读可见性，还包括旧实现依赖 JDBC affected rows 判定 ownership；Connector/J 默认 `useAffectedRows=false` 时，重复命中 `ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)` 会返回 1，非 owner 线程会误判抢占成功并继续启动 Flowable。后续已改为 `claim_token` ownership 校验并复跑通过，而不是仅反转 `claimed != 1`。
- 2026-09-30 已在隔离 MySQL 8.4.11（容器 `zhicloud-mysql-sandbox-18e0f782`，镜像 `mysql:8.4` digest `sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d`，server `8.4.11`）用 V118 幂等登记结构 + V121 `claim_token` 做双会话 REPEATABLE-READ 探针（6 步）：会话 B 开启事务后普通一致性读 `COUNT(*)`=0；会话 A 插入 `claim_token=token-A`（id=1）并提交；会话 B 再普通一致性读仍为 0；会话 B 执行 duplicate-path upsert（`INSERT ... ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)`，等同 `insertOrLockUniqueKey`）未抛 `DuplicateKeyException`，其后普通一致性读仍为 0；只有会话 B 执行 `SELECT ... FOR UPDATE`（等同 V121 `selectByUniqueKeyForUpdate`）才返回 1 行且 `claim_token=token-A`。证据 `docs/enterprise-platform/evidence/production/v118-rr-consistency-read-mysql-20260930_000517.json`（SHA-256 `ad4f07005878d802adeb725f8e72fb7e82f3ad5eb2d994e199b71e84f5931bdb`）与同名 `.log`（SHA-256 `d35631a440808b54c9cf5a62eb58e0b63a3bf735cda7293b804c98aeffd196f2`）。
- 隔离 MySQL 8.4.11 的动态数据源路由/事务绑定 3/3 已覆盖；但 master/slave 是同实例两个逻辑库，生产物理主从/真实读写分离/多租户物理连接、网络/数据库/进程故障、生产规模、Outbox/补偿和对账仍未覆盖。

### 2.5.2 claim-token ownership 修复（2026-09-29）

根因与修复：

- 旧实现依赖 JDBC affected rows 判断抢占是否成功。Connector/J 默认 `useAffectedRows=false` 时，`INSERT ... ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)` 对重复键返回 1，非 owner 线程会误判抢占成功并继续启动 Flowable。
- V121 新增 `claim_token varchar(64) NULL DEFAULT NULL`；插入抢占时写入本次调用的 UUID token，重复命中不覆盖既有 token；随后执行 `selectByUniqueKeyForUpdate`，`process_instance_id` 非空时返回既有实例，token 不匹配时 fail-closed 为 `PROCESS_INSTANCE_IDEMPOTENCY_PENDING`。
- 空白登记（`process_instance_id` 为空且 token 不匹配，包括 legacy NULL 行）也 fail-closed，不再尝试继续创建流程实例。
- 迁移与验证：`zhicloud-server/src/main/resources/db/migration/V121__bpm_process_instance_idempotency_claim_token.sql`（SHA-256 `9597f047324b1a7f7348a050eebf302f6fabf505c642cf4ede5f60a54f8093e0`）、`zhicloud-server/src/main/resources/db/verification/V121_verify.sql`（SHA-256 `23760521d1fbb2dc0c4388fcd798e4278019287a34a82cdc4372817070cd61d2`）；汇总证据为 `docs/enterprise-platform/evidence/production/bpm-idempotency-real-chain-claim-token-20260929_215500.json`（SHA-256 `8fc10375e4118c3f2310abc7acb9c6b1f3f92e83996628aac22918e0899437fa`）。
- 定向验证：claim-token unit 4/0/0/0、真实链 fail-closed 2/0/0/0、同 `businessKey` 10 并发 1/0/0/0；BPM 模块全回归 63/0/0/9、CRM 真实链 2/0/0/0、CRM 动态数据源 3/0/0/0、CRM 模块全回归 48/0/0/5。对应日志位于 `docs/enterprise-platform/evidence/production/`，日志 SHA-256 已逐项复核。
- 2026-09-30 已在隔离 MySQL 8.4.11（容器 `zhicloud-mysql-sandbox-18e0f782`，镜像 `mysql:8.4` digest `sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d`，server `8.4.11`）用 V118 幂等登记结构 + V121 `claim_token` 做双会话 REPEATABLE-READ 探针（6 步）：会话 B 开启事务后普通一致性读 `COUNT(*)`=0；会话 A 插入 `claim_token=token-A`（id=1）并提交；会话 B 再普通一致性读仍为 0；会话 B 执行 duplicate-path upsert（`INSERT ... ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)`，等同 `insertOrLockUniqueKey`）未抛 `DuplicateKeyException`，其后普通一致性读仍为 0；只有会话 B 执行 `SELECT ... FOR UPDATE`（等同 V121 `selectByUniqueKeyForUpdate`）才返回 1 行且 `claim_token=token-A`。证据 `docs/enterprise-platform/evidence/production/v118-rr-consistency-read-mysql-20260930_000517.json`（SHA-256 `ad4f07005878d802adeb725f8e72fb7e82f3ad5eb2d994e199b71e84f5931bdb`）与同名 `.log`（SHA-256 `d35631a440808b54c9cf5a62eb58e0b63a3bf735cda7293b804c98aeffd196f2`）。因此 V121 的 `claim_token` + `FOR UPDATE` 当前读是 REPEATABLE-READ 一致性读缺陷的工程缓解，不是修复；`d0ebc96c` 的定向回归（`FlowableSpringTransactionIntegrationTest`、`BpmProcessInstanceIdempotencyMapperTest`、`BpmProcessInstanceIdempotencyTransactionTest` 等 6 tests / 0 failures / 0 errors / 0 skipped、BUILD SUCCESS）**不覆盖**该一致性读路径，目前只有上述 SQL 级隔离探针验证了它。
- 动态数据源当前证据为 `crm-bpm-dynamic-datasource-mysql-20260929_214517.json`（SHA-256 `f1cb55420c93eb26a0e2693cc6e861ec22cbe5e1fccc1e35fdd67d79772b84a5`）。master 的 45 张 Flowable 表由 `zhicloud_crm_bpm_chain_test` 人工复制，`ACT_GE_PROPERTY` 复制 7 行；slave 为 0 张表，不是完全空库自举 CI；连接一致性由 `CONNECTION_ID()`/`DATABASE()` 和提交/回滚结果验证，不是网络抓包或全量审计日志。

必须保留的边界：

- 仍无 lease/expiry/recovery/reconciliation；空白登记会永久阻塞同一唯一键，需人工对账或运维回收。
- V121 不回填 legacy NULL；软删除仍占用唯一键，但 MyBatis-Plus 逻辑删除过滤可能让 `deleted=1` 的唯一键命中后 select 返回 null，进而抛 `IllegalStateException`。
- V121 为 MySQL 专用；存量大表 `ADD COLUMN` 可能触发表重建或长时间 MDL，生产前需评估备份、磁盘、复制延迟和低峰窗口。
- 以上均为隔离单机 MySQL/静态证据，不是生产物理主从、真实读写分离、多租户物理连接、故障注入、规模验证、HA/failover 或 Gate3 通过。

### 2.6 更新证据与风险文档

**更新文件：**

- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\docs\enterprise-platform\evidence\production\bpm-idempotency-mysql-20260928_150000.json`
- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\docs\enterprise-platform\08-测试与证据.md`
- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\docs\enterprise-platform\12-风险登记册.md`
- `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\docs\enterprise-platform\14-实施任务分解与执行顺序.md`

**证据中明确记录：**

- H2 事务边界验证。
- MySQL 单实例 InnoDB 回滚验证。
- Flowable 历史、部署、CRM 单据回滚后的零残留。
- 历史口径（2026-09-28）：当时 CRM→BPM 测试使用测试 BPM API，而不是真实 `BpmProcessInstanceServiceImpl` 完整调用链；当前验证已接入真实 `BpmProcessInstanceApiImpl → BpmProcessInstanceServiceImpl.createProcessInstance → 真实 Mapper → Flowable`，但流程定义查询等非目标依赖仍为 mock。
- 尚未验证生产 `DynamicRoutingDataSource`。
- `R-011`、`R-029` 保持 `OPEN`。

## 3. 回归测试结果

| 范围 | 结果 |
|---|---:|
| Flowable Spring 本地事务集成 | `2/2` |
| CRM→Flowable H2 回滚集成 | `1/1` |
| CRM→Flowable MySQL 8.4.9（历史沙箱版本）InnoDB 回滚集成 | `1/1` |
| CRM→真实 BPM→Flowable MySQL 8.4.11 事务链（当前） | `2/2, 0 failures, 0 errors, 0 skipped` |
| BPM claim-token 定向单元（当前） | `4/0/0/0` |
| BPM claim-token 真实链 fail-closed（当前） | `2/0/0/0` |
| BPM 真实并发同 `businessKey`（当前，10/10 请求成功） | `1/0/0/0` |
| V118 REPEATABLE-READ 一致性读探针（2026-09-30，隔离 MySQL 8.4.11，SQL 级非 JUnit） | 6 步：普通一致性读 0/0；duplicate-path upsert 不抛异常；`FOR UPDATE` 可见 1 行 `token-A` |
| BPM 模块（当前） | `63 tests, 0 failures, 0 errors, 9 skipped` |
| CRM 模块（当前） | `48 tests, 0 failures, 0 errors, 5 skipped` |
| server 模块（历史证据口径） | `86 tests, 0 failures, 0 errors, 3 skipped` |

其他验证：

- JSON 证据文件格式校验通过。
- `git diff --check` 通过。
- MySQL 临时库 `zhicloud_crm_bpm_tx_20260928` 已删除。
- 本阶段仅验证 H2 / 单实例 MySQL 本地事务边界，不验证生产环境。

## 4. 修改前后对比

| 项 | 修改前 | 修改后 |
|---|---|---|
| Flowable 8.0.0 Spring Boot 自动配置 | starter 缺少直接引用的可选模块，Spring 上下文启动失败 | 补齐依赖，自动配置可启动 |
| Flowable App Engine | 可能被自动装配，产生不必要组件 | 显式 `flowable.app.enabled=false` |
| Flowable 本地事务 | 没有真实 Flowable 事务边界证据 | H2 真实 Flowable 事务提交/回滚测试通过 |
| CRM→BPM 回滚一致性 | 只有单元测试和迁移/幂等证据，缺跨域事务回滚证据 | H2、历史 MySQL 8.4.9 及隔离 MySQL 8.4.11 的真实 BPM 服务链均证明 CRM 状态、BPM 登记和 Flowable 历史共同提交/回滚；流程定义查询等非目标依赖仍为 mock |
| 证据链 | 只有 BPM 幂等迁移、H2 单测等证据 | 增加 Flowable 事务边界、CRM→BPM 回滚、MySQL 单实例回滚证据 |
| 风险登记册 | `R-029` 只有静态判断和部分 H2 证据 | 增加 MySQL 单实例回滚证据，但仍不关闭 |
| 生产结论 | 不可生产部署 | 仍不可生产部署，但本地事务边界更扎实 |

## 5. 当前未解决问题

### 5.1 真实 BPM 服务链已覆盖，但仍有 mock 与生产环境缺口

隔离 MySQL 8.4.11 的当前测试已接入真实：

- `BpmProcessInstanceApiImpl`。
- `BpmProcessInstanceServiceImpl.createProcessInstance`。
- 幂等 Mapper。
- Flowable RuntimeService/HistoryService。

已覆盖同一 Spring 事务内 CRM 状态、BPM 登记和 Flowable 历史的一致提交/回滚，以及同 `businessKey` 10 并发幂等。

仍需明确边界：

- 流程定义查询等非目标依赖仍为 mock，不是完整外部集成。
- 隔离环境已确认显式 master/slave 路由、事务管理器绑定和 Flowable 同一数据源；未在生产物理主从/真实读写分离/多租户物理连接上验证。
- 未覆盖生产等价网络、数据库和进程故障。

### 5.2 隔离动态数据源路由已验证，生产物理拓扑未验证

隔离 MySQL 8.4.11 已验证：

- `DynamicRoutingDataSource` 仅暴露 master/slave 两个显式路由。
- `transactionManager` 绑定 `DynamicRoutingDataSource`。
- Flowable `TransactionAwareDataSourceProxy` 指向同一动态数据源。
- CRM、BPM 与 Flowable 在同一 Spring 事务中使用同一连接，提交/回滚一致，slave 无业务写入。

但 master/slave 是同实例两个逻辑库，不是生产主从复制或独立物理库。仍未验证：

- 生产物理主从、复制延迟和真实读写分离。
- 多租户与动态数据源叠加时的生产物理连接归属。
- 生产事务管理器与 Flowable 数据源的一致性。
- 生产网络、数据库和进程故障下的连接与事务一致性。

### 5.3 并发幂等已覆盖，生产故障注入仍未完成

已在隔离 MySQL 8.4.11 覆盖：

- 同一 `businessKey` 的 10 并发请求返回同一实例。
- 仅一条幂等登记和一条 Flowable 历史。
- BPM 返回后 CRM 外层事务故障时三者共同回滚。
- 历史并发失败证据已保留。

仍未覆盖：

- 生产等价数据库瞬时故障。
- 网络超时和进程中断。
- 提交前后异常边界。
- 幂等表与 Flowable 运行时/历史表的生产对账。
- V118 REPEATABLE-READ 一致性读路径：`d0ebc96c` 定向 6/6 未覆盖，只有 SQL 级隔离探针验证；生产等价环境的真实并发、锁等待和锁超时行为仍未验证。

### 5.4 尚未做补偿与对账

`R-029` 完整关闭还需要：

- Outbox / 补偿机制。
- CRM 单据与 Flowable 历史对账。
- 异常流程实例回收。
- 幂等残留清理。
- 独立复核。

### 5.5 claim-token 修复后的回收与对账仍未闭环

- 空白登记会永久阻塞同一唯一键；V121 不自动回填 legacy NULL，也不提供 lease/expiry/recovery/reconciliation。
- 软删除仍占用唯一键，但逻辑删除过滤可能让唯一键命中后的 select 返回 null；需要专门对账与恢复策略。
- V121 的 `ADD COLUMN` 在存量大表上可能触发表重建或长时间 MDL；需要生产规模窗口、备份、磁盘和复制延迟评估。
- 生产物理主从、真实读写分离、多租户物理连接、网络/数据库/进程故障、规模并发、Outbox/补偿/对账和独立复核仍未闭环，`R-029` 不得关闭。

## 6. 后续工作方向

### 第一优先级：在生产等价环境完成数据源、故障与补偿验证

已覆盖链路：

```text
CrmReceivableServiceImpl.submitReceivable
→ BpmProcessInstanceApiImpl
→ BpmProcessInstanceServiceImpl.createProcessInstance
→ BpmProcessInstanceIdempotencyMapper
→ Flowable RuntimeService
```

当前隔离 MySQL 8.4.11 已证明事务提交/回滚、10 并发幂等和动态数据源路由/事务绑定 3/3；下一步验证点：

- 生产 `DynamicRoutingDataSource`、主从/多租户下 CRM、BPM 和 Flowable 是否使用同一物理连接及事务管理器。
- 数据库瞬时故障、网络超时、进程中断和提交前后异常时是否仍保持一致。
- 生产规模并发、锁等待、MDL/复制影响和恢复能力。
- Outbox/补偿、幂等残留清理及 CRM 与 Flowable 运行时/历史对账。

### 第二优先级：在生产物理拓扑上验证动态数据源路由

隔离 MySQL 8.4.11 已确认 `DynamicRoutingDataSource` 只暴露 master/slave、`transactionManager` 绑定动态数据源、Flowable `TransactionAwareDataSourceProxy` 指向同一数据源，且事务中 CRM/BPM/Flowable 使用同一连接；但 master/slave 是同实例两个逻辑库。生产仍需确认：

- 生产物理主从/真实读写分离下 Flowable 自动配置拿到的是生产 `DynamicRoutingDataSource`。
- CRM MyBatis 和 Flowable 在生产多租户叠加时使用同一个主数据源。
- 写操作始终路由到 master。
- 生产复制、延迟和故障下事务中不会发生主从切换导致的连接不一致。

### 第三优先级：补充并发与故障注入

需要覆盖：

- 在生产等价数据源和生产规模下验证同一 `businessKey` 并发提交。
- Flowable 启动成功后本地事务失败。
- 幂等表与 Flowable 运行时/历史表一致性。
- 异常后补偿和对账。

### 第四优先级：整体生产就绪门禁

继续处理更大的生产阻断项：

- K8s server-side dry-run / 真实集群部署。
- 生产 TLS。
- 外部 Secret 轮换。
- 依赖 CVE。
- 备份与 PITR。
- HA 故障切换。
- 生产规模 MDL / 复制延迟 / DDL 风险。
- 可观测性与告警。
- 独立复核。

## 7. 当前状态标记

| 状态项 | 当前值 |
|---|---|
| `production_ready` | `false` |
| `gate3_passed` | `false` |
| `R-011` | `OPEN` |
| `R-029` | `OPEN` |
| 模拟数据 | 仅用于开发 / 测试 / 演示 / 沙箱 |
| 生产结论 | 不可直接生产部署；Go/No-Go=`No-Go` |
| `independent_review` | `false` |
| 本阶段结论 | H2 / 单实例 MySQL InnoDB 本地事务边界及隔离 MySQL 8.4.11 真实 BPM 链、claim-token ownership/fail-closed、10 并发幂等、动态数据源路由/事务绑定 3/3 已验证；2026-09-30 另取得 V118 REPEATABLE-READ 一致性读缺陷隔离证据（`v118-rr-consistency-read-mysql-20260930_000517.json`，SHA-256 `ad4f07005878d802adeb725f8e72fb7e82f3ad5eb2d994e199b71e84f5931bdb`），`claim_token` 属缓解非修复；仍无 lease/expiry/recovery/reconciliation，非生产 |

## 8. 交接提醒

1. 当前工作区存在大量未提交修改和证据文件，接手前先做完整 review 和分组提交。
2. 不要把当前 MySQL 单实例真实链、动态数据源路由事务绑定、并发幂等或回滚结果表述为“生产级验证”。
3. 不要因为 `P0=0` 就认为安全闭环。
4. 不要把 Maven `BUILD SUCCESS` 解释为“生产可部署”。
5. `R-011`、`R-029` 仍必须保持 `OPEN`。
6. 隔离动态数据源路由/事务绑定 3/3 已完成；下一项核心工作是在生产物理主从/真实读写分离/多租户拓扑上验证路由、故障/补偿和对账、生产规模并发与恢复，而不是继续扩大 H2 覆盖。
7. V121 只解决 claim ownership 和空白登记 fail-closed；不要把 `claim_token`、V121 迁移或 7 组 `BUILD SUCCESS` 表述为生产验证、HA/failover 或 Gate3 通过。
8. V118 REPEATABLE-READ 一致性读缺陷只有隔离 MySQL 8.4.11 的 SQL 级探针验证，不得据此宣称该缺陷已修复或用来关闭 `R-011`/`R-029`；迁移契约 canonical-byte 门禁通过也不等于 MySQL 执行、生产迁移或 Gate3 通过。
