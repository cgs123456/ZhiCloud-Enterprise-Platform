# Schema Finding 分域 Triage（2026-09-27）

> 证据源：`schema-audit-sandbox-2026-09-27.json`  
> 证据源 SHA-256：`90a1a700bf8d5d0d546db31b332b93399b8534aa6361d01c9a3882385d8b9724`  
> 结论边界：本报告只完成证据分流，不代表生产库已修复或已生产就绪。

## 1. 汇总

- 审计 finding 总数：**248**
- P0_BLOCKER：**154**
- P1_HIGH：**76**
- P2_REVIEW：**18**
- 目标 8 域真实缺表：**22**
- 非目标/可选模块缺表：**129**
- 旧 DDL 口径假阳性：**4**
- 租户索引性能缺口：**75**
- 类型/宽度复核项：**18**

## 2. 目标 8 域真实缺表（必须进入 Flyway）

- `erp_actual_cost`
- `erp_cost_item`
- `erp_cost_variance`
- `erp_mps_plan`
- `erp_mps_plan_detail`
- `erp_standard_cost`
- `erp_work_order_cost`
- `mes_dv_tp_kpi`
- `mes_dv_tp_plan`
- `mes_dv_tp_plan_item`
- `mes_dv_tp_record`
- `mes_pro_rework_order`
- `mes_pro_rework_order_detail`
- `qms_audit_nonconformity`
- `qms_audit_plan`
- `qms_audit_plan_auditor`
- `qms_audit_report`
- `qms_document`
- `qms_document_change_request`
- `qms_document_distribute`
- `qms_instrument`
- `qms_instrument_calibration`

处理方案：

- ERP 7 张：V110 纳入 `erp_cost.sql` 与 `erp_mps.sql`。
- MES 6 张：V111 纳入 TPM 4 张与返工 2 张。
- QMS 9 张：V112 纳入文档控制 3 张、审核管理 4 张、计量器具 2 张。
- 所有新表 `id BIGINT NOT NULL AUTO_INCREMENT`，唯一键按 `(tenant_id, 业务键)` 设计，禁止沿用旧脚本的全局唯一键。
- 不写入模拟业务数据；生产迁移只建结构。

## 3. 非目标/可选模块缺表

这些表来自当前真库未启用的商城、IoT、支付、IM、Quartz、AI/RAG 等模块。它们不能在本轮伪装成“可选”，因为一旦启用对应模块，缺表会直接阻断启动或运行。

- `ai`：1 张
- `airag`：2 张
- `im`：17 张
- `iot`：15 张
- `member`：11 张
- `mp`：8 张
- `pay`：14 张
- `product`：10 张
- `promotion`：23 张
- `qrtz`：11 张
- `report`：1 张
- `trade`：16 张

本轮决策：不批量补迁移。后续按“模块启用、表清单、数据生命周期、迁移回滚、权限隔离”单独立项。

## 4. 旧 DDL 假阳性

- `erp_gl_voucher.no`：V7 权威定义为 voucher_no；旧 sql/mysql/erp.sql 的 no 字段已废弃
- `erp_gl_voucher.voucher_time`：V7 权威定义为 voucher_date；旧 sql/mysql/erp.sql 的 voucher_time 字段已废弃
- `erp_gl_voucher_entry.master_id`：V7 权威定义为 voucher_id；V10 注释已明确旧列 master_id 到 voucher_id 的映射
- `wms_shipment_order.no`：V109 已将全局 uk_no 替换为租户级 uk_tenant_no(tenant_id, no)；审计器仍按旧 DDL 的全局唯一键判定

处理方式：不新增旧字段，不恢复全局唯一键；后续修正审计器的 DDL 源优先级，避免把旧 `sql/mysql` 与 Flyway 权威定义混用。

## 5. 租户索引性能缺口（V113）

共 75 张表已有 `tenant_id`，但缺少以 `tenant_id` 为前缀的索引。该问题不会立即导致 DDL 失败，但在多租户数据量上升后会放大扫描和锁竞争。

- `crm_clue_pool_config`
- `crm_invoice`
- `crm_invoice_line`
- `erp_account_book`
- `erp_accounting_event`
- `erp_accounting_posting`
- `erp_bank_account`
- `erp_cash_flow`
- `erp_consolidation_scope`
- `erp_consolidation_worksheet`
- `erp_cost_allocation`
- `erp_cost_center`
- `erp_currency`
- `erp_exchange_rate`
- `erp_finance_settlement_allocation`
- `erp_fund_plan`
- `erp_gl_account`
- `erp_gl_voucher`
- `erp_gl_voucher_entry`
- `erp_period`
- `erp_period_close`
- `erp_posting_rule`
- `erp_posting_rule_line`
- `erp_profit_center`
- `erp_profitability_analysis`
- `erp_sale_out`
- `erp_sale_out_items`
- `erp_tax_invoice`
- `erp_tax_invoice_line`
- `erp_tax_invoice_source`
- `erp_tax_rate`
- `hr_contract`
- `hr_interview`
- `hr_job_posting`
- `hr_leave_balance`
- `hr_leave_request`
- `hr_leave_type`
- `hr_resume`
- `hr_social_insurance_base`
- `mdm_cross_ref`
- `mes_energy_consumption`
- `oa_announcement`
- `oa_approval_template`
- `oa_document`
- `oa_document_attachment`
- `oa_knowledge_article`
- `oa_knowledge_category`
- `oa_knowledge_comment`
- `oa_knowledge_version`
- `oa_meeting_reservation`
- `oa_meeting_room`
- `oa_reimburse`
- `oa_reimburse_item`
- `oa_schedule`
- `qms_capa_document`
- `qms_electronic_signature_log`
- `qms_fmea_document`
- `qms_fmea_item`
- `qms_inspection_item`
- `qms_inspection_order`
- `qms_inspection_record`
- `qms_msa_measurement`
- `qms_msa_study`
- `qms_ncr_document`
- `qms_ncr_mrb_record`
- `system_dept`
- `system_oauth2_access_token`
- `system_oauth2_refresh_token`
- `system_role`
- `system_role_menu`
- `system_sms_code`
- `system_social_client`
- `system_users`
- `tms_fleet_operation`
- `tms_freight_reconciliation`

迁移要求：仅当表存在、`tenant_id` 列存在且不存在以 `tenant_id` 为第一列的索引时，创建 `idx_tenant_id`；迁移失败即停止，不吞错。大表上线前需评估在线 DDL、MDL、复制延迟和磁盘空间。

## 6. 类型/宽度复核（P2）

18 项属于静态声明与真库类型/宽度差异。当前不把 `bit(1)` 与 `tinyint(1)`、`varchar` 宽度差异直接当作 P0 修复；它们必须先结合真实数据范围、JDBC 映射和查询计划复核，禁止盲目收窄。

## 7. 本轮结论

- 不直接补 151 张缺失表；只对目标 8 域的 22 张真实缺口编写 V110-V112。
- V113 处理 75 张表的租户索引性能缺口。
- 旧 DDL 的 4 项 P0/P1 假阳性不通过新增字段或索引掩盖。
- 在 V110-V113 真库执行、DO/DDL 复验和全量门禁通过前，`production_ready=false`、`gate3_passed=false`。

## 8. 证据文件

- 机器可读明细：`schema-findings-triage-2026-09-27.json`
- 原始审计：`schema-audit-sandbox-2026-09-27.json`
- 原始门禁日志：`gate-schema-audit-sandbox-2026-09-27.log`
