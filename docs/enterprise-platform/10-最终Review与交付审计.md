# 10 最终 Review 与交付审计

## 1. 当前审计结论

| 项目 | 结论 |
|---|---|
| 方案设计 | 已形成 |
| 现状核验 | 已完成 |
| 外部对标 | 已形成 |
| 生产就绪 | **否** |
| `production_ready` | `false` |
| `gate3_passed` | `false` |
| 安全扫描 | `passed`（P0=0，P1=216） |
| 是否可直接部署生产 | **否** |

## 2. 已完成的交付

1. 确认项目定位为企业数字化中台，覆盖 ERP/MES/WMS/QMS/CRM/HR/OA/TMS。
2. 核验仓库、分支、模块、迁移和测试现状。
3. 建立企业级能力差距分析。
4. 形成目标架构、业务路线、迁移、模拟数据、测试、排期、风险和任务分解文档。
5. 补齐 12-14 文档，并完成 00-14 文档一致性和边界自审。
6. 生成安全扫描证据：2026-09-29 时点为 487 文件；2026-09-30 复跑当前口径为 494 文件、216 发现、P0=0、P1=216、controls_failed=0，证据 `security-baseline-t000-20260930_113027.json`（SHA-256 `284e9d7fd767405b…`）；旧件 `security-baseline-t000-20260929_095135.json` 仅作追溯（生成于 2026-09-29 09:52，早于 V119/V120/V121 证据，未覆盖后续迁移安全验证；历史 `security-baseline-20260928_151500.json` 的 482 文件仅作追溯）。P0=0 只代表当前静态扫描未命中阻断项，真实 TLS、外部 Secret、依赖 CVE 和运行时权限仍未闭环。
7. 生成 Python 门禁证据：当前口径 `Ran 532 tests ... OK`（退出码 0；2026-09-30 补齐工具链并完成复核返工后复跑）（需 pwsh/mysql 在 PATH）；历史 474 与补链前 481/47-errors 均作追溯；历史 T-000 473 tests、专项 81 tests OK 仅作历史口径，不替代当前全量结果。
8. 新增 V115 生产种子账号清理迁移，并从 8 套安装脚本移除 96 条客户端明文凭据 INSERT。
9. 新增 V116 MySQL 活跃账号唯一约束和验证 SQL，并在 MySQL 8.4.9 clean 沙箱真实执行、验证和重跑幂等通过。
10. 完成 bootstrap 静态契约和只读审计器测试；MySQL 8.4.9 clean 沙箱 runner 验证 ensure 创建、相同密码 noop、不同密码 fail-closed 且哈希不变、rotate 成功、旧密码失效、新密码 noop，并通过只读审计确认账号、租户、角色、岗位、BCrypt 形态和唯一索引一致；`independent_review=false`。
11. 完成 CRM 回款并发最小修复和 V117 MySQL 回款幂等迁移；16 个定向测试通过，当前 CRM 模块 48 tests（0 失败 / 5 skipped），V117 已在 MySQL clean 沙箱执行并重跑幂等；隔离 MySQL 沙箱已覆盖并发重复拒绝、软删除永久占用和 100,000 行合成 DDL 成本。隔离 MySQL 8.4.11 已通过 CRM→真实 BPM→Flowable 2/2、同 businessKey 10 并发 1/1（联合验证 V118 登记）和动态数据源路由/事务绑定 3/3，证据为 `crm-bpm-dynamic-datasource-mysql-20260929_141551.json`；master/slave 是同实例两个逻辑库，V117 仍缺生产规模 MDL/复制验证，V118 仍缺生产物理主从/真实读写分离/多租户数据源拓扑、故障、规模并发、Flowable 对账和补偿验证。V121 另在隔离 MySQL 8.4.11 上通过 claim-token unit 4/0/0/0、真实链 fail-closed 2/0/0/0、10 并发 1/0/0/0，并完成 V121 迁移/验证 SQL lint 2 文件 0 违规；该证据修复 BPM 登记 claim ownership 语义，但不替代生产物理主从/真实读写分离、故障、规模、Flowable 对账和补偿验证。
12. 生成 Helm 3.22.0/4.3.0 本地渲染、6 个正向、10 个负向 fail-closed、revision 轮换契约和 12 个离线 schema 用例证据（`helm-render-20260929_202001.json`；SHA-256 `72FB8CAEDDF2642F200D0C3B44CD0C794BBD2E8FCB843C4D34622A059A5C43DC`）；`server_side_dry_run=not_run` 明确保留。
13. 新增 BPM 流程实例幂等登记 V118：DTO 入口按租户、流程 key 和 businessKey 去重并回填实例 ID；历史 MySQL 8.4.9 clean 沙箱建表成功且重跑幂等；Mapper 幂等 2/2、H2 事务回滚专项 2/2 通过。当前隔离 MySQL 8.4.11 已接入真实 `BpmProcessInstanceApiImpl → BpmProcessInstanceServiceImpl.createProcessInstance → 真实 Mapper → Flowable`，事务链 2/2、同 businessKey 10 并发复跑 1/1 和动态数据源路由/事务绑定 3/3；master/slave 是同实例两个逻辑库，流程定义查询等非目标依赖仍为 mock，生产物理主从/真实读写分离/多租户数据源拓扑和 Flowable 对账仍未验证。V121 进一步为 BPM 幂等登记增加 `claim_token` ownership，并在隔离 MySQL 8.4.11 通过 claim-token unit 4/0/0/0、真实链 fail-closed 2/0/0/0、10 并发 1/0/0/0；仍存在空白登记永久阻塞、legacy NULL 不回填、逻辑删除过滤可能命中唯一键但 `select` 返回 null、无 lease/expiry/recovery/reconciliation，以及生产大表 `ADD COLUMN` 的 MDL/表重建/磁盘/复制风险。
14. 明确未完成项和禁止宣称项。
15. 交付模拟数据：474 表结构快照生成器、19 张主数据表/267 行、99 张业务/支撑表/2,458 行、SQL/JSON/manifest 三件套、清理脚本、只读不变式校验和 29 项生成器回归测试。已在 MySQL 8.4.9 临时库完成 clean baseline、118 张表导入、逐表计数差异 0、六条跨域数据级关联和 cleanup 后 118 表清零验收（`demo-data-mysql-20260928_133559.json`）。**该证据只证明数据级 SQL/关联/清理通过，不是应用层端到端验收**；仅 MySQL 单方言，API、审批、权限、并发、幂等、财务过账和质量放行仍未验证，T-303 仍 OPEN。
16. 补充隔离单机容器 MySQL 8.4 GTID 复制近似证据：93 checks / 0 failed / exit 0，覆盖最小 V100→V104→V107→V120、Outbox/Inbox 复制、受控停源/单向副本提升和直接 SQL owner fencing 37/37；另有应用级 Outbox/Inbox owner fencing 隔离 MySQL 8.4.11 测试 9 tests / 0 failures / 0 errors / 0 skipped（`event-owner-fencing-mysql-20260929_191149.json`，最小 V100→V104→V107→V120 依赖链）。该复制近似证据仅在同一 Docker host 的两个容器内成立，不是生产物理主从；应用级 owner fencing 也只是单节点最小链沙箱证据，不证明 HA/故障切换、真实网络分区、生产规模、生产 worker 并发、DLQ 服务重放、补偿/对账或租户数据源拓扑，不能用于关闭 G3、R-011、R-029。

