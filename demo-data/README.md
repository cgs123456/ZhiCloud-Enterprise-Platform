# demo-data —— 模拟数据（开发 / 测试 / 演示）

> 状态：**MySQL 数据级实库验收通过，应用层端到端仍未验收**。2026-09-28 已在 MySQL 8.4.9
> 临时库执行 clean baseline、导入 118 张表 / 2,725 行、逐表计数、六条跨域关联查询和 cleanup 清零；
> 证据见 `docs/enterprise-platform/evidence/production/demo-data-mysql-20260928_133559.json`。
> 未调用后端 API、未执行审批/权限/并发/幂等/财务过账/质量放行，仍禁止导入生产。

---

## 1. 目的与边界

| 项 | 说明 |
| --- | --- |
| 目的 | 为 ERP / MES / WMS / QMS / CRM / HR / OA / TMS 一体化平台的开发、联调、演示生成可复现的模拟主数据 |
| 允许环境 | 本地开发库、CI 临时库、演示环境 |
| **禁止环境** | **生产库、预发库、任何存有真实业务数据的库** |
| 方言 | 当前仅 **MySQL**（其余 7 套方言暂只有仓库原有的 `*_demo_seed.sql` 账号种子） |
| 覆盖范围 | **118 张表**：19 张主数据（267 行）+ 99 张业务/支撑表（2,458 行），small 档合计 2,725 行；MySQL 实库数据级覆盖 ERP / MES / WMS / QMS / CRM / HR / OA / TMS。六条应用层端到端场景仍未执行（见 §6） |
| 数据性质 | 全部虚构：租户 9001 起、`DEMO_` 前缀、邮箱 `.invalid`（RFC 2606）、无真实证件号 / 手机号 |

隔离设计（三重保险，缺一不可）：

1. `tenant_id >= 9001`（默认 9001，可用 `--tenant` 改）；
2. 名称 / 编码统一 `DEMO_` 前缀；
3. 审计列 `creator = 'demo-seed'` —— 清理脚本以此为第二谓词。

---

## 2. 使用顺序

```bash
# 都在仓库根目录执行；PowerShell 请先 $env:PYTHONIOENCODING='utf-8'

# 1) 先从 sql/mysql/*.sql 建表 DDL 抽取结构快照（474 张表）
python demo-data/generators/schema_snapshot.py --output demo-data/schema/schema-snapshot.json

# 2) 再生成模拟主数据（默认 seed=20260928 tenant=9001 scale=small）
python demo-data/generators/generate_master_data.py

# 3) 生成业务单据/支撑数据（依赖上一步的主数据 JSON）
python demo-data/generators/generate_business_documents.py

# 可选参数（两个生成器都支持；业务单据生成器依赖同租户/同 seed 的主数据）
python demo-data/generators/generate_master_data.py --tenant 9002 --scale large --seed 20260928
python demo-data/generators/generate_business_documents.py --tenant 9002 --scale large --seed 20260928
#   --tenant  租户段，默认 9001（同时决定主键号段，跨租户不撞号）
#   --scale   small | medium | large  （基数倍率 1 / 5 / 20）
#   --seed    随机种子，默认 20260928（同 seed 必得同产物）
#   --output  覆盖输出目录，默认 demo-data/schema | demo-data/sql/mysql
```

清理（**先读脚本头部，确认连接目标后再执行**）：

```
demo-data/cleanup/cleanup_demo.sql
```

---

## 3. 产物与校验值

