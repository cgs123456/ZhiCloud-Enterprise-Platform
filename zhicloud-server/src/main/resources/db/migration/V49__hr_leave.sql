-- HR 假期管理 DDL
CREATE TABLE hr_leave_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL COMMENT '假期类型(年假/事假/病假/调休/婚假/产假/丧假)',
  code VARCHAR(32) NOT NULL,
  is_paid TINYINT DEFAULT 1 COMMENT '1 带薪 0 不带薪',
  deduct_salary TINYINT DEFAULT 0 COMMENT '是否扣薪',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0,
  UNIQUE KEY uk_leave_type_code (code)
);
CREATE TABLE hr_leave_balance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  leave_type_id BIGINT NOT NULL,
  year INT NOT NULL,
  total_days DECIMAL(6,1) NOT NULL COMMENT '年度总额度',
  used_days DECIMAL(6,1) DEFAULT 0 COMMENT '已用',
  remaining_days DECIMAL(6,1) COMMENT '剩余(冗余)',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0,
  UNIQUE KEY uk_emp_type_year (employee_id, leave_type_id, year)
);
CREATE TABLE hr_leave_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  leave_type_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  days DECIMAL(6,1) NOT NULL COMMENT '请假天数',
  reason VARCHAR(500),
  status TINYINT DEFAULT 0 COMMENT '0 待审批 1 已批准 2 已驳回 3 已撤销',
  approver_id BIGINT,
  approve_time DATETIME,
  approve_remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE INDEX idx_leave_request_employee ON hr_leave_request(employee_id);
CREATE INDEX idx_leave_request_status ON hr_leave_request(status);