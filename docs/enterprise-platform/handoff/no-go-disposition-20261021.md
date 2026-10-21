# 生产 Go/No-Go 处置说明（2026-10-21）

> 本文是**处置说明**，不是放行结论。
> 截至本文生成时，统一状态仍为：
> `production_ready=false`、`gate3_passed=false`、`independent_review=false`、
> `R-011=OPEN`、`R-029=OPEN`、`Go/No-Go=No-Go`。

## 1. 结论

本轮完成了**业务可达性与演示数据**层面的收口，但没有、也不能在本地沙箱关闭生产 No-Go。原因不是“还有页面打不开”，而是三类外部/生产等价条件仍未满足：

1. **许可证门禁**需要法务或授权人逐项裁定；工程侧不能自行把 `deny` 改成 `allow`，也不能预填 allowlist。
2. **R-011 / R-029** 需要生产等价的数据库、事务、故障与对账证据；隔离 MySQL、H2、单机容器不能替代。
3. **G0-G8 的其余生产门禁**（真实 Secret/TLS、K8s/部署、可观测性、合规、备份恢复/灾备、六条主流程生产验收）仍未闭环。

因此本轮的正确交付物是：把“能做的业务验证”做完并留下证据，把“不能自行关闭的项”整理成可交法务/运维/DBA 的处置清单，而不是把门禁状态翻绿。

## 2. 本轮已完成（业务侧，不改变生产状态）

| 项 | 结果 | 证据/入口 |
|---|---|---|
| 全菜单主列表 API smoke | 325 个有主列表的页面：`pass=323`、`fail=0`、`external=2` | `docs/enterprise-platform/evidence/production/business-pilot-data-20261021/api_smoke_summary.txt` |
| 47 个原 smoke 失败逐项核实 | 45 个补父单/上下文参数后 `code=0`；2 个公众号接口为微信 AppID 无效的外部依赖 | `.../smoke_fail_verification.md`、`.../smoke_fail_verification.csv` |
| ERP 演示数据 | 修复生成器漏写 `total_price`；回填存量；补充今日/昨日/本月演示单据 | `.../seed_visual_data.sql`、`.../backfill_erp_total_price.sql` |
| ERP 统计上界 | 修复 `today/month/year` 未传结束时间导致未来单据被计入的缺陷 | `ErpSaleStatisticsController.java`、`ErpPurchaseStatisticsController.java` |
| HRM 工资条 | 为演示员工 925000 补齐 2026-11 月度工资记录与工资条，门户列表非空 | `.../seed_salary_slip.sql` |
| README 截图 | 8 张页面截图按当前数据重新生成 | `docs/images/screenshots/`、`docs/screenshots.md` |

边界：以上数据均为租户 `9001` 的演示数据，**禁止导入生产**；smoke 的 `external=2` 不代表接口代码通过，只代表本地没有有效微信公众号 AppID/密钥，无法完成外部调用验证。

## 3. 阻断项 A：许可证门禁

### 3.1 当前事实

- 门禁输入：`docs/enterprise-platform/evidence/production/maven-license-inventory-regen-20260928_0127.json`
- 复核输出：`docs/enterprise-platform/evidence/production/license-gate-recheck-20261021.json`（本轮重跑）
- 判定：`decision=fail`、`blocking=true`
- 计数：`components=862`、`denied=13`、`review_required=38`、`runtime_blocked=38`、`allowlist_hits=0`
- 处置台账：`docs/enterprise-platform/evidence/production/license-disposition-register-20260928_0143.md`
- 台账状态：51 项全部 `PENDING`

### 3.2 13 项 `deny_until_approved`

| 组件 | 运行时 | 工程提示 |
|---|---|---|
| `org.bouncycastle:bcjmail-jdk18on:1.84` | 是 | 需法务确认 Bouncy Castle License 归类 |
| `org.bouncycastle:bcpkix-jdk18on:1.80` | 是 | 同上 |
| `org.bouncycastle:bcprov-jdk18on:1.80` | 是 | 同上 |
| `org.bouncycastle:bcutil-jdk18on:1.80` | 是 | 同上 |
| `com.github.jai-imageio:jai-imageio-core:1.4.0` | 是 | 需法务确认实际许可证与分发义务 |
| `com.github.junrar:junrar:7.6.0` | 是 | UnRAR 限制性条款，需法务裁定 |
| `com.mysql:mysql-connector-j:9.7.0` | 是 | GPLv2 + FOSS Exception，需法务确认适用性 |
| `com.oracle.database.jdbc:ojdbc11:23.7.0.25.01` | 是 | OTN 许可，需法务确认分发方式 |
| `com.oracle.database.jdbc:ojdbc8:23.7.0.25.01` | 否 | 不在运行时闭包，可优先评估移除 |
| `com.oracle.database.jdbc:ucp:23.7.0.25.01` | 是 | 随 Oracle 数据源能力引入，需产品/法务共同裁定 |
| `com.oracle.database.spring:oracle-spring-boot-starter-ucp:23.4.0` | 是 | 同上 |
| `io.github.x-stream:mxparser:1.2.2` | 是 | 需法务确认实际许可证 |
| `org.jdom:jdom2:2.0.6.1` | 是 | 需法务确认实际许可证 |

