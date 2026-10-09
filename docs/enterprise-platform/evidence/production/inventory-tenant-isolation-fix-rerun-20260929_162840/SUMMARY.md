# 库存租户隔离修复重跑证据汇总（最终时点）

- 证据目录：`docs/enterprise-platform/evidence/production/inventory-tenant-isolation-fix-rerun-20260929_162840/`
- 汇总日期：2026-09-29
- 当前分支/HEAD：`upgrade/production-readiness` / `d0ebc96c9f864450c9c293094e114aff0e4e8588`
- 结论范围：隔离 H2/Maven 与 Python 静态门禁；不是生产验证。
- MySQL 口径：V119 MySQL 8.4.11 沙箱 4/4 是前序目录 `inventory-tenant-isolation-fix-20260929_160657` 的隔离证据引用，不是本重跑目录重新执行的 MySQL 证据。

> 本目录是独立重跑证据，不覆盖、不改写原证据目录。H2、Maven、Python 和隔离 MySQL 沙箱结果均不能替代生产等价 MySQL、真实租户数据源拓扑、生产故障/补偿/对账、生产规模迁移或独立复核。

## 1. 本轮时点与变更纳入

- 本轮重跑发生在 2026-09-29，执行证据文件时间戳范围为 16:28–16:36（本机 Asia/Singapore）；摘要文件在本轮校验后另行生成。
- `zhicloud-framework/zhicloud-spring-boot-starter-biz-inventory/pom.xml` 末尾多余空行清理已纳入本轮时点。
- V119 最终 128 字符错误消息截断修复已纳入本轮时点；`mes-regression.log` 是修复后最终工作树时点的重跑，消除了原先“MES 回归早于 V119 最终修复”的时点缺口。
- ERP 本切片没有单独原始 Maven 日志，不得宣称存在强原始日志证据。
- 本目录不包含新的 MySQL 原始执行日志；MySQL 结论仅引用前序隔离沙箱证据。

## 2. 执行命令与退出码

| 检查 | 命令 | 退出码 | 证据文件 |
|---|---|---:|---|
| 库存定向测试 | `mvn -pl zhicloud-framework/zhicloud-spring-boot-starter-biz-inventory -am '-Dtest=InventoryTenantIsolationTest,InventoryServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` | `0` | `inventory-targeted-tests.log` / `.exit.txt` |
| 迁移 runner 定向测试 | `mvn -pl zhicloud-server -am '-Dtest=DatabaseMigrationRunnerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` | `0` | `database-migration-runner-test.log` / `.exit.txt` |
| MES 回归 | `mvn -pl zhicloud-module-mes -am test` | `0` | `mes-regression.log` / `.exit.txt` |
| SQL 迁移 lint | `python scripts/production/sql_migration_lint.py zhicloud-server/src/main/resources/db/migration/V119__inventory_item_tenant_unique.sql zhicloud-server/src/main/resources/db/verification/V119_verify.sql --json` | `0` | `sql-migration-lint.json` / `.exit.txt` |
| 迁移契约检查 | `python scripts/production/check_migration_contract.py --json` | `0` | `migration-contract-check.json` / `.exit.txt` |
| 迁移契约单测 | `python scripts/production/tests/test_check_migration_contract.py` | `0` | `test-check-migration-contract.log` / `python-tests.exit.txt` |
| SQL lint 单测 | `python scripts/production/tests/test_sql_migration_lint.py` | `0` | `test-sql-migration-lint.log` / `python-tests.exit.txt` |

## 3. 重跑结果

| 证据 | 结果 | 说明 |
|---|---:|---|
| `inventory-targeted-tests.log` | `InventoryServiceImplTest` 6/0/0/0；`InventoryTenantIsolationTest` 5/0/0/0；合计 `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` | 库存租户隔离与库存服务定向测试 |
| `database-migration-runner-test.log` | `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` | 迁移 runner 定向测试 |
| `mes-regression.log` | `Tests run: 826, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` | 最终工作树时点 MES 回归 |
| `test-check-migration-contract.log` | `Ran 8 tests in 0.101s`；`OK` | 迁移契约单测 |
| `test-sql-migration-lint.log` | `Ran 13 tests in 0.008s`；`OK` | SQL lint 单测 |
| `sql-migration-lint.json` | `scanned_files=2`、`total_violations=0`、`status=passed` | V119 迁移和验证 SQL |
| `migration-contract-check.json` | `tracked_migrations=30`、`scanned_migrations=126`、`findings=[]`、`status=passed` | 契约检查；JSON 同时声明 SHA-256 锁定不等同于 MySQL 执行证明，语义回填仍需预生产数据验证 |

## 4. 哈希与契约一致性

| 文件 | SHA-256 |
|---|---|
| `zhicloud-server/src/main/resources/db/migration/V119__inventory_item_tenant_unique.sql` | `0e59f833ff2f988f249f8d2f505ea5657a52fa82b34bcf5e5607a3acc0a8281d` |
| `zhicloud-server/src/main/resources/db/verification/V119_verify.sql` | `e9ae7f063547328a901d37d3463eda82f6bc83ea3e8e47bd5d80b634bff952ad` |
| `scripts/production/migration-contract.json` | `6caae9230a7824190aa7f1660a7c5ebda933ff7c577a2466bfba65ef9259e2cf` |

