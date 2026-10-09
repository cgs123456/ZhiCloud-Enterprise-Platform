# 库存租户隔离修复证据汇总

- 证据目录：`docs/enterprise-platform/evidence/production/inventory-tenant-isolation-fix-20260929_160657/`
- 汇总日期：2026-09-29
- 当前分支/HEAD：`upgrade/production-readiness` / `d0ebc96c9f864450c9c293094e114aff0e4e8588`
- 结论范围：隔离 H2/MySQL 8.4.11 沙箱环境，不是生产验证。

> 隔离 H2/MySQL 沙箱环境，非生产验证；不能替代生产等价 MySQL、真实租户数据源拓扑、生产规模 DDL/MDL/复制行为或独立复核。

## 1. 执行命令与退出码

| 检查 | 命令 | 退出码 |
|---|---|---|
| 迁移 runner 定向测试 | `mvn -pl zhicloud-server -am '-Dtest=DatabaseMigrationRunnerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` | `0` |
| 库存定向测试 | `mvn -pl zhicloud-framework/zhicloud-spring-boot-starter-biz-inventory -am '-Dtest=InventoryTenantIsolationTest,InventoryServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` | `0` |
| MES 回归 | `mvn -pl zhicloud-module-mes -am test` | `0` |
| SQL 迁移 lint | `python scripts/production/sql_migration_lint.py zhicloud-server/src/main/resources/db/migration/V119__inventory_item_tenant_unique.sql zhicloud-server/src/main/resources/db/verification/V119_verify.sql --json` | `0` |
| 迁移契约检查 | `python scripts/production/check_migration_contract.py --json` | `0` |
| 迁移契约单测 | `python scripts/production/tests/test_check_migration_contract.py` | `0` |
| SQL lint 单测 | `python scripts/production/tests/test_sql_migration_lint.py` | `0` |
| V119 MySQL 沙箱验证 | `pwsh -NoProfile -File docs/enterprise-platform/evidence/production/inventory-tenant-isolation-fix-20260929_160657/v119-mysql-verifier.ps1 -StateFile <redacted-state-file> -EvidenceDir docs/enterprise-platform/evidence/production/inventory-tenant-isolation-fix-20260929_160657 -RepoRoot <repo-root>` | `0` |
| MySQL 沙箱启动 | `pwsh -NoProfile -File scripts/production/mysql-sandbox.ps1 start -RepoRoot <repo-root> -StateFile <state-file> -EvidenceFile ...` | `0` |
| MySQL 沙箱停止 | `pwsh -NoProfile -File scripts/production/mysql-sandbox.ps1 stop -RepoRoot <repo-root> -StateFile <redacted-state> -EvidenceFile ...` | `0` |

命令原文、状态路径和退出码分别保存在同目录的 `*.command.txt`、`*.exit.txt`。

## 2. 测试结果

| 证据 | 结果 | 说明 |
|---|---:|---|
| `database-migration-runner-test.log` | `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` | 日志中的 `FAILED: V117 semantic verification failed` 是 fail-closed 负向测试夹具，最终测试全通过 |
| `inventory-targeted-tests.log` | `InventoryServiceImplTest` 6/0/0/0；`InventoryTenantIsolationTest` 5/0/0/0；合计 `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` | 库存定向测试 |
| `mes-regression.log` | `Tests run: 826, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` | 该回归在 V119 最终 128 字符消息截断修复前完成；V119 是迁移 SQL，不改变 MES Java 测试路径，但如需最强时点证据应重跑 |
| `test-check-migration-contract.log` | `Ran 8 tests in 0.143s`；`OK` | 迁移契约单测 |
| `test-sql-migration-lint.log` | `Ran 13 tests in 0.012s`；`OK` | SQL lint 单测 |
| `v119-sql-migration-lint.json` | `scanned_files=2`、`total_violations=0`、`status=passed` | V119 迁移和验证 SQL |
| `migration-contract-check.json` | `tracked_migrations=30`、`scanned_migrations=126`、`findings=0`、`status=passed` | 契约检查 |

## 3. MySQL 沙箱环境

- 沙箱名：`zhicloud-mysql-sandbox-956ce0c9`
- 数据库：`zhicloud_sandbox`
- 监听：`127.0.0.1:54610`
- 镜像：`mysql:8.4@sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d`
- 服务端版本：`8.4.11`
- 字符集/排序规则：`utf8mb4` / `utf8mb4_0900_ai_ci`
- SQL mode：包含 `STRICT_TRANS_TABLES`
- 默认存储引擎：`InnoDB`
- 隔离范围：synthetic schema/data only
- 说明：`mysql-sandbox-start.json` 记录 `sandbox_database_present=yes`，数据库名以 `v119-mysql-evidence.json` 和启动日志为准；未手改证据。

### V119 场景结果

| 场景 | 结果 | 关键字段 |
|---|---:|---|
| `happy_path_and_tenant_separation` | 通过 | V119 首次/重复执行 exit 0；验证 4/4 PASS；跨租户同组合插入 exit 0；同租户重复 1062 |
| `duplicate_rows_fail_closed` | 通过 | V119 exit 1；MySQL 1644；失败后新索引 count=0 |
| `wrong_new_index_fail_closed` | 通过 | V119 exit 1；MySQL 1644；旧/新错误索引列数均为 5 |
| `wrong_legacy_index_fail_closed` | 通过 | V119 exit 1；MySQL 1644；正确新索引列数 6，错误旧索引列数 4 |

总计：`4/4 passed`、`failed=0`。沙箱已停止：`container_removed=true`、`volume_removed=true`；仅清理本次 `956ce0c9` 容器/卷，未触碰历史沙箱。

## 4. 哈希与契约

### 迁移文件