| 产物 | 说明 | SHA-256 |
| --- | --- | --- |
| `schema/schema-snapshot.json` | 表结构快照（474 表 / 13 warnings） | `c251e1b93bfd9f21899af08909fd0934b8d84d9e7cadeb52ffe294e354a4a4ed` |
| `sql/mysql/demo_master_data.sql` | 主数据 INSERT（267 行 / 19 表，含 `BEGIN;…COMMIT;`） | `d7be937714aeb9ad4c344b2705224387d18c640c42ac47de15479cbd27c7c789` |
| `schema/demo_master_data.json` | 同批数据的 JSON 形态，供接口层 / 前端 mock 使用 | `3a6a06cdf4a5de4ffece26deda01fcb9db2c02bde7ae57277461692ecf5e9552` |
| `schema/demo_master_data_summary.json` | manifest：seed / tenant / scale / 各表行数 / 未核实字段 / 上述 SHA / 状态 | `f65e85e069907c83da6a45a1e4445d91bed649734533fe5b525d5048bc2f1300` |
| `sql/mysql/demo_business_documents.sql` | 业务/支撑 INSERT（2,458 行 / 99 表，含 `BEGIN;…COMMIT;`） | `782b84832d15f7b7dbedf40f3e8eaf2e9310e492718c94becb9a852331c20801` |
| `schema/demo_business_documents.json` | 同批业务/支撑数据的 JSON 形态，供接口层 / 前端 mock 使用 | `76beb44954ad4ce7a42769c6941c6b27338f199d790c2defb640ea29263cfa11` |
| `schema/demo_business_documents_summary.json` | 业务/支撑数据 manifest：依赖主数据 SHA / 各表行数 / 未核实字段 / 上述 SHA / 状态 | `a5c7dc5a95995831640c3c19b70d14518ec0e0a4471d42edc7160ba0849c33b5`（2026-09-30 复测更正：原钉 `18d37081…` 对应 HEAD 版本，工作树内该 manifest 已被重新生成但 README 未同步。本次只更正钉值使其与当前字节一致，**不代表那次重生成本身已获证据或审核**） |

> 上述 SHA 对应 **默认参数**（seed=20260928 / tenant=9001 / scale=small）。
> 改任一参数 SHA 都会变；以 manifest 内记录的值为准。

---

## 4. 覆盖的表（small 档，合计 2,725 行）

### 4.1 主数据（19 张 / 267 行）

| 域 | 表 | 行数 |
| --- | --- | --- |
| ERP | erp_product_category / erp_product_unit / erp_product | 8 / 8 / 24 |
| ERP | erp_customer / erp_supplier / erp_warehouse | 12 / 10 / 4 |
| WMS | wms_warehouse / wms_zone / wms_location | 4 / 12 / 48 |
| WMS | wms_item_category / wms_item / wms_merchant | 6 / 20 / 8 |
| CRM | crm_customer | 15 |
| HR | hr_department / hr_position / hr_employee | 8 / 10 / 40 |
| TMS | tms_carrier / tms_driver / tms_vehicle | 6 / 12 / 12 |

### 4.2 业务/支撑表（99 张 / 2,458 行）

| 域 | 表数 | 行数 | 静态覆盖示例 |
| --- | ---: | ---: | --- |
| ERP | 27 | 341 | 销售订单、采购订单、库存单据、收付款 |
| MES | 18 | 263 | 工单、任务、派工、报工、BOM/工艺路线 |
| WMS | 13 | 308 | ASN、收货、库存、出库、波次、拣货、移库 |
| QMS | 8 | 57 | 检验单、NCR、CAPA、投诉、仪器校准 |
| CRM | 15 | 157 | 线索、商机、合同、回款、发票、跟进 |
| HR | 7 | 1,224 | 请假、考勤、合同、薪资、绩效 |
| OA | 6 | 49 | 公告、报销、会议、日程 |
| TMS | 5 | 59 | 运单、节点、运费、对账 |

`--scale large` 会放大两个生成器的数据量；实际行数以各自 manifest 的 `row_total` 为准。

---

## 5. 确定性与防错