- V119 迁移和验证文件的实际 SHA-256 与 `migration-contract.json` 注册值一致。
- 原证据目录 `inventory-tenant-isolation-fix-20260929_160657/SUMMARY.md` 的实际 SHA-256 为 `b13627891a017d147a8a66c87e5c027cb34cf78644e26217583103e7d1156332`，与其 `SUMMARY.md.sha256` companion 一致，原证据未被覆盖。
- 原证据目录 `v119-mysql-evidence.json` 的实际 SHA-256 为 `cb54f0e53c002f8af118a0789d157db81d491541ff0389a3a07f2d9a69b00290`，其场景汇总为 `4/4 passed`、`failed=0`。
- `V81__inventory_item.sql` 的实际 SHA-256 为 `5778e0bc7d399a18461c74b9e57d8069db012dc172e381f42f9d082dc6755468`，本轮未修改。
- 本目录摘要的 SHA-256 记录在同目录 `SUMMARY.md.sha256`。

### 本目录证据文件 SHA-256

| 文件 | SHA-256 |
|---|---|
| `database-migration-runner-test.command.txt` | `cf7743d74f9a15269c25eb581cf40226e8700aa5312ef5770a42d2e8a370ca35` |
| `database-migration-runner-test.exit.txt` | `13bf7b3039c63bf5a50491fa3cfd8eb4e699d1ba1436315aef9cbe5711530354` |
| `database-migration-runner-test.log` | `911fce626bfc359189c120f76ca60e49cff4dc5ec59933b269950dbc6abd3919` |
| `inventory-targeted-tests.command.txt` | `372f17ac4452926d7814d64eb248ce0bd4fd8e150a0013d31922cc24bf17febb` |
| `inventory-targeted-tests.exit.txt` | `13bf7b3039c63bf5a50491fa3cfd8eb4e699d1ba1436315aef9cbe5711530354` |
| `inventory-targeted-tests.log` | `32961126da130c4f0fa8cb7201f03ce7fc59bcaee749ad9a536bd6caf2b098d2` |
| `mes-regression.command.txt` | `2b1815d073585081140d8b966d0c6dd8509b5452ea27fee623b68bfce1c02252` |
| `mes-regression.exit.txt` | `13bf7b3039c63bf5a50491fa3cfd8eb4e699d1ba1436315aef9cbe5711530354` |
| `mes-regression.log` | `3d6a8c503ce01e51ddb8e2c47f1e587f37a7257b5ff61df4f223d34ff92e7b1c` |
| `migration-contract-check.command.txt` | `14698196b892618e785a7ae1abc6b249c1159ae8fad4a3294b02e2791a9facdd` |
| `migration-contract-check.exit.txt` | `13bf7b3039c63bf5a50491fa3cfd8eb4e699d1ba1436315aef9cbe5711530354` |
| `migration-contract-check.json` | `7cbca019e621e6c90ea0b019054a03f821f5d0a87c43c0b78aec0640558425f4` |
| `python-tests.command.txt` | `e5d7ee6e3c91388d2a05b7a6212d872bcb8e2e47a01d9a47fd89833731fed351` |
| `python-tests.exit.txt` | `c2c351836fba320e33ac8370ecf98939477bd05facac28373779d39dbd8861a0` |
| `sql-migration-lint.command.txt` | `f3dc6c0da597e9fcd67be9fdc5685f62fe06e52b182a0f0addfebccec5cebaf1` |
| `sql-migration-lint.exit.txt` | `13bf7b3039c63bf5a50491fa3cfd8eb4e699d1ba1436315aef9cbe5711530354` |
| `sql-migration-lint.json` | `74b7b5868a487326f56753eb34d0b3485756c82d0ce7cabaae2ed28392a055ec` |
| `test-check-migration-contract.log` | `fe59a880c90824ed0a149d5ceeb572a8389d41ccb7ca8dfa7e44b8d74efec45a` |
| `test-sql-migration-lint.log` | `de34224f8bc367dd4e8eb799dd85f7609c6d78619f2be48a8674f891a06daac9` |

## 5. MySQL 隔离证据引用（前序目录）

- 前序沙箱名：`zhicloud-mysql-sandbox-956ce0c9`
- 数据库：`zhicloud_sandbox`
- 镜像：`mysql:8.4@sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d`
- 服务端版本：`8.4.11`
- V119 场景：`happy_path_and_tenant_separation`、`duplicate_rows_fail_closed`、`wrong_new_index_fail_closed`、`wrong_legacy_index_fail_closed`，合计 `4/4 passed`、`failed=0`。
- 该沙箱仅使用合成数据和小规模隔离环境；证据来自前序目录，不是生产 MySQL，也不是本目录重新执行。
- 本轮复核：`956ce0c9` 容器不存在、对应卷不存在；历史沙箱 `87323300` 和 `f29c293d` 的容器及卷仍存在，未触碰。

## 6. 未闭环与风险

- 生产等价 MySQL、真实读写分离、多租户数据源拓扑和事务绑定未验证。
- 生产故障注入、补偿、对账和差异工单闭环未验证。
- 生产规模 DDL/MDL、复制延迟、锁等待、回滚成本和迁移预演未验证。
- 非 MySQL 数据库方言的等价约束未实现、未实库验证。
- WMS 权威库存/流水、ERP/MES 投影、绕过 WMS 接口的下线或审计、日终对账仍未闭环。
- 独立复核未完成。
- 本目录所有 Maven 结果均为本地 H2/隔离测试环境结果，不能作为生产验收证据。
- 全仓既有未提交修改和未跟踪证据较多；本切片只声明本轮目标路径的 `git diff --check` 退出码为 `0`，未执行 `git add`、commit、reset 或 clean。

## 7. 门禁状态

本轮不改变以下状态：

- `production_ready=false`
- `gate3_passed=false`
- `independent_review=false`
- `R-011=OPEN`
- `R-029=OPEN`
- Go/No-Go=`No-Go`