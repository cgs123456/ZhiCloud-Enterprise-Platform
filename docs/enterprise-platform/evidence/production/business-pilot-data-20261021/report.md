# 业务演示数据与 API smoke 复核（2026-10-21）

> 环境：本地 `local` profile，MySQL 容器 `zc-live-mysql`，业务库 `zhicloud_sandbox_v140`，租户 `9001`。
> 本文所有数据均为合成演示数据，**禁止导入生产**。本文不改变 `production_ready=false`、`gate3_passed=false`、
> `independent_review=false`、`R-011=OPEN`、`R-029=OPEN`、`Go/No-Go=No-Go`。

## 1. 目的

补齐弱数据页面的演示数据，逐项核实 API smoke 的 47 个失败项，并复跑全量主列表 smoke。目标是区分：

- 页面/接口真实缺陷；
- smoke 脚本缺少父单/上下文参数；
- 外部依赖（微信公众号 AppID/密钥）不可用。

## 2. 数据补充

| 模块 | 补充内容 | 验证 |
|---|---|---|
| MES | 最近 7 天 `mes_pro_feedback`（`status=4`） | `/mes/home-statistics/production-trend?days=7` 每天有产量 |
| ERP | 今日/昨日/本月 `erp_sale_out`、`erp_purchase_in` 演示单据 | 销售/采购统计今日、昨日、本月非空 |
| HRM | 2026-10 月度考勤（21/20.5 天，少量迟到/旷工） | `/hrm/attendance/statistics/month-record-page?year=2026&month=10` `total=798` |
| HRM | 员工 925000 的 2026-11 月度工资记录与工资条 | `/hrm/portal/salary/slip/list` 非空 |
| QMS | 6 条 `qms_audit_plan` 计划 | `/qms/audit-plan/page` `total=7` |

SQL 见同目录：

- `seed_visual_data.sql`
- `seed_salary_slip.sql`
- `backfill_erp_total_price.sql`

## 3. ERP 数据/统计缺陷修复

### 3.1 生成器漏写 `total_price`

`demo-data/generators/generate_business_documents.py` 在销售订单/出库/退货、采购订单/入库/退货 6 类单据中只写
`total_amount`，未写 `total_price`。ERP 统计查询使用 `total_price`，导致演示库统计为空/偏小。

修复：6 类单据同时写入 `total_price`；重新生成 `demo-data/sql/mysql/demo_business_documents.sql` 及 schema/summary；
对存量库执行 `backfill_erp_total_price.sql`，把 `total_price IS NULL` 的行回填为 `total_amount`。

### 3.2 `today/month/year` 统计缺少上界

`ErpSaleStatisticsController` / `ErpPurchaseStatisticsController` 的 `today`、`month`、`year` 查询只传开始时间、
结束时间传 `null`，会把未来日期的演示单据计入。已改为：

- today：`[today, today.plusDays(1))`
- month：`[month, month.plusMonths(1))`
- year：`[year, year.plusYears(1))`

yesterday 原本已用 `[yesterday, today)`，未改。

修复后实测（2026-10-21）：

```json
/erp/sale-statistics/summary     {"todayPrice":12800.0,"yesterdayPrice":9600.0,"monthPrice":299686.65,"yearPrice":1609707.93}
/erp/purchase-statistics/summary {"todayPrice":6800.0,"yesterdayPrice":5200.0,"monthPrice":233593.83,"yearPrice":2318209.21}
```

今日/昨日金额只包含当天单据，不再把 2027 年的演示单据计入。

## 4. API smoke 复核

### 4.1 原始失败

基线：`pages_with_primary_list=325`，其中 47 个失败。逐项调用后：

| 结论 | 数量 | 说明 |
|---|---:|---|
| `OK_WITH_CONTEXT` | 45 | 补父单/上下文参数后 `code=0`，接口本身正常 |
| `EXTERNAL_WECHAT_CONFIG` | 2 | `/mp/free-publish/page`、`/mp/draft/page` 调用微信公众平台返回 AppID 无效；本地无有效 AppID/密钥，属外部依赖 |
| `REAL_FAIL` | 0 | 无 500/404/非参数校验失败 |

逐项明细：`smoke_fail_verification.md` / `.csv`。

### 4.2 复跑

`api_smoke.py` 已加入 47 个接口的上下文参数映射，并区分 `EXTERNAL`。复跑结果：

```text
pages_with_primary_list=325 pass=323 fail=0 external=2
```

`external=2` 仅表示微信公众号外部调用无法在本地完成，不代表接口代码通过；需要有效 AppID/密钥后复测。

## 5. README 截图

2026-10-21 使用 1920×1080 视口重新截取，内容区裁剪为 1706×880，覆盖 8 个页面：

| 文件 | 路由 |
|---|---|
| `docs/images/screenshots/erp-dashboard.png` | `/erp/home` |
| `docs/images/screenshots/mes-dashboard.png` | `/mes/mes/home/index` |
| `docs/images/screenshots/wms-overview.png` | `/wms/home` |
| `docs/images/screenshots/qms-ncr.png` | `/qms/ncr` |
| `docs/images/screenshots/crm-customer-analysis.png` | `/crm/statistics/customer` |
| `docs/images/screenshots/hrm-employee.png` | `/hrm/employee` |
| `docs/images/screenshots/oa-reimburse.png` | `/oa/reimburse` |
| `docs/images/screenshots/tms-shipment.png` | `/tms/shipment` |

同时修正 OA 报销、TMS 运单、QMS NCR 列表的长枚举标签/技术 ID 列导致的表格横向挤压，截图不再出现标签重叠或列头被固定操作列截断。

## 6. 边界

- 本文是业务可达性证据，不是生产验收。
- smoke 使用本地 mock token 与演示租户，未覆盖真实认证、权限边界、并发、故障、生产规模。
- 微信接口需要真实公众号凭据；本轮未伪造成功结果。
- 演示数据仅用于截图与页面可达性，禁止导入生产。

---

统一状态：`production_ready=false`；`gate3_passed=false`；`independent_review=false`；`R-011=OPEN`；`R-029=OPEN`；`Go/No-Go=No-Go`。