## 3. 当前未完成项

### 3.1 安全

- 外部 Secret 的真实注入、对象存在性、键名、权限和挂载仍未验证；R5 空 Secret 命中已确认是扫描器假阳性，当前为 0，不作为静态 P0。
- 20 条生产 Flyway 种子账号。
- 12 条 `V1.1` 明文客户端凭据；8 套安装脚本的 96 条已移除但尚未完成真实 Secret 轮换和旧值失效验证。
- V116 已在 MySQL clean 沙箱执行并重跑幂等；隔离 MySQL 沙箱已覆盖并发唯一性、软删除后重建和 100,000 行合成 DDL 成本；生产规模 MDL/复制仍未验证。
- V117 已在 MySQL clean 沙箱执行并重跑幂等；隔离 MySQL 沙箱已覆盖并发重复拒绝、软删除永久占用和 100,000 行合成 DDL 成本，仍缺生产规模 MDL/复制验证。V118 BPM 幂等登记已在隔离 MySQL 8.4.11 通过 CRM→真实 BPM→Flowable 2/2、同 businessKey 10 并发 1/1 和动态数据源路由/事务绑定 3/3；master/slave 是同实例两个逻辑库，仍缺生产物理主从/真实读写分离/多租户数据源拓扑、生产故障、规模并发、Flowable 运行时/历史对账及 Outbox/补偿/对账验证。V121 已修复隔离环境中的 BPM claim-token ownership 缺陷（unit 4/0/0/0、真实链 fail-closed 2/0/0/0、10 并发 1/0/0/0、SQL lint 2 文件 0 违规），但仍缺生产等价验证，并保留空白登记永久阻塞、legacy NULL 不回填、逻辑删除过滤不一致、无 lease/expiry/recovery/reconciliation 和生产大表 `ADD COLUMN` 的 MDL/表重建/磁盘/复制风险。
- 非 MySQL 方言没有等价的活跃账号唯一约束迁移；存量 TOTP 升级和 SQL Server 列注释未完成。
- bootstrap 的 MySQL runner 实库执行和只读审计在 2026-09-28 时点完成；**2026-09-30 本机复跑 `status=failed`**：脚本只授 `@'127.0.0.1'`，宿主经 Docker 发布端口连入时容器内来源为 `172.17.0.1`，应用级账号被拒（`R-033`；证据 `mysql-runner-sandbox-20260930_124102.json`）。未放宽任何授权。 因此该项**不得再写成"已完成"**，须待 R-033 裁决后重跑成功再恢复。该审计只覆盖 MySQL 沙箱库，且 `independent_review=false`，不证明生产目标库、事务、并发或其余 7 套方言。其余 7 套尚未在目标实库执行，非 MySQL 方言尚无实库语法、事务和幂等性证据。
- K8s 服务端 dry-run 和真实集群部署；本地 Helm 3/4 渲染、lint 与离线 schema 校验已通过，但不能替代集群验证。
- 真实 TLS 握手、证书链、主机名验证。
- SSO/MFA、审计 WORM、密钥轮换。
- 依赖漏洞和供应链安全闭环。

