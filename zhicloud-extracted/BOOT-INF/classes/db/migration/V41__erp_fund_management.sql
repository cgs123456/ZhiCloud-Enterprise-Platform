-- =====================================================
-- P0-3 资金管理：银行账户、资金计划、现金流记录
-- =====================================================
CREATE TABLE IF NOT EXISTS erp_bank_account (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
  account_no VARCHAR(64) NOT NULL COMMENT '账号',
  account_name VARCHAR(128) NOT NULL COMMENT '账户名称',
  bank_name VARCHAR(128) NULL COMMENT '开户行',
  bank_branch VARCHAR(128) NULL COMMENT '开户支行',
  balance DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '账户余额',
  currency_id BIGINT NULL COMMENT '币种编号',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 启用 1 禁用）',
  remark VARCHAR(255) NULL COMMENT '备注',
  creator VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) COMMENT='ERP 银行账户';

CREATE TABLE IF NOT EXISTS erp_fund_plan (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
  plan_period VARCHAR(16) NOT NULL COMMENT '计划期间（如 2026-07）',
  plan_type TINYINT NOT NULL COMMENT '计划类型（10 收款 20 付款）',
  amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '计划金额',
  bank_account_id BIGINT NULL COMMENT '银行账户编号',
  remark VARCHAR(255) NULL COMMENT '备注',
  creator VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) COMMENT='ERP 资金计划';

CREATE TABLE IF NOT EXISTS erp_cash_flow (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
  biz_type TINYINT NOT NULL COMMENT '业务类型（10 收款 20 付款）',
  amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '金额',
  bank_account_id BIGINT NULL COMMENT '银行账户编号',
  biz_order_id BIGINT NULL COMMENT '业务单据编号',
  biz_order_type VARCHAR(32) NULL COMMENT '业务单据类型',
  occur_date DATE NOT NULL COMMENT '发生日期',
  remark VARCHAR(255) NULL COMMENT '备注',
  creator VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) COMMENT='ERP 现金流记录';