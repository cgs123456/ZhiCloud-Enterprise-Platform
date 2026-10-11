-- V163: HR/HRM 融合第一步——员工子域 9 张表（异动记录/离职信息/附件/证书/联系人/经历×3/字段配置）
-- 来源: 前端 api/hrm/employee/{change-record,quit-info,file,certificate,contact,education-experience,work-experience,training-experience,config} 契约逐字段生成。
-- 工资卡（salary-card）为员工单卡，列已内置 hr_employee（V161 的 salary_card_* 五列），不另建表。
-- 全部租户表（TenantBaseDO）；日期时间统一 datetime（epoch 毫秒序列化）。
-- 幂等: CREATE TABLE IF NOT EXISTS。

CREATE TABLE IF NOT EXISTS `hr_employee_change_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '异动记录编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `type` tinyint NOT NULL COMMENT '异动类型：4转正 5调岗 6晋升 7降级 8转为全职 9再入职',
  `reason` tinyint DEFAULT NULL COMMENT '异动原因：1组织架构调整 2个人申请 3工作安排 4违规违纪 5绩效不达标 6身体原因 7不适应岗位',
  `old_dept_id` bigint DEFAULT NULL COMMENT '原部门编号',
  `old_dept_name` varchar(64) DEFAULT NULL COMMENT '原部门名称（快照）',
  `new_dept_id` bigint DEFAULT NULL COMMENT '新部门编号',
  `new_dept_name` varchar(64) DEFAULT NULL COMMENT '新部门名称（快照）',
  `old_post_name` varchar(64) DEFAULT NULL COMMENT '原职位',
  `new_post_name` varchar(64) DEFAULT NULL COMMENT '新职位',
  `old_post_level` varchar(64) DEFAULT NULL COMMENT '原岗位职级',
  `new_post_level` varchar(64) DEFAULT NULL COMMENT '新岗位职级',
  `old_work_address` varchar(128) DEFAULT NULL COMMENT '原工作地点',
  `new_work_address` varchar(128) DEFAULT NULL COMMENT '新工作地点',
  `old_leader_employee_id` bigint DEFAULT NULL COMMENT '原直属上级员工编号',
  `old_leader_employee_name` varchar(64) DEFAULT NULL COMMENT '原直属上级姓名（快照）',
  `new_leader_employee_id` bigint DEFAULT NULL COMMENT '新直属上级员工编号',
  `new_leader_employee_name` varchar(64) DEFAULT NULL COMMENT '新直属上级姓名（快照）',
  `probation` int DEFAULT NULL COMMENT '试用期（月，转全职时记录）',
  `effect_time` datetime DEFAULT NULL COMMENT '生效日期',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_change_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工异动记录表';

CREATE TABLE IF NOT EXISTS `hr_employee_quit_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '离职信息编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `plan_quit_time` datetime DEFAULT NULL COMMENT '计划离职时间',
  `apply_quit_time` datetime DEFAULT NULL COMMENT '申请离职时间',
  `salary_settlement_time` datetime DEFAULT NULL COMMENT '薪资结算时间',
  `type` tinyint DEFAULT NULL COMMENT '离职类型：1主动 2被动 3退休',
  `reason` tinyint DEFAULT NULL COMMENT '离职原因（1-16，见前端 HrmEmployeeQuitReason）',
  `old_employee_status` tinyint DEFAULT NULL COMMENT '离职前员工状态',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_quit_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工离职信息表';