### 3.2 数据与业务

- 库存单一真值。
- QMS 门禁。
- MES 状态机和超产/超发料。
- WMS 流程旁路和盘点。
- CRM 回款并发已完成最小修复，V117 已在 MySQL clean 沙箱执行并重跑幂等；隔离 MySQL 沙箱已覆盖并发重复拒绝、软删除永久占用和 100,000 行合成 DDL 成本。V118 已在隔离 MySQL 8.4.11 通过真实 BPM 事务链 2/2、同 businessKey 10 并发 1/1 和动态数据源路由/事务绑定 3/3；但 V117 生产规模 MDL/复制、V118 生产物理主从/真实读写分离/多租户数据源/故障和 BPM 补偿对账仍未闭环。
- ERP 财务过账和对账。
- 全模块 `api` 包和跨模块依赖治理。
- 模拟数据的业务单据层：118 张表 / 2,725 行已在本机 MySQL 8.4.9 实库导入并完成计数、六条跨域数据级关联和 cleanup 清零；但六条流程尚未通过 API/页面/审批/权限/并发/幂等/财务过账/质量放行做应用层端到端验收，dm/highgo/kingbase/opengauss/oracle/postgresql/sqlserver 七套方言仍未适配。

### 3.3 部署与运营

- K8s 服务端 dry-run。
- 真实生产环境部署。
- 备份恢复和灾备。
- 压测、长稳、故障注入。
- SLO、告警、on-call。
- 合规审计。

## 4. 审计检查表

### 设计检查

- [x] 目标范围明确。
- [x] 架构原则明确。
- [x] 业务路线明确。
- [x] 迁移和回滚原则明确。
- [x] 模拟数据边界明确。
- [x] 资源和风险说明明确。

### 生产检查

