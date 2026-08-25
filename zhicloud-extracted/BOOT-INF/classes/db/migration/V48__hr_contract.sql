-- HR 合同管理 DDL
CREATE TABLE hr_contract (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL COMMENT '员工 ID',
  contract_no VARCHAR(64) NOT NULL COMMENT '合同编号',
  contract_type TINYINT NOT NULL COMMENT '1 固定期限 2 无固定期限 3 完成任务 4 实习',
  start_date DATE NOT NULL COMMENT '合同开始日期',
  end_date DATE COMMENT '合同结束日期(无固定期限为空)',
  sign_date DATE NOT NULL COMMENT '签订日期',
  probation_end_date DATE COMMENT '试用期结束日期',
  position_id BIGINT COMMENT '岗位 ID',
  department_id BIGINT COMMENT '部门 ID',
  salary DECIMAL(20,4) COMMENT '合同约定薪资',
  status TINYINT DEFAULT 0 COMMENT '0 生效 1 即将到期 2 已到期 3 已终止 4 已续签',
  file_url VARCHAR(500) COMMENT '合同附件',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0,
  UNIQUE KEY uk_contract_no (contract_no)
);
CREATE INDEX idx_contract_employee ON hr_contract(employee_id);
CREATE INDEX idx_contract_status ON hr_contract(status);
CREATE INDEX idx_contract_end_date ON hr_contract(end_date);
