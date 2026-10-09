# 30-V107 事件租户唯一键负向验证证据

> 生成时间：2026-09-26  
> 目标迁移：`V107__event_tenant_unique_keys.sql`  
> 目标核验：`V107_verify.sql`  
> 结论：**已覆盖的 6 个场景全部通过**；该结论仅证明 V107 在指定 MySQL 8.4.9 沙箱中具备预期的 fail-closed 与租户边界行为，**不代表生产就绪**。

## 1. 为什么补这层证据

原有 `v107-mysql-sandbox.json` 只证明“干净库上迁移成功、结构正确”。这不足以证明：

1. 存量同租户重复事件会被阻断，而不是被静默删除或覆盖；
2. 已存在同名但列顺序错误、非唯一索引时，迁移不会误判为完成；
3. `tenant_id` 缺失时迁移不会继续执行；
4. 不同租户使用相同 `event_id` 时确实可以共存；
5. 失败后重跑不会重复破坏结构。

因此新增 `scripts/production/verify_v107_negative.py`，每次使用 `zhicloud_sandbox*` 隔离库执行，结束后自动删除。

## 2. 执行命令

```powershell
python scripts\production\verify_v107_negative.py `
  --mysql-client 'C:\Users\Administrator\mysql84-sandbox\mysql-8.4.9-winx64\bin\mysql.exe' `
  --host 127.0.0.1 --port 53306 --user root `
  --database zhicloud_sandbox_v107_negative `
  --output 'C:\Users\Administrator\Desktop\zhicloud\docs\enterprise-platform\evidence\production\v107-negative-mysql-sandbox.json'
```

执行环境：MySQL `8.4.9`，`sql_mode` 含 `STRICT_TRANS_TABLES`，事务隔离级别 `REPEATABLE-READ`。

## 3. 场景与结果

| 场景 | 输入 | 预期 | 实测 |
|---|---|---|---|
| Outbox 同租户重复 | 先移除旧全局唯一键以模拟脏数据，再插入同租户同 `event_id` 两行 | 迁移非 0，出现 V107 duplicate SIGNAL，且不创建新索引 | 通过，exit=1 |
| Inbox 同租户同消费者重复 | 模拟同租户、同消费者、同事件两行 | 迁移非 0，出现 V107 duplicate SIGNAL，且不创建新 inbox 索引 | 通过，exit=1 |
| 新索引列顺序错误 | 预建 `uk_event_tenant_event(event_id,tenant_id)` | 迁移非 0，不静默替换错误索引，旧索引保留 | 通过，exit=1 |
| 新索引不是唯一索引 | 预建同名普通索引 `(tenant_id,event_id)` | 迁移非 0，不把普通索引当唯一索引接受 | 通过，exit=1 |
| `tenant_id` 缺失 | 删除 Outbox 的 `tenant_id` | 迁移在索引变更前失败 | 通过，exit=1 |
| 正向租户边界与幂等 | 首次迁移、重复迁移、只读核验、跨租户同 `event_id`、同租户重复插入 | 前四项成功；同租户重复被 `1062` 拒绝；`V107=PASS` | 通过 |

机读证据：`docs/enterprise-platform/evidence/production/v107-negative-mysql-sandbox.json`  
证据 SHA-256：`10a0409424e5e6f68899547489702341d8c1c0d16421b61a8a2532797b6ab505`  
场景汇总：`6 passed / 0 failed`。

## 4. 回归与契约验证

```text
python -m unittest scripts.production.tests.test_verify_v107_negative \
  scripts.production.tests.test_sql_migration_lint \
  scripts.production.tests.test_check_migration_contract -v
Ran 24 tests ... OK

python scripts\production\check_migration_contract.py
PASSED: tracked=19 scanned=115 findings=0
```

迁移与核验文件哈希与 `scripts/production/migration-contract.json` 完全一致：

- V107 migration：`bddfbb8b8476ed7e0358a65f16d18e1f9b7276e8dfdf7903fda708bb9f21a78b`
- V107 verification：`95875b1552bdad7e6b3cd0aa85fd8f849f867a7c156da7bf94450bbd2f4f8d12`

## 5. Review 结论与未覆盖边界

1. **PASS 不等于生产就绪。** 当前证据是定向行为验证，不是完整 Flyway 迁移链认证；`flyway_parser_covered=false`。
2. MySQL DDL 隐式提交。Inbox 重复场景中，Outbox 新唯一键已经建立后才在 Inbox 阶段失败；这是预期 partial DDL，依赖迁移幂等重跑收敛，不能宣称“失败无副作用”。
3. 生产执行前仍必须完成：备份可恢复验证、大表在线 DDL 评估、复制延迟与 MDL/磁盘评估、低峰窗口审批。
4. 本验证没有覆盖真实业务并发写入、主从复制、在线流量下的锁等待，也没有证明应用层所有事件路径都传入正确 `tenant_id`。
5. 下一项仍为 P0-2：修复 ERP→WMS 事件链路固定租户 `0` 的问题，并先写失败测试。

## 6. 禁止事项

本证据中的库名、事件编号和插入行均为 `FAKE-SIMULATED-DATA`，仅存在于已删除的隔离沙箱库中。`production_import_allowed=false`，不得把该脚本或数据当作生产初始化/修复脚本执行。