- [x] 安全扫描 P0=0（当前 2026-09-30 口径：494 文件、216 条 P1；不代表 P1、真实 TLS、外部 Secret 和依赖 CVE 已关闭）。
- [ ] 所有 Secret 来自外部密钥系统。
- [ ] V116 已完成 MySQL clean 执行和重跑幂等，并在隔离 MySQL 沙箱完成并发唯一性、软删除重建和 100,000 行合成 DDL 观测；仍需验证生产规模 MDL/复制/回滚。
- [ ] V117 已完成 MySQL clean 执行和重跑幂等，并在隔离 MySQL 沙箱完成并发重复拒绝、软删除永久占用和 100,000 行合成 DDL 观测；仍需验证生产规模 MDL/复制/回滚。
- [x] BPM businessKey 数据库幂等工程件已落地（V118 + V121 `claim_token` ownership + Mapper 唯一键/H2 事务回滚测试），并有隔离 MySQL 8.4.11 真实服务链 2/2、同 businessKey 10 并发 1/1、动态数据源路由/事务绑定 3/3 证据；仍缺生产物理主从/真实读写分离/多租户数据源拓扑确认、生产故障、规模并发、Flowable 运行时/历史对账及 Outbox/补偿/对账。V121 另有 unit 4/0/0/0、真实链 fail-closed 2/0/0/0、10 并发 1/0/0/0 和 SQL lint 2 文件 0 违规；仍不能关闭 R-011/R-029。
- [ ] 隔离单机容器 GTID 复制近似 93/93 和直接 SQL owner fencing 37/37 已通过；应用级 Outbox/Inbox owner fencing 在隔离 MySQL 8.4.11 最小链测试 9/0/0/0 通过；生产物理主从/真实读写分离/多租户拓扑、真实故障和 Outbox/补偿/对账仍未验证。
- [ ] 非 MySQL 方言具备等价的活跃账号唯一约束和存量 MFA schema 升级路径。
- [ ] MySQL bootstrap runner 已在沙箱实库完成 ensure/noop/fail-closed/rotate 并通过只读审计；其余 7 套目标数据库仍需执行并重跑通过，且尚未完成独立复核。
- [x] 本地 Helm 3/4 渲染/lint 与离线 schema 校验通过；`server_side_dry_run=not_run`，仍需 K8s 服务端 dry-run。
- [ ] 真实 TLS 握手通过。
- [ ] 证书链和主机名验证通过。
- [ ] K8s 服务端 dry-run 通过。
- [ ] 真实生产迁移通过。
- [ ] 库存对账通过。
- [ ] 质量门禁通过。
- [ ] 工单状态机和并发测试通过。
- [ ] 回款和过账幂等通过。
- [ ] 备份恢复通过。
- [ ] 灾备 RTO/RPO 达标。
- [ ] 压测和长稳通过。
- [ ] SLO/告警/on-call 演练通过。
- [ ] 合规审计通过。
- [ ] 六条主流程端到端验收通过。

## 5. Go/No-Go 规则

只有同时满足以下条件才可标记生产就绪：

1. G0-G8 全部门禁通过。
2. 安全扫描 P0=0，且不是通过放宽规则实现。
3. 真实环境证据齐全。
4. 关键业务闭环有并发、幂等、回滚和对账测试。
5. 备份恢复和灾备演练通过。
6. 没有未关闭的 P0 风险。

隔离单机容器 GTID 复制近似 93/93 不改变该结论。当前不满足，因此结论为 **No-Go**。

统一状态：`production_ready=false`；`gate3_passed=false`；`independent_review=false`；`R-011=OPEN`；`R-029=OPEN`；`Go/No-Go=No-Go`。

## 6. 变更控制

- 任何迁移、Secret、权限、财务规则、质量规则变更必须走评审。
- 任何 P0 修复必须附证据。
- 任何“通过”结论必须引用当前 commit。
- 任何模拟数据必须标注环境和禁止生产导入。
- 任何生产上线必须经过最终评审和回滚演练。

## 7. 下一步建议

1. 先完成 P0 安全和生产底座。
2. 先完成 V116/V117 的生产规模 MDL/复制/回滚验证；V118 需在生产物理主从/真实读写分离/多租户数据源、故障、规模、Flowable 对账与补偿环境验证；V119 需在生产等价 MySQL 验证库存租户唯一约束的并发、回滚及 MDL/复制影响；V120 需在生产等价环境验证 Outbox/Inbox owner/lease 的多 worker、并发、故障切换、DLQ、重放、补偿和对账（隔离动态数据源路由/事务绑定 3/3 和应用级 owner fencing 9/0/0/0 已完成，但不能替代生产验证）；V121 需在生产等价环境验证 `ADD COLUMN` 的 MDL/表重建/磁盘/复制影响、空白登记/legacy NULL 回收、逻辑删除语义以及 lease/expiry/recovery/reconciliation（隔离 claim-token unit 4/0/0/0、真实链 fail-closed 2/0/0/0、10 并发 1/0/0/0 和 SQL lint 2 文件 0 违规已完成，但不能替代生产验证）；随后完成非 MySQL 账号唯一约束/MFA 实库验证，再处理库存、质量、工单和 CRM 资金链路剩余正确性问题。
3. 然后补全集成、测试、可观测性和灾备。
4. 最后做六条主流程验收和上线评审。

**最终表述：**

> 本项目已完成企业级升级方案设计，并具备继续生产化改造的基础；当前仍不能直接生产部署，不能宣称 `production_ready=true` 或 `gate3_passed=true`。隔离动态数据源路由/事务绑定 3/3 不等同于生产物理主从或真实读写分离验证。