| 文件 | SHA-256 |
|---|---|
| `zhicloud-server/src/main/resources/db/migration/V119__inventory_item_tenant_unique.sql` | `0e59f833ff2f988f249f8d2f505ea5657a52fa82b34bcf5e5607a3acc0a8281d` |
| `zhicloud-server/src/main/resources/db/verification/V119_verify.sql` | `e9ae7f063547328a901d37d3463eda82f6bc83ea3e8e47bd5d80b634bff952ad` |
| `zhicloud-server/src/main/resources/db/migration/V81__inventory_item.sql` | `5778e0bc7d399a18461c74b9e57d8069db012dc172e381f42f9d082dc6755468` |
| `scripts/production/migration-contract.json` | `6caae9230a7824190aa7f1660a7c5ebda933ff7c577a2466bfba65ef9259e2cf` |

V119 迁移和验证哈希与 `migration-contract.json` 注册值一致。V81 本轮未修改，`git diff --exit-code -- V81__inventory_item.sql` 返回 `0`。

### 证据文件

| 文件 | SHA-256 |
|---|---|
| `v119-mysql-evidence.json` | `cb54f0e53c002f8af118a0789d157db81d491541ff0389a3a07f2d9a69b00290` |
| `v119-mysql-verifier.log` | `c001aefc6e178ec323dcf157a559920f0f9b65fdc8d7122d371b038fa9c7c1cf` |
| `v119-mysql-verifier.exit.txt` | `bde294368bfed77c2cddf8cec271d398aee9cdbab3b26e1059281bd33adb0120` |
| `v119-mysql-verifier.ps1` | `20726f9d0d3c5153263f7afb77339428af84ca4666c53c942be0b39e57fe41dd` |
| `v119-sql-migration-lint.json` | `30d567949e3df89a3374075af7aaebcc72f55a34322a6afdff28816646dfdf95` |
| `migration-contract-check.json` | `283ebb85faed6b6143209ccc5bfd888e8ed743c7b67ade657371c19301d97991` |
| `test-check-migration-contract.log` | `674f158cb8da7507c76eb20d728b4458e258a2f932d69494c5dd660f4c85e605` |
| `test-sql-migration-lint.log` | `4b776611e79397c3c8158033c424e01346eb2451a2bf2a362722ebe1132522c6` |
| `database-migration-runner-test.log` | `7279830e8fb0a48a94ff367201a23069cac7077578a1768f7d67e05861bd7042` |
| `inventory-targeted-tests.log` | `0bcb60a929a54a6fe54666d521b8593fe405630ecd711c519ece0624cb29dcdb` |
| `mes-regression.log` | `8796c4dddfe2402442beff696772178562d3dedbbae8e6f58d1c98fda9ae6a3e` |
| `mysql-sandbox-start.json` | `f767f1d359dacb9eb69fadfaa254b963060d3dd7922d4fdbe311097bd9d62f68` |
| `mysql-sandbox-stop.json` | `2782f619f2809adf42634e8cef91cc30b3ebbe312018ffe3f600387fb15eb71b` |

## 5. 代码与配置修改清单

- `InventoryItemDO` 由 `BaseDO` 改为 `TenantBaseDO`，删除本地重复 `tenantId` 字段。
- `InventoryServiceImpl` 删除新建库存时的 `.tenantId(0L)`。
- 三套 H2 schema 的 `inventory_item` 唯一键改为租户复合唯一键：
  - `zhicloud-framework/zhicloud-spring-boot-starter-biz-inventory/src/test/resources/sql/create_tables.sql`
  - `zhicloud-module-erp/src/test/resources/sql/create_tables.sql`
  - `zhicloud-module-mes/src/test/resources/sql/create_tables.sql`
- `TenantDatabaseInterceptor` 增加表名大小写不敏感回退查找，避免已注册租户表被误判为未知表而绕过拦截。
- `ZhiCloudTenantAutoConfiguration` 的租户拦截器 bean 保持 `@Lazy(false)`，并新增对应初始化测试。
- 新增 `InventoryTenantIsolationTest`，覆盖拦截器链、非 0 租户持久化、跨租户同组合独立、跨租户裸 UPDATE 不可见、缺租户上下文 fail-closed、对账仅读当前租户。
- `MesInventoryReconciliationTest` 删除 `.tenantId(0L)`。
- 库存 starter POM 增加 tenant starter 编译依赖。
- 新增 `V119__inventory_item_tenant_unique.sql` 和 `V119_verify.sql`。
- 更新 `scripts/production/migration-contract.json`，注册 V119 并注明隔离 MySQL 沙箱证据不是生产验证。
- 本目录新增完整命令、退出码、日志、结果 JSON 和汇总文件。

## 6. 未闭环与风险

- 生产等价 MySQL 实例、真实租户数据源拓扑和路由/事务绑定未验证。
- 生产规模 DDL/MDL、复制延迟、锁等待和回滚成本未验证。
- 非 MySQL 数据库方言的等价约束未实现和未实库验证。
- 独立复核未完成。
- T-202 仍待 WMS 权威库存/流水、ERP/MES 投影、绕过 WMS 接口下线或审计、日终对账和差异工单闭环。
- `mes-regression.log` 的时点早于 V119 最终 128 字符错误消息截断修复；该修复仅涉及迁移 SQL，不能替代重跑。
- ERP 本切片没有单独原始 Maven 日志，不得宣称有强原始日志证据。
- 沙箱证据只覆盖合成数据和小规模隔离环境，不得表述为生产验收。

## 7. 门禁状态

本轮不改变以下状态：

- `production_ready=false`
- `gate3_passed=false`
- `independent_review=false`
- `R-011=OPEN`
- `R-029=OPEN`
- Go/No-Go=`No-Go`