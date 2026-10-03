# 智云企业数字化中台升级收尾交接（2026-09-30）

## 0. 文档信息

| 项目 | 内容 |
|---|---|
| 日期 | 2026-09-30 |
| 仓库 | `ZhiCloud-Enterprise-Platform` |
| 分支 | `upgrade/production-readiness` |
| HEAD | `d0ebc96c9f864450c9c293094e114aff0e4e8588` |
| 工作树状态 | 576 项（56 tracked 修改 + 520 untracked）；未冻结、未提交 |
| 总门禁 | `production_ready=false`、`gate3_passed=false`、`independent_review=false`、`R-011=OPEN`、`R-029=OPEN`、Go/No-Go=`No-Go` |
| 本文档性质 | 工作过程收尾记录，不是生产验收报告，不改变任何门禁状态 |

## 1. 工作过程

### 1.1 本轮已完成的事项

1. 逐项阅读了交接文档 `docs/enterprise-platform/handoff/zhicloud-enterprise-platform-upgrade-handover-20260928.md` 和工作树分组 review 清单 `docs/enterprise-platform/handoff/uncommitted-review-groups-20260928.md`。
2. 按清单第 5 节逐项核对依赖顺序，确认分组静态 review 已完成至 G2.5、G2.6、G2.7、G2.8、G3、G3.5、G4、G5、G6；G7 为空/不适用。
3. 实测当前工作树计数：`git status --porcelain -uall` 为 576 项（56 tracked + 520 untracked），不带 `-uall` 折叠为 291 行。
4. 定点修正了 `00-README.md:105` 和 `01-平台现状核验.md:15` 的工作树计数，从旧值 `570/54/516/285` 修正为 `576/56/520/291`。
5. 将 `13-安全基线扫描证据.md` 第 4 行唯一混入的 CRLF 归一为 LF（文件现为 0 CRLF、354 LF、无 BOM），并确认未改变任何内容。
6. 更新了 review 清单的对应记录，将"工作树计数已经漂移"标记为"已于 2026-09-30 修正"，并在末尾新增本轮修订说明。

### 1.2 前序已完成的核心工作（来自交接报告，本日未重做）

- 企业中台审计与升级设计：00-14 方案文档全部形成。
- 安全迁移：V115 明文凭据清理、V116 活跃账号唯一约束、V117 CRM 回款幂等、V118 BPM 幂等登记。
- 新增迁移：V119 库存租户唯一约束、V120 Outbox/Inbox owner/lease、V121 BPM claim-token。
- 安全基线：487 文件、216 findings、P0=0、P1=216、controls_failed=0。
- Python 全量：474 tests OK（当前工作树证据包）。
- 迁移契约：scanned=128 / tracked=32 / findings=[] / passed。
- CRM/BPM 真实链：隔离 MySQL 8.4.11 事务、10 并发幂等、动态数据源路由 3/3。
- V118 REPEATABLE-READ 一致性读缺陷隔离探针：6 步实证并保存证据。
- 物理复制近似：单机双容器 GTID 93/93 checks。
- 本地 Helm 渲染/lint：6 正向 + 10 负向 fail-closed + 12 schema 用例通过。
- 工作树分组 review：56 tracked + 22 非证据 untracked + 498 证据文件全部纳入分组。

### 1.3 本轮未做的事项

- 未执行 `git add`、`git commit`、`git push`。
- 未修改任何业务代码、迁移 SQL、迁移契约 JSON 或 V118 哈希。
- 未重跑 Maven、Python 或迁移契约测试（仅做文档定点修订和文件系统 EOL 修正）。
- 未获取生产等价环境、凭据、窗口或责任人。

## 2. 当前项目状况

### 2.1 可以做的

- 继续开发、测试、迁移预演和沙箱验证。
- 按分组清单处理当前未提交改动。
- 准备生产等价环境验证所需的清单和依赖。

### 2.2 不能做的

- 不能直接导入生产数据、上线生产环境。
- 不能宣称 `production_ready=true`、`gate3_passed=true` 或 `independent_review=true`。
- 不能关闭 `R-011` 或 `R-029`。
- 不能把隔离 MySQL、H2、单机双容器或同实例双逻辑库的结果替代为生产等价验证。
- 不能把文档修订、分组 review 或哈希清单完成误写为生产门禁通过。

