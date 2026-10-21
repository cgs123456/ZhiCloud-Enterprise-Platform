# API smoke 失败项逐个核实

对每个失败接口补充其所需的父单/上下文参数后重新调用。

| # | 模块 | 页面 | 接口 | 使用参数 | code | 结论 |
|---|---|---|---|---|---|---|
| 1 | AI 大模型 | Agent编排执行 | `/aimultiagent/checkpoint/list` | `executionLogId=1` | 0 | OK_WITH_CONTEXT |
| 2 | ERP 系统 | 主生产计划 | `/erp/mps-plan/detail/list` | `planId=1` | 0 | OK_WITH_CONTEXT |
| 3 | ERP 系统 | 供应商评估 | `/erp/supplier-evaluation/item/list` | `evaluationId=1` | 0 | OK_WITH_CONTEXT |
| 4 | IM 即时通讯 | 群聊列表 | `/im/manager/group/member/list` | `groupId=1` | 0 | OK_WITH_CONTEXT |
| 5 | MES 系统 | 班组设置 | `/mes/cal/team-member/page` | `teamId=1` | 0 | OK_WITH_CONTEXT |
| 6 | MES 系统 | 排班日历 | `/mes/cal/calendar/list` | `startDay=2026-10-01%2000:00:00&endDay=2026-10-31%2023:59:59&queryType=TYPE&calendarType=1` | 0 | OK_WITH_CONTEXT |
| 7 | MES 系统 | 常见缺陷 | `/mes/qc/defect-record/page` | `qcType=1&qcId=1` | 0 | OK_WITH_CONTEXT |
| 8 | MES 系统 | 生产工单 | `/mes/pro/work-order-bom/page` | `workOrderId=5960000` | 0 | OK_WITH_CONTEXT |
| 9 | MES 系统 | 工序流转 | `/mes/pro/card-process/page` | `cardId=1` | 0 | OK_WITH_CONTEXT |
| 10 | MES 系统 | 质检方案 | `/mes/qc/template/indicator/page` | `templateId=1` | 0 | OK_WITH_CONTEXT |
| 11 | MES 系统 | 过程检验 | `/mes/qc/ipqc/line/page` | `ipqcId=1` | 0 | OK_WITH_CONTEXT |
| 12 | MES 系统 | 出货检验 | `/mes/qc/oqc/line/page` | `oqcId=1` | 0 | OK_WITH_CONTEXT |
| 13 | MES 系统 | 退料检验 | `/mes/qc/rqc/line/page` | `rqcId=1` | 0 | OK_WITH_CONTEXT |
| 14 | MES 系统 | 到货通知 | `/mes/wm/arrival-notice-line/page` | `noticeId=1` | 0 | OK_WITH_CONTEXT |
| 15 | MES 系统 | 生产领料 | `/mes/wm/product-issue-line/page` | `issueId=1` | 0 | OK_WITH_CONTEXT |
| 16 | MES 系统 | 采购退货 | `/mes/wm/return-vendor-line/page` | `returnId=1` | 0 | OK_WITH_CONTEXT |
| 17 | MES 系统 | 生产退料 | `/mes/wm/return-issue-line/page` | `issueId=1` | 0 | OK_WITH_CONTEXT |
| 18 | MES 系统 | 杂项出库 | `/mes/wm/misc-issue-line/page` | `issueId=1` | 0 | OK_WITH_CONTEXT |
| 19 | MES 系统 | 外协发料 | `/mes/wm/outsource-issue-line/page` | `issueId=1` | 0 | OK_WITH_CONTEXT |
| 20 | MES 系统 | 杂项入库 | `/mes/wm/misc-receipt-line/page` | `receiptId=1` | 0 | OK_WITH_CONTEXT |
| 21 | MES 系统 | 装箱管理 | `/mes/wm/package-line/page` | `packageId=1` | 0 | OK_WITH_CONTEXT |
| 22 | MES 系统 | 转移调拨 | `/mes/wm/transfer-line/list` | `transferId=1` | 0 | OK_WITH_CONTEXT |
| 23 | MES 系统 | 盘点方案 | `/mes/wm/stocktaking-plan-param/page` | `planId=1` | 0 | OK_WITH_CONTEXT |
| 24 | OA管理 | 文章评论 | `/oa/knowledge-comment/list` | `articleId=1` | 0 | OK_WITH_CONTEXT |
| 25 | WMS 系统 | 库存统计 | `/wms/inventory/list` | `warehouseId=916000` | 0 | OK_WITH_CONTEXT |
| 26 | 人力资源 HRM | 员工管理 | `/hrm/employee/certificate/list` | `employeeId=925000` | 0 | OK_WITH_CONTEXT |
| 27 | 人力资源 HRM | 员工薪资 | `/hrm/salary/change-record/list` | `employeeId=925000` | 0 | OK_WITH_CONTEXT |
| 28 | 人力资源 HRM | 月度工资表 | `/hrm/salary/month-employee-record/list` | `monthRecordId=32` | 0 | OK_WITH_CONTEXT |
| 29 | 人力资源 HRM | 工资条发放 | `/hrm/portal/salary/slip/list` | `startMonth=2026-01&endMonth=2026-12` | 0 | OK_WITH_CONTEXT |
| 30 | 公众号管理 | 粉丝管理 | `/mp/user/page` | `accountId=1` | 0 | OK_WITH_CONTEXT |
| 31 | 公众号管理 | 消息管理 | `/mp/message/page` | `accountId=1` | 0 | OK_WITH_CONTEXT |
| 32 | 公众号管理 | 图文发表记录 | `/mp/free-publish/page` | `accountId=1` | 1006006000 | EXTERNAL_WECHAT_CONFIG |
| 33 | 公众号管理 | 图文草稿箱 | `/mp/draft/page` | `accountId=1` | 1006007000 | EXTERNAL_WECHAT_CONFIG |
| 34 | 公众号管理 | 素材管理 | `/mp/material/page` | `accountId=1` | 0 | OK_WITH_CONTEXT |
| 35 | 公众号管理 | 菜单管理 | `/mp/menu/list` | `accountId=1` | 0 | OK_WITH_CONTEXT |
| 36 | 公众号管理 | 自动回复 | `/mp/auto-reply/page` | `accountId=1` | 0 | OK_WITH_CONTEXT |
| 37 | 基础设施 | 代码生成 | `/infra/codegen/table/list` | `dataSourceConfigId=1` | 0 | OK_WITH_CONTEXT |
| 38 | 基础设施 | 主子表（标准） | `/infra/demo03-student-erp/demo03-course/page` | `studentId=1` | 0 | OK_WITH_CONTEXT |
| 39 | 系统管理 | 用户管理 | `/system/user/list` | `ids=1` | 0 | OK_WITH_CONTEXT |
| 40 | 财务管理 FMS | 账套管理 | `/fms/config/account-user/list` | `accountSetId=1` | 0 | OK_WITH_CONTEXT |
| 41 | 财务管理 FMS | 凭证字管理 | `/fms/config/voucher-word/list` | `accountSetId=1` | 0 | OK_WITH_CONTEXT |
| 42 | 财务管理 FMS | 常用摘要管理 | `/fms/config/digest/list` | `accountSetId=1` | 0 | OK_WITH_CONTEXT |
| 43 | 财务管理 FMS | 辅助核算管理 | `/fms/config/auxiliary-item/list` | `accountSetId=1` | 0 | OK_WITH_CONTEXT |
| 44 | 财务管理 FMS | 凭证模板管理 | `/fms/config/voucher-template/list` | `accountSetId=1` | 0 | OK_WITH_CONTEXT |
| 45 | 财务管理 FMS | 财务指标管理 | `/fms/config/finance-indicator/list` | `accountSetId=1` | 0 | OK_WITH_CONTEXT |
| 46 | 质量管理 QMS | MSA 分析 | `/qms/msa/measurement/list` | `studyId=1` | 0 | OK_WITH_CONTEXT |
| 47 | 质量管理 QMS | 不合格品处理 | `/qms/ncr/mrb-record/list` | `ncrId=6360000` | 0 | OK_WITH_CONTEXT |

## 说明

- `OK_WITH_CONTEXT`：补充上下文参数后返回 `code=0`，接口本身正常，原 smoke 缺少参数。
- `EXPECTED_PARAM_VALIDATION`：补充常见参数后仍被参数校验拦截；通常是父单不存在或该接口不是页面主列表接口，不是 500/404 缺陷。
- `EXTERNAL_WECHAT_CONFIG`：接口调用微信公众平台失败，原因是本地 `mp_account` 的 AppID/密钥无效；属于外部凭证配置问题，不是代码缺陷。
- `REAL_FAIL`：非参数校验类失败，需要单独排查。