### 3.3 关闭条件（缺一不可）

1. 法务/授权人逐项填写 `disposition=approve/reject/replace`，并补齐 `approved_by`、`approved_on`、`expires_on`、`scope`。
2. `reject/replace` 的组件必须真正从依赖树移除，重新生成 inventory，确认 purl 不再出现。
3. `approve` 的条目才允许写入 `scripts/production/license-policy.json` 的 `allowlist`；审批前 allowlist 必须为空。
4. 重跑 `check_license_policy.py`，取得新的 `decision`；依赖升级后重新审批，不得沿用旧结论。

### 3.4 禁止事项

- 不得把 `deny` 类别直接改成 `permissive` 来让门禁变绿。
- 不得预填台账或 allowlist。
- 不得把“51 项 PENDING”写成“已处置”。
- 不得把法务审批外包给工程脚本或本文档。

## 4. 阻断项 B：R-011 / R-029

### 4.1 R-011（CRM 合同回款并发）

现有证据只覆盖隔离 MySQL 8.4.11 上的应用层幂等、CAS、行锁、V117/V121 迁移、真实 BPM/Flowable 事务链和 10 并发。仍缺：

- 生产等价 MySQL 的并发重复请求、软删除永久占用、生产规模 MDL/耗时/复制和回滚；
- 合同余额、退款、冲销的财务对账；
- V118/V121 在真实 CRM+BPM 物理数据源和事务管理器下的生产等价验证。

### 4.2 R-029（BPM 实例与本地回款事务一致性）

现有证据覆盖隔离 MySQL 8.4.11 的共同提交/回滚、同 `businessKey` 10 并发、动态数据源路由/事务绑定 3/3、单机容器 GTID 复制近似 93/93。仍缺：

- 生产物理主从/真实读写分离/多租户物理连接；
- 生产网络、数据库、进程故障注入；
- 生产规模并发、V121 `ADD COLUMN` 的 MDL/表重建/磁盘/复制影响；
- Flowable 运行时/历史表与业务表对账；
- Outbox/补偿/重试/对账闭环；
- V121 空白登记、legacy NULL、lease/expiry/recovery/reconciliation 的处置。

### 4.3 关闭条件

必须在生产等价环境执行真实事务回滚故障注入，核对 Flowable 运行时/历史表与 CRM 业务表，验证幂等重入、并发竞争、重试/补偿和对账。H2、隔离 MySQL、单机双容器、同实例双逻辑库均不能作为关闭证据。

## 5. 其余 G0-G8 阻断

| 门禁 | 仍未闭环的关键项 |
|---|---|
| G0 代码与构建 | 独立复核、提交冻结 |
| G1 安全 | 真实 TLS、外部 Secret、MFA、依赖许可证、安全扫描 P0 |
| G2 数据正确性 | 库存单一真值、财务过账/冲销/期间关闭、QMS 门禁、MES/WMS 状态机 |
| G3 跨域集成 | 生产物理拓扑、故障恢复、Outbox/Inbox/DLQ/补偿/对账 |
| G4 部署 | K8s 服务端 dry-run、真实环境部署、回滚 |
| G5 可观测性 | SLO、告警、on-call 演练 |
| G6 合规 | 等保、个保法、行业合规矩阵与审计证据 |
| G7 运营 | 备份恢复、灾备 RTO/RPO、容量 |
| G8 业务验收 | 六条主流程生产验收 |

详细清单见 `docs/enterprise-platform/05-生产就绪门禁清单.md` 与 `docs/enterprise-platform/10-最终Review与交付审计.md`。

## 6. 责任与下一步

| 工作流 | 需要的责任角色 | 本轮状态 | 下一步 |
|---|---|---|---|
| 许可证裁定 | 法务/授权人 | PENDING | 逐项填写 disposition；工程按结论移除或登记 |
| R-011 生产等价验证 | DBA/财务/CRM | OPEN | 提供生产等价 MySQL、并发窗口、对账口径 |
| R-029 生产等价验证 | DBA/BPM/集成 | OPEN | 提供物理主从/读写分离、故障注入、Flowable 对账 |
| 安全与部署 | 安全/平台/运维 | OPEN | 提供 Secret Manager、TLS、K8s 集群与窗口 |
| 独立复核 | 未指定 | false | 指定独立复核人并签字 |

在上述输入到位前，任何“Go”结论都不成立；本轮不改变统一状态。

## 7. 复现命令

```powershell
# 许可证门禁复核（示例；路径按实际 inventory 调整）
python scripts\production\check_license_policy.py `
  --inventory docs\enterprise-platform\evidence\production\maven-license-inventory-regen-20260928_0127.json `
  --output docs\enterprise-platform\evidence\production\license-gate-recheck-20261021.json --json

# 业务 smoke（需本地后端 48080、租户 9001 mock token）
python Temp\api_smoke.py
```

---

统一状态：`production_ready=false`；`gate3_passed=false`；`independent_review=false`；`R-011=OPEN`；`R-029=OPEN`；`Go/No-Go=No-Go`。