### 2.3 工作区冻结与提交边界

- 当前改动尚未冻结提交。
- 按分组清单建议顺序：G2.5 → G2.6 → G2.7 → G2.8 → G3 → G3.5 → G4 → G5 → G6。
- G6（498 份证据快照）必须与代码和文档分开审查和提交。
- 提交说明必须逐组标注当前/历史/失败复现/仅追溯。
- 冻结提交后必须重跑静态扫描与迁移契约，再按台账修复扫描器。

## 3. 已解决的问题

| 问题 | 状态 | 说明 |
|---|---|---|
| 文档计数漂移 | 已解决 | `00-README.md` 和 `01-平台现状核验.md` 已从 570/54/516/285 修正为 576/56/520/291 |
| `13` 文档 EOL 混入 | 已解决 | 唯一一处 CRLF 已归一为 LF，文件行尾保持一致 |
| 分组静态 review | 已完成 | G2.5–G6 全部纳入清单并登记结论 |
| 新增风险登记 | 已完成 | R-G3.5-1~8、R-G4-1~4、R-G5-1~4、R-G6-1~5 |
| 迁移契约 canonical-byte | 已解决 | V1.1 verification SQL 使用 LF canonical 字节计算 SHA-256，V118 迁移哈希未变 |
| V118 REPEATABLE-READ 缺陷发现 | 已登记 | 隔离 MySQL 8.4.11 探针实证并保存证据 |
| Flowable 依赖缺失 | 已解决 | 补齐 starter 依赖配置模块，server 可打包启动 |
| BPM claim ownership | 已解决 | V121 claim_token + FOR UPDATE 当前读，空白登记 fail-closed |
| Outbox/Inbox owner fencing | 已解决 | V120 增加 owner/lease 字段和状态租约索引，隔离证据 9/0/0/0 |
| Helm 渲染/负向 | 已完成 | 6 正向 + 10 负向 + 12 schema，本地渲染和离线校验通过 |

## 4. 当前问题清单

### 4.1 未解决的生产等价问题

| 编号 | 问题 | 当前状态 | 影响 |
|---|---|---|---|
| P-01 | 生产物理主从/真实读写分离/多租户物理拓扑 | 仅有单机双容器 GTID 近似 93/93 | 生产拓扑、HA/failover 未验证 |
| P-02 | 生产故障/补偿/对账 | 网络超时、DB 瞬时故障、进程中断、提交边界异常未覆盖 | 跨域一致性未证明 |
| P-03 | 生产规模迁移 | 无生产规模数据，MDL/复制/磁盘/锁等待/回滚未验证 | 大表 DDL 风险未知 |
| P-04 | 非 MySQL 方言实库 | 静态脚本存在，未在目标实库执行 | 多方言支持未证明 |
| P-05 | K8s server-side dry-run/真实部署 | 无 kubeconfig、helm/kind/minikube/k3d 均缺失 | 部署门禁 G4 未通过 |
| P-06 | Secret/TLS/CVE/供应链 | 静态 P0=0 但运行时安全未闭环 | 安全门禁 G1 部分通过 |
| P-07 | 核心业务正确性 | 库存/QMS/MES/WMS/ERP 未闭环 | 业务门禁 G2 未通过 |
| P-08 | 运营/合规/验收 | 备份、PITR、SLO、等保、E2E 未完成 | 运营门禁 G5-G8 未通过 |
| P-09 | V118 REPEATABLE-READ | 需人工决定接受并发约束或引入 lease/expiry/recovery/reconciliation | 影响并发安全决策 |
| P-10 | 独立复核 | `independent_review=false` | 不能解除总门禁 |

### 4.2 未解决的工作树问题

| 编号 | 问题 | 当前状态 | 影响 |
|---|---|---|---|
| W-01 | 576 项改动未冻结提交 | 未分组提交 | 无法做版本一致性验证 |
| W-02 | 对象/哈希清单未正式纳入版本 | 只有临时清单 | 冻结提交时需重新生成并签字 |
| W-03 | 部分证据记录与当前工作树不一致 | runner 证据 target_version=117、当前 Java V2..V121 | 只能作追溯，不能作当前验收 |
| W-04 | 498 份证据含本机路径/内部主机名 | 入库前需按证据保留/脱敏政策确认 | 安全/审计合规风险 |
| W-05 | 后续编辑会再次改变 diff 统计 | 需冻结前重新实测 | 不得沿用旧 +/- 数值 |