- 唯一随机源：`random.Random(seed)`；**禁止**用 `hash()` 参与排序或 ID 分配（`PYTHONHASHSEED` 会影响结果）。
- 落盘统一 `write_bytes(utf-8)`，规避 CRLF 导致的 SHA 漂移；`payload_sha256`（内容）与 `file_sha256`（字节）分开记录。
- 主键按 `(租户段, 表序号, 行序)` 分配：`ID_START=910000`、`ID_TABLE_STRIDE=1000`、`ID_TENANT_STRIDE=1000000`，扩量 / 跨租户不撞号。
- **fail-closed 校验**：列名必须存在于结构快照；快照中 NOT NULL 的列必须全部被覆盖；主键行数越界直接抛错退出（不静默产出残缺数据）。
- 枚举取值遵循快照与迁移脚本：`deleted` 为 `BIT(1)` 写 `b'0'`、`TINYINT(1)` 写 `0`；TMS 状态用 **10/20/30**；ERP/WMS 主数据 `status=0`；HR 用 10/20/30/40/50；`wms_location.status=10`（空闲）；`crm_customer.version=0`（乐观锁）。

---

> 口径说明：两个 manifest（`schema/demo_master_data_summary.json`、`schema/demo_business_documents_summary.json`）里的 `status` 固定为 `STATIC_ONLY —— 未在任何数据库执行过，可执行性未验证`，这是**生成期口径**：生成器无法知道自己产出的 SQL 后来是否被导入过，因此该字段一旦改成"已执行"就会在每次重跑时变成假话。实库执行状态一律记在本 README 与 `docs/enterprise-platform/evidence/production/demo-data-mysql-*.json`，两者不构成矛盾，也不得互相替代。（原 `R-G4-4` 即此误读，现按口径说明关闭，不改被哈希钉住的产物。）

## 6. 已知缺口（诚实清单）

1. **六条应用层端到端场景尚未验收**。99 张业务/支撑表 / 2,458 行已实库导入，六条跨域数据级关联均有非零结果；但接口调用、页面操作、审批流、权限、并发、幂等、对账、审计和失败恢复均未执行。
2. **数据级实库验收已完成，性能和约束边界未完成**。导入、逐表计数、最小场景关联和 cleanup 已在 MySQL 8.4.9 临时库验证；大数据量导入耗时、索引影响、锁等待、死锁恢复和跨方言约束仍未验证。
3. **量级偏小**。默认 small 档主数据 267 行、业务/支撑数据 2,458 行，远低于 doc 07 §5.1 的建议量级；可用 `--scale large` 放大，但大数据量下的导入耗时与索引影响同样未实测。
4. **未核实字段**（刻意留 NULL 或标注，不做无依据猜测，见两个 manifest 的 `unverified_fields`）：主数据 manifest 有 7 项；业务单据 manifest 有 20 项。
5. **单方言**。仅 MySQL；dm / highgo / kingbase / opengauss / oracle / postgresql / sqlserver 七套方言的模拟数据尚未生成（类型与字面量差异未适配，如 `b'0'` 只在 MySQL 有效）。
6. **清理和不变量覆盖不对称**。`cleanup/cleanup_demo.sql` 覆盖 118 张表，并已于 2026-09-28 在 MySQL 8.4.9 临时库实跑通过（证据 `docs/enterprise-platform/evidence/production/demo-data-mysql-20260928_133559-cleanup.log`，汇总 `demo-data-mysql-20260928_133559.json` `status=passed`）；仍未验证的是清理后的生产级回滚语义与跨方言行为。`expectations/invariants.sql` 目前只覆盖 19 张主数据表，尚未覆盖 99 张业务/支撑表，也没有场景级对账报告。

---

## 7. 验收口径（MySQL 沙箱已跑过一轮；剩余项见下）

`demo-data/expectations/invariants.sql` 提供静态只读校验 SQL（租户隔离、DEMO_ 前缀、creator 计数、BIT/TINYINT 取值、唯一键去重、审计列非空、编码租户内唯一）。
该文件目前只覆盖 19 张主数据表；业务/支撑表的逐表计数、六条最小关联和 cleanup 清零已由 `verify_demo_data_mysql.ps1` 实库验证，但更严格的库存、质量、财务和并发不变量仍需继续补齐。
