-- ======================== 人力资源管理系统（HR）建表脚本 ========================
-- 作者：yudao
-- 说明：覆盖员工档案（hr_employee）/ 部门（hr_department）/ 职位（hr_position）/
--      考勤（hr_attendance）/ 薪资（hr_salary）/ 绩效（hr_performance）
-- 规范：InnoDB / utf8mb4 / utf8mb4_unicode_ci；主键 BIGINT（应用层雪花ID）；
--      统一含 tenant_id/creator/create_time/updater/update_time/deleted 审计字段。

-- ----------------------------
-- 部门表（树形结构）
-- ----------------------------
CREATE TABLE IF NOT EXISTS hr_department (
    id BIGINT PRIMARY KEY COMMENT '主键',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门 ID（0 表示根节点）',
    code VARCHAR(64) NOT NULL COMMENT '部门编码',
    name VARCHAR(128) NOT NULL COMMENT '部门名称',
    leader_id BIGINT COMMENT '部门负责人（员工 ID）',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 启用 20 禁用）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_dept_code (code),
    KEY idx_parent_id (parent_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='HR 部门表';

-- ----------------------------
-- 职位表
-- ----------------------------
CREATE TABLE IF NOT EXISTS hr_position (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '职位编码',
    name VARCHAR(128) NOT NULL COMMENT '职位名称',
    dept_id BIGINT COMMENT '所属部门 ID',
    level TINYINT NOT NULL DEFAULT 10 COMMENT '职级（10 初级 20 中级 30 高级 40 专家 50 管理）',
    base_salary DECIMAL(12,2) COMMENT '基本工资',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_position_code (code),
    KEY idx_dept_id (dept_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='HR 职位表';

-- ----------------------------
-- 员工档案表
-- ----------------------------
CREATE TABLE IF NOT EXISTS hr_employee (
    id BIGINT PRIMARY KEY COMMENT '主键',
    emp_no VARCHAR(64) NOT NULL COMMENT '工号',
    name VARCHAR(64) NOT NULL COMMENT '姓名',
    gender TINYINT NOT NULL DEFAULT 10 COMMENT '性别（10 男 20 女）',
    birth_date DATE COMMENT '出生日期',
    id_card VARCHAR(32) COMMENT '身份证号',
    phone VARCHAR(32) COMMENT '联系电话',
    email VARCHAR(128) COMMENT '邮箱',
    dept_id BIGINT COMMENT '部门 ID',
    position_id BIGINT COMMENT '职位 ID',
    hire_date DATE COMMENT '入职日期',
    leave_date DATE COMMENT '离职日期',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 在职 20 离职 30 停薪）',
    employment_type TINYINT NOT NULL DEFAULT 10 COMMENT '用工类型（10 全职 20 兼职 30 实习 40 外包）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_emp_no (emp_no),
    KEY idx_dept_id (dept_id),
    KEY idx_position_id (position_id),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) COMMENT='HR 员工档案表';

-- ----------------------------
-- 考勤记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS hr_attendance (
    id BIGINT PRIMARY KEY COMMENT '主键',
    employee_id BIGINT NOT NULL COMMENT '员工 ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    check_in_time DATETIME COMMENT '签到时间',
    check_out_time DATETIME COMMENT '签退时间',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 正常 20 迟到 30 早退 40 缺勤 50 加班）',
    overtime_hours DECIMAL(6,2) COMMENT '加班时长（小时）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_employee_id (employee_id),
    KEY idx_attendance_date (attendance_date),
    KEY idx_tenant_id (tenant_id)
) COMMENT='HR 考勤记录表';

-- ----------------------------
-- 薪资记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS hr_salary (
    id BIGINT PRIMARY KEY COMMENT '主键',
    employee_id BIGINT NOT NULL COMMENT '员工 ID',
    salary_month VARCHAR(7) NOT NULL COMMENT '薪资月份（yyyyMM）',
    base_salary DECIMAL(12,2) COMMENT '基本工资',
    overtime_pay DECIMAL(12,2) COMMENT '加班费',
    bonus DECIMAL(12,2) COMMENT '奖金',
    deduction DECIMAL(12,2) COMMENT '扣款',
    social_insurance DECIMAL(12,2) COMMENT '社保',
    housing_fund DECIMAL(12,2) COMMENT '公积金',
    tax DECIMAL(12,2) COMMENT '个税',
    net_salary DECIMAL(12,2) COMMENT '实发工资',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 20 已审核 30 已发放）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_employee_id (employee_id),
    KEY idx_salary_month (salary_month),
    KEY idx_tenant_id (tenant_id)
) COMMENT='HR 薪资记录表';

-- ----------------------------
-- 绩效记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS hr_performance (
    id BIGINT PRIMARY KEY COMMENT '主键',
    employee_id BIGINT NOT NULL COMMENT '员工 ID',
    period VARCHAR(8) NOT NULL COMMENT '考核周期（yyyyMM 月度 或 yyyyQn 季度，如 2024Q1）',
    score DECIMAL(6,2) COMMENT '考核得分',
    grade TINYINT COMMENT '考核等级（10 A 20 B 30 C 40 D）',
    evaluator_id BIGINT COMMENT '考核人 ID',
    evaluation_date DATE COMMENT '考核日期',
    comment TEXT COMMENT '考核意见',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_employee_id (employee_id),
    KEY idx_period (period),
    KEY idx_tenant_id (tenant_id)
) COMMENT='HR 绩效记录表';

-- ----------------------------
-- 字典初始化
-- ----------------------------

-- HR 性别字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('HR 性别', 'hr_gender', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'HR 员工性别字典');
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '男', '10', 'hr_gender', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '女', '20', 'hr_gender', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- HR 员工状态字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('HR 员工状态', 'hr_employee_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'HR 员工状态字典');
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '在职', '10', 'hr_employee_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '离职', '20', 'hr_employee_status', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '停薪', '30', 'hr_employee_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- HR 用工类型字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('HR 用工类型', 'hr_employment_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'HR 用工类型字典');
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '全职', '10', 'hr_employment_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '兼职', '20', 'hr_employment_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '实习', '30', 'hr_employment_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '外包', '40', 'hr_employment_type', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- HR 职位级别字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('HR 职位级别', 'hr_position_level', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'HR 职位级别字典');
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '初级', '10', 'hr_position_level', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '中级', '20', 'hr_position_level', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '高级', '30', 'hr_position_level', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '专家', '40', 'hr_position_level', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(5, '管理', '50', 'hr_position_level', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- HR 考勤状态字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('HR 考勤状态', 'hr_attendance_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'HR 考勤状态字典');
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '正常', '10', 'hr_attendance_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '迟到', '20', 'hr_attendance_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '早退', '30', 'hr_attendance_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '缺勤', '40', 'hr_attendance_status', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(5, '加班', '50', 'hr_attendance_status', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- HR 薪资状态字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('HR 薪资状态', 'hr_salary_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'HR 薪资状态字典');
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '草稿',   '10', 'hr_salary_status', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '已审核', '20', 'hr_salary_status', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '已发放', '30', 'hr_salary_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- HR 绩效等级字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('HR 绩效等级', 'hr_performance_grade', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'HR 绩效等级字典');
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, 'A', '10', 'hr_performance_grade', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, 'B', '20', 'hr_performance_grade', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, 'C', '30', 'hr_performance_grade', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, 'D', '40', 'hr_performance_grade', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);