### 4.3 外部依赖（生产等价环境阻塞）

见交接报告第 11 节。核心缺失：K8s 集群/kubeconfig、部署工具链、目标数据库与凭据、客户端工具、Secret Manager、生产 TLS 证书、生产拓扑、故障注入/规模数据。责任角色和窗口当前均为"未指定"。

## 5. 后续计划方向

### 5.1 第一优先级（近期，1-2 天）

1. **分组提交**：按清单 G2.5 → G6 顺序逐组提交，每组提交后记录 commit SHA 并重跑迁移契约。
2. **补齐对象/哈希清单**：冻结前重新生成含 `path / bytes / mtime_utc / sha256` 的完整清单并纳入版本，指定责任人签字。
3. **冻结重跑静态扫描与迁移契约**：避免行号漂移后再修复扫描器。
4. **V118 REPEATABLE-READ 人工结论**：明确接受并发约束还是引入 lease/expiry/recovery/reconciliation。

### 5.2 第二优先级（中期，1-2 周）

1. **获取生产等价环境**：按交接报告第 11 节清单，逐项向运维/DBA/安全推责，取得 kubeconfig、目标库、凭据、窗口、责任人。
2. **生产拓扑验证**：物理主从/真实读写分离/多租户物理连接 + 同一事务管理器。
3. **生产故障/补偿/对账**：网络超时、DB 瞬时故障、进程中断、提交边界异常 + Outbox/Inbox DLQ/重放/补偿演练。
4. **生产规模迁移验证**：V116-V121 在生产等量库上的 MDL/锁等待/复制/磁盘/回滚。

### 5.3 第三优先级（中长期）

1. **安全与部署底座**：真实 TLS 握手、外部 Secret 注入、历史 Secret 轮换、依赖 CVE/SBOM、K8s server-side dry-run 和预发部署。
2. **核心业务正确性**：ERP 财务过账、QMS 质量门禁、MES 工单状态机、WMS 流程防旁路、CRM 完整闭环。
3. **非 MySQL 方言**：PostgreSQL、Highgo、Kingbase、OpenGauss、Oracle、SQL Server、DM8 实库验证。
4. **运营/合规/验收**：压测、长稳、容量、SLO、告警、on-call 演练、备份/PITR/灾备、等保、个人信息保护、六条企业主流程 E2E、独立复核。

### 5.4 明确不做的事

- 不继续扩大 H2 或隔离用例规模。
- 不在漂移工作树上先改扫描器（先冻结再重跑）。
- 不把本日文档修订写成测试或生产验证通过。
- 不在没有外部环境输入的情况下推进生产等价验证。

## 6. 接手注意事项

1. **先读**：`00-README.md` → `05-生产就绪门禁清单.md` → `08-测试与证据.md` → `10-最终Review与交付审计.md` → `12-风险登记册.md` → `13-安全基线扫描证据.md` → `14-实施任务分解与执行顺序.md`。
2. **再读**：本目录下的 `zhicloud-enterprise-platform-upgrade-handover-20260928.md`（总交接）和 `uncommitted-review-groups-20260928.md`（分组 review 清单）。
3. **禁止**：`git reset`、`git checkout`、`git clean` 覆盖工作树；导入生产数据；修改 V118 migration 哈希 `f57dc75a...`；修改 `migration-contract.json` 的 CRLF；把隔离结果写成生产验证。
4. **保持**：在全部完成前，维持 `production_ready=false`、`gate3_passed=false`、`independent_review=false`、`R-011=OPEN`、`R-029=OPEN`、Go/No-Go=`No-Go`。
5. **文档编码**：所有 `.md` 保持 UTF-8 无 BOM、LF 行尾；禁止引入混合行尾或 BOM。
6. **本日已修订的文件**：
   - `docs/enterprise-platform/00-README.md`（计数修正）
   - `docs/enterprise-platform/01-平台现状核验.md`（计数修正）
   - `docs/enterprise-platform/13-安全基线扫描证据.md`（EOL 修正，1 CRLF → LF）
   - `docs/enterprise-platform/handoff/uncommitted-review-groups-20260928.md`（更新计数修正记录 + 本日修订说明）
   - 本文件（新增）