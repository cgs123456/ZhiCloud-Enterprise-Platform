# 安装脚本（sql/mysql）与 Flyway 迁移链 Schema 双源漂移报告

| 项 | 内容 |
|---|---|
| 报告日期 | 2026-09-28 |
| 数据源 | docs/enterprise-platform/evidence/production/demo-schema-snapshot-20260928_030403.json（payload_sha256 37df29503a427702fb61f62da187de6e75ba55c636cafe5ec664502ed573ac3d） |
| 比对两侧 | A：sql/mysql/*.sql（安装脚本）；B：zhicloud-server/src/main/resources/db/migration（Flyway 迁移链） |
| 证据类别 | **静态 DDL 文本解析对比，不是实库 information_schema 对比** |
| 关联风险 | R-030（本文新增）、R-012（ERP 过账/期间关闭）、R-013（租户隔离）、R-016（Flyway checksum 漂移） |
| 该证据不能说明什么 | 不能说明任一真实数据库实例的当前结构；不能替代实库 information_schema 双向 diff；不覆盖非 MySQL 方言 |

## 1. 总量口径

| 口径 | 数量 | 说明 |
|---|---:|---|
| 快照合并表总数 | 474 | 两侧并集，重复定义按列并集合并 |
| 仅来自迁移链 | 463 | 其中 11 张表在快照中标记为 install_script_only，其余归属迁移链 |
| 定义在 sql/mysql 中的表 | 331 | 333 条 CREATE TABLE 语句去重后 |
| 两侧同名表 | 320 | 331 − 11（仅安装脚本的 Quartz 表） |
| 存在列级差异的同名表 | 14 | 见第 3 节；其中 1 条为解析误报 |
| 列集合一致的同名表 | 306 | 320 − 14 |
| **只在迁移链、安装脚本完全没有** | **143** | 463 − 320，说明 sql/mysql 是**过时基线** |
| **只在安装脚本、迁移链完全没有** | **11** | 全部为 Quartz 表 |

## 2. 关键结论（按严重度排序）

### 2.1 P1：11 张 Quartz 表只存在于安装脚本，迁移链 0 处定义

- 表：qrtz_blob_triggers、qrtz_calendars、qrtz_cron_triggers、qrtz_fired_triggers、qrtz_job_details、qrtz_locks、qrtz_paused_trigger_grps、qrtz_scheduler_state、qrtz_simple_triggers、qrtz_simprop_triggers、qrtz_triggers。
- 唯一定义来源：sql/mysql/quartz.sql。迁移链全文检索 QRTZ 命中数 = **0**。
- pplication-local.yaml:9 排除 QuartzAutoConfiguration；pplication-local.yaml:109 设置 initialize-schema: NEVER（注释写明"我们手动创建表结构"）。
- pplication-prod.yaml **完全没有 quartz 配置段**，无 job-store-type=jdbc。
- 推论：走 Helm/K8s 迁移 Job 的生产路径**不会创建 Quartz 表**；如果生产启用 JDBC JobStore，调度器启动即失败，除非人工执行 sql/mysql/quartz.sql。这条与"生产 DDL 只由迁移链产生"的假设直接冲突。

### 2.2 P1：ERP 财务凭证表在两条源上是两套命名，安装脚本侧已废弃

sql/mysql/erp.sql:725 的 rp_gl_voucher 只有 12 列，使用旧命名 
o（凭证号）、oucher_time（凭证日期）；迁移链侧使用 oucher_no、oucher_date，并另有 12 列（ccount_book_id、pproved_by、pproved_time、ttachment_count、credit_total、debit_total、period_code、prepared_by、emark、oucher_date、oucher_no、oucher_type）。

代码侧证据：ERP 模块 + server 模块中 oucherNo 命中 **26** 处，oucherTime 命中 **0** 处，oucher_time 在 XML 中命中 **0** 处。

结论：
o / oucher_time / rp_gl_voucher_entry.master_id 是**遗留命名，当前代码不使用**。危害方向是"从 sql/mysql 初始化的库"：少 12 个代码实际依赖的列、多 5 个废弃列，ERP 凭证功能直接不可用。这正好落在 R-012（ERP 过账/期间关闭）的关键路径上。

### 2.3 P2：143 张表只在迁移链，sql/mysql 已严重过时——但仍有 4 条活跃路径以它为输入

| 引用位置 | 用途 | 影响 |
|---|---|---|
| script/docker/docker-compose.yml:30-48 | 把 sql/mysql/*.sql 挂进 MySQL docker-entrypoint-initdb.d | 本地/演示容器初始化走的是过时脚本 |
| scripts/production/init_bootstrap_admin.py:40 | "mysql": "sql/mysql/zhicloud_platform_bootstrap.sql" | bootstrap runner 的执行体来自安装脚本侧 |
| scripts/production/check_tenant_isolation.py:285 | --ddl-dir 默认 <repo>/sql/mysql | **租户隔离审计（R-013）默认只扫 331 张表，漏掉 143 张迁移链独有表** |
| scripts/production/schema_audit.py:57 | DEFAULT_LEGACY_DDL_GLOB = "sql/mysql/*.sql" | 结构审计基线同样不完整 |

其中最需要立刻处理的是第三条：R-013（租户隔离）当前的审计覆盖面被低估，任何"全表扫描通过"的结论在补齐 DDL 目录前都不成立。

### 2.4 P2：快照自带 13 条重复定义警告，快照本身不等于任一真实环境结构

V10__erp_ddl.sql（rp_period、rp_period_close）、V11__wms_ddl.sql（wms_wave_order、wms_wave_order_detail）、V13__qms_ddl.sql（qms_inspection_item、qms_inspection_order、qms_inspection_record、qms_capa_document）、V14__aimultiagent_ddl.sql（2 张）、V73__ai_ddl.sql（1 张）、sql/mysql/wms_wave.sql（2 张）共 13 处重复 CREATE TABLE，快照按**列并集**合并。因此第 3 节的列清单是"合并结果"，不是任一真实库的结构。

### 2.5 P3：静态解析对动态 SQL 可见性不一致，漂移清单存在双向误报

- V62__wms_asn_pick_batch.sql 用**硬编码字面量**拼 ALTER TABLE ... ADD COLUMN，解析器可见 → 正确识别为"仅迁移链列"。
- V24__wms_fefo_slotting_fields.sql 用 CONCAT('ALTER TABLE \', tableName, '\ ADD COLUMN \', columnName, ...) + 存储过程 CALL，列名是变量，解析器**不可见** → 把 wms_receipt_order_detail.production_date / xpiry_date 误报为"仅安装脚本列"，而这两列实际已由 V24 添加。
- 同类不可见风险存在于 V21（存储过程回填）、V80（rp_stock.version 动态 SQL）、V116（ctive_username 生成列）。

因此：**第 3 节的 54 行列差异中，"仅迁移链"方向的可信度高于"仅安装脚本"方向**；wms_receipt_order_detail 的两条属于已确认的假阳性。唯一可靠口径是在实库上做 information_schema 双向 diff。

### 2.6 已确认的解析误报

wms_item 的"仅安装脚本列 if"来自 sql/mysql/zhicloud_platform_bootstrap.sql:30 的 IF v_zhicloud_bootstrap_user_count > 1 THEN，被解析器当作列名吞入，**不是真实列**。

## 3. 逐列差异明细（54 列 / 13 张表）

方向统一为"该列只在迁移链中出现"，即 sql/mysql 滞后。

| 表 | 列 | 引入迁移（最低版本定位） | 分类 |
|---|---|---|---|
| `erp_customer` | `credit_limit` | `V40__erp_customer_credit_fields.sql` | 安装脚本滞后 |
| `erp_customer` | `credit_period` | `V40__erp_customer_credit_fields.sql` | 安装脚本滞后 |
| `erp_customer` | `used_credit` | `V40__erp_customer_credit_fields.sql` | 安装脚本滞后 |
| `erp_gl_account` | `closing_credit` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_account` | `closing_debit` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_account` | `current_credit` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_account` | `current_debit` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_account` | `is_leaf` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_account` | `opening_credit` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_account` | `opening_debit` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_account` | `remark` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_account` | `sort` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_account` | `type` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `account_book_id` | `V30__erp_account_book.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `approved_by` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `approved_time` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `attachment_count` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `credit_total` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `debit_total` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `period_code` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `prepared_by` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `remark` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `voucher_date` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `voucher_no` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher` | `voucher_type` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher_entry` | `account_code` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher_entry` | `account_name` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher_entry` | `sort` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_gl_voucher_entry` | `voucher_id` | `V7__erp_gl_account_voucher.sql` | 安装脚本滞后 |
| `erp_period` | `closed_by` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period` | `closed_time` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period` | `end_date` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period` | `month` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period` | `remark` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period` | `start_date` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period` | `year` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period_close` | `adjustment_amount` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period_close` | `executed_by` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period_close` | `executed_time` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period_close` | `period_code` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period_close` | `process_status` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period_close` | `remark` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period_close` | `summary` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_period_close` | `type` | `V6__erp_period_close.sql` | 安装脚本滞后 |
| `erp_stock` | `version` | `V80__erp_stock_version_column.sql（动态 SQL）` | 安装脚本滞后 |
| `mes_dv_machinery` | `iot_device_pk` | `V54__mes_scada_piecework.sql` | 安装脚本滞后 |
| `mes_dv_machinery` | `protocol_type` | `V54__mes_scada_piecework.sql` | 安装脚本滞后 |
| `qms_fmea_item` | `action_priority` | `V21__qms_fmea_action_priority.sql` | 安装脚本滞后 |
| `system_operate_log` | `current_hash` | `V9__operate_log_hash_chain.sql` | 安装脚本滞后 |
| `system_operate_log` | `prev_hash` | `V9__operate_log_hash_chain.sql` | 安装脚本滞后 |
| `system_users` | `active_username` | `V116__system_users_active_username_unique.sql（STORED 生成列）` | 安装脚本滞后 |
| `wms_inventory_batch` | `shelf_life_days` | `V62__wms_asn_pick_batch.sql（动态 SQL）` | 安装脚本滞后 |
| `wms_inventory_batch` | `supplier_batch_no` | `V62__wms_asn_pick_batch.sql（动态 SQL）` | 安装脚本滞后 |
| `wms_item` | `abc_classification` | `V44__wms_check_order_ext.sql` | 安装脚本滞后 |

## 4. 只在安装脚本中出现的列（5 列 / 4 张表）

| 表 | 列 | 迁移链是否存在 | 代码是否使用 | 判定 |
|---|---|---|---|---|
| rp_gl_voucher | 
o | 否（链上为 oucher_no） | 否（oucherNo 26 处；
o 未单独核对） | 遗留命名，建议删除 |
| rp_gl_voucher | oucher_time | 否（链上为 oucher_date） | 否（oucherTime 0 处） | 遗留命名，建议删除 |
| rp_gl_voucher_entry | master_id | 否（链上为 oucher_id） | 否 | 遗留命名，建议删除 |
| wms_receipt_order_detail | production_date | 是（V24 动态 SQL 添加） | 是（DO 含 productionDate） | **解析器假阳性**，非真实漂移 |
| wms_receipt_order_detail | xpiry_date | 是（V24 动态 SQL 添加） | 是（DO 含 xpiryDate） | **解析器假阳性**，非真实漂移 |
| wms_item | if | — | — | **解析器误报**（bootstrap 脚本 IF ... THEN） |

## 5. 建议处置顺序

1. **决策单一真值来源**（阻塞项）：建议确立"Flyway 迁移链为唯一生产真值"，sql/mysql 降级为"本地/演示快速起库"，并在 sql/mysql/README 或等价位置显式标注"非生产真值、已滞后 143 张表"。在此之前，任何"474 表基线"的表述都应附带本报告的漂移口径。
2. **用实库 information_schema 重做 diff**（消除 2.5 的双向误报）：分别用"迁移链初始化"和"sql/mysql 初始化"各建一个 MySQL 8.4.9 库，导出 information_schema.COLUMNS 做双向差集，产出机器可读 JSON 证据，替换静态快照作为漂移判定依据。
3. **补齐 Quartz 真值**（对应 2.1）：二选一并写进文档——(a) 新增 Flyway 迁移固化 11 张 QRTZ 表；(b) 明确生产不使用 JDBC JobStore，删除 sql/mysql/quartz.sql 的挂载与引用。
4. **修正 R-013 审计覆盖面**（对应 2.3）：check_tenant_isolation.py 的默认 DDL 目录改为迁移链，或显式传 --ddl-dir 并覆盖 474 张表后重跑，重出证据。
5. **清理遗留列**（对应 2.2）：删除 sql/mysql 中 
o / oucher_time / master_id，或在迁移链中补一条重命名映射说明，避免后续误以为安装脚本可用。
6. **消除重复定义**（对应 2.4）：13 处重复 CREATE TABLE 逐个确认是否应合并/删除，避免"列并集"掩盖真实差异。

## 6. 复核要求

本报告由静态解析生成，结论 2.1 / 2.2 / 2.3 / 2.4 / 2.6 已逐条在文件级复核；结论 2.5 的假阳性判定已定位到 V24 与 V62 的具体实现差异。**第 3 节的逐列归属尚未用实库验证**，在第 5 节第 2 项完成前不应作为删除或改表的唯一依据。