CREATE TABLE IF NOT EXISTS `hr_employee_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `type` tinyint NOT NULL COMMENT '附件类型（11-25 分组，见前端 HrmEmployeeFileType）',
  `url` varchar(512) NOT NULL COMMENT '附件地址',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_file_employee_type` (`employee_id`, `type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工档案附件表';

CREATE TABLE IF NOT EXISTS `hr_employee_certificate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '证书编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `name` varchar(128) NOT NULL COMMENT '证书名称',
  `level` varchar(64) DEFAULT NULL COMMENT '证书级别',
  `no` varchar(64) DEFAULT NULL COMMENT '证书编码',
  `start_time` datetime DEFAULT NULL COMMENT '有效开始日期',
  `end_time` datetime DEFAULT NULL COMMENT '有效结束日期',
  `issuing_authority` varchar(128) DEFAULT NULL COMMENT '发证机构',
  `issuing_time` datetime DEFAULT NULL COMMENT '发证日期',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_cert_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工证书表';

CREATE TABLE IF NOT EXISTS `hr_employee_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `name` varchar(64) NOT NULL COMMENT '联系人姓名',
  `relation` varchar(32) DEFAULT NULL COMMENT '关系',
  `phone` varchar(32) DEFAULT NULL COMMENT '联系人电话',
  `work_unit` varchar(128) DEFAULT NULL COMMENT '联系人工作单位',
  `post_name` varchar(64) DEFAULT NULL COMMENT '联系人职务',
  `address` varchar(255) DEFAULT NULL COMMENT '联系人地址',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_contact_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工紧急联系人表';

CREATE TABLE IF NOT EXISTS `hr_employee_education_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '教育经历编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `education` tinyint DEFAULT NULL COMMENT '学历（1-12，见 hrm_employee_education 字典）',
  `graduate_school` varchar(128) DEFAULT NULL COMMENT '毕业院校',
  `major` varchar(64) DEFAULT NULL COMMENT '专业',
  `admission_time` datetime DEFAULT NULL COMMENT '入学日期',
  `graduation_time` datetime DEFAULT NULL COMMENT '毕业日期',
  `teaching_methods` tinyint DEFAULT NULL COMMENT '教学方式',
  `first_degree` bit(1) DEFAULT b'0' COMMENT '是否第一学历',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_edu_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工教育经历表';

CREATE TABLE IF NOT EXISTS `hr_employee_work_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作经历编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `work_unit` varchar(128) DEFAULT NULL COMMENT '工作单位',
  `post_name` varchar(64) DEFAULT NULL COMMENT '职务',
  `start_time` datetime DEFAULT NULL COMMENT '工作开始日期',
  `end_time` datetime DEFAULT NULL COMMENT '工作结束日期',
  `reason` varchar(255) DEFAULT NULL COMMENT '离职原因',
  `witness_name` varchar(64) DEFAULT NULL COMMENT '证明人',
  `witness_phone` varchar(32) DEFAULT NULL COMMENT '证明人手机号',
  `remark` varchar(255) DEFAULT NULL COMMENT '工作备注',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_work_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工工作经历表';

CREATE TABLE IF NOT EXISTS `hr_employee_training_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '培训经历编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `course` varchar(128) DEFAULT NULL COMMENT '培训课程',
  `organization_name` varchar(128) DEFAULT NULL COMMENT '培训机构名称',
  `start_time` datetime DEFAULT NULL COMMENT '培训开始日期',
  `end_time` datetime DEFAULT NULL COMMENT '培训结束日期',
  `duration` varchar(32) DEFAULT NULL COMMENT '培训时长',
  `result` varchar(255) DEFAULT NULL COMMENT '培训成绩',
  `certificate_name` varchar(128) DEFAULT NULL COMMENT '培训证书名称',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_training_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工培训经历表';

CREATE TABLE IF NOT EXISTS `hr_employee_field_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置编号',
  `kind` tinyint NOT NULL COMMENT '配置种类：1档案字段 2新建员工字段',
  `entry_status` tinyint DEFAULT NULL COMMENT '入职状态（kind=2 时区分在职/待入职表单；kind=1 为 NULL）',
  `name` varchar(64) NOT NULL COMMENT '字段名称（与前端 HrmEmployeeVO 属性名一致）',
  `title` varchar(64) NOT NULL COMMENT '字段标题',
  `group_name` varchar(64) NOT NULL DEFAULT '' COMMENT '字段分组名称',
  `visible` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否显示',
  `editable` bit(1) DEFAULT b'1' COMMENT '是否允许员工编辑',
  `visible_locked` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否锁定显示',
  `editable_locked` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否锁定编辑',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_emp_field_config` (`kind`, `entry_status`, `name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工字段显示配置表';

CREATE TABLE IF NOT EXISTS `hr_employee_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `no` varchar(64) DEFAULT NULL COMMENT '合同编码',
  `type` tinyint DEFAULT NULL COMMENT '合同类型：1劳动合同 2保密协议 3竞业限制 4其他',
  `start_time` datetime DEFAULT NULL COMMENT '合同开始日期',
  `end_time` datetime DEFAULT NULL COMMENT '合同结束日期',
  `term` int DEFAULT NULL COMMENT '合同期限（月）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '合同状态：0未生效 1已生效 2已到期 3已解除',
  `sign_company` varchar(128) DEFAULT NULL COMMENT '签约公司',
  `sign_time` datetime DEFAULT NULL COMMENT '合同签订日期',
  `expire_remind` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否到期提醒',
  `file_urls` json DEFAULT NULL COMMENT '附件地址数组',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_contract_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工合同表';
