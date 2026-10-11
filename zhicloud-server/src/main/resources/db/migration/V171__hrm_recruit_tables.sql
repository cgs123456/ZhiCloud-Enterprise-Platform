-- V171: HRM 招聘域建表——职位类型/招聘职位/招聘渠道/候选人/面试/淘汰原因配置
-- ============================================================
-- 来源：zhicloud-ui-admin-vue3/src/api/hrm/recruit/{post/type,post,channel,candidate,interview,config} 契约逐字段生成。
-- 设计要点：
--   * hr_recruit_post_type：职位类型树（parent_id 自引用）
--   * hr_recruit_post：招聘职位（面试官/薪资面议(-1)/年龄不限(-1) 等特殊值由前端口径写入，服务层不解释）
--   * hr_recruit_channel：招聘渠道（system_flag 系统内置不可删，删除需 transfer_channel_id 承接）
--   * hr_recruit_candidate：候选人（简历附件 json 数组；面试信息冗余当前轮便于列表展示）
--   * hr_recruit_interview：面试记录（一候选人多轮；result 1未完成 2通过 3未通过 4取消）
--   * hr_recruit_config：招聘配置（淘汰原因字符串数组，按租户一条，kind 区分配置项）
-- 全部租户表（TenantBaseDO）；幂等：CREATE TABLE IF NOT EXISTS。
-- 与旧 /hr 招聘表（hr_job_posting/hr_resume/hr_interview）并存，旧表在旧页面退役后清理。
-- MySQL 8.4。

CREATE TABLE IF NOT EXISTS `hr_recruit_post_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '职位类型编号',
  `name` varchar(64) NOT NULL COMMENT '类型名称',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父类型编号（0=根）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0开启 1关闭',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_recruit_post_type_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 招聘职位类型表';

CREATE TABLE IF NOT EXISTS `hr_recruit_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招聘职位编号',
  `post_name` varchar(64) NOT NULL COMMENT '职位名称',
  `dept_id` bigint DEFAULT NULL COMMENT '用人部门编号',
  `job_nature` tinyint DEFAULT NULL COMMENT '工作性质：1全职 2实习 3兼职',
  `area_id` bigint DEFAULT NULL COMMENT '工作城市地区编号',
  `recruit_num` int DEFAULT NULL COMMENT '招聘人数',
  `reason` varchar(255) DEFAULT NULL COMMENT '招聘原因',
  `work_time` tinyint DEFAULT NULL COMMENT '工作经验要求',
  `education_require` tinyint DEFAULT NULL COMMENT '学历要求',
  `min_salary` decimal(12, 2) DEFAULT NULL COMMENT '最低薪资（-1 面议）',
  `max_salary` decimal(12, 2) DEFAULT NULL COMMENT '最高薪资（-1 面议）',
  `salary_unit` tinyint DEFAULT NULL COMMENT '薪资单位：1元/月 2元/年（-1 面议）',
  `min_age` int DEFAULT NULL COMMENT '最小年龄（-1 不限）',
  `max_age` int DEFAULT NULL COMMENT '最大年龄（-1 不限）',
  `latest_entry_time` datetime DEFAULT NULL COMMENT '最迟到岗时间',
  `owner_employee_id` bigint DEFAULT NULL COMMENT '招聘负责人员工编号',
  `interview_employee_ids` json DEFAULT NULL COMMENT '面试官员工编号数组',
  `description` text COMMENT '职位描述',
  `emergency_level` tinyint DEFAULT NULL COMMENT '紧急程度：1紧急 2一般',
  `post_type_id` bigint DEFAULT NULL COMMENT '职位类型编号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '职位状态：0停止招聘 1招聘中',
  `stop_reason` varchar(255) DEFAULT NULL COMMENT '停止原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_recruit_post_dept` (`dept_id`),
  KEY `idx_hr_recruit_post_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 招聘职位表';

CREATE TABLE IF NOT EXISTS `hr_recruit_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招聘渠道编号',
  `name` varchar(64) NOT NULL COMMENT '渠道名称',
  `system_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否系统内置（不可删）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0开启 1关闭',
  `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_recruit_channel_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 招聘渠道表';

CREATE TABLE IF NOT EXISTS `hr_recruit_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '候选人编号',
  `name` varchar(64) NOT NULL COMMENT '候选人姓名',
  `mobile` varchar(32) NOT NULL COMMENT '手机号码',
  `sex` tinyint DEFAULT NULL COMMENT '性别：1男 2女',
  `age` int DEFAULT NULL COMMENT '年龄',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `post_id` bigint DEFAULT NULL COMMENT '应聘职位编号',
  `dept_id` bigint DEFAULT NULL COMMENT '用人部门编号',
  `owner_employee_id` bigint DEFAULT NULL COMMENT '招聘负责人员工编号',
  `stage_number` int DEFAULT NULL COMMENT '面试轮次',
  `work_time` tinyint DEFAULT NULL COMMENT '工作年限',
  `education` tinyint DEFAULT NULL COMMENT '学历',
  `graduate_school` varchar(128) DEFAULT NULL COMMENT '毕业院校',
  `latest_work_place` varchar(128) DEFAULT NULL COMMENT '最近工作单位',
  `channel_id` bigint DEFAULT NULL COMMENT '招聘渠道编号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '候选人状态：1新候选人 2初选通过 3安排面试 4面试通过 5已发Offer 6待入职 7已淘汰 8已入职',
  `eliminate` varchar(255) DEFAULT NULL COMMENT '淘汰原因',
  `status_update_time` datetime DEFAULT NULL COMMENT '状态更新时间',
  `entry_time` datetime DEFAULT NULL COMMENT '入职时间',
  `resume_urls` json DEFAULT NULL COMMENT '简历附件地址数组',
  `interview_type` tinyint DEFAULT NULL COMMENT '当前面试方式',
  `interview_employee_id` bigint DEFAULT NULL COMMENT '主面试官员工编号',
  `other_interview_employee_ids` json DEFAULT NULL COMMENT '其他面试官员工编号数组',
  `interview_time` datetime DEFAULT NULL COMMENT '面试时间',
  `interview_address` varchar(255) DEFAULT NULL COMMENT '面试地址',
  `interview_result` tinyint DEFAULT NULL COMMENT '当前面试结果',
  `employee_id` bigint DEFAULT NULL COMMENT '转入的员工编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_recruit_candidate_post` (`post_id`),
  KEY `idx_hr_recruit_candidate_status` (`status`),
  KEY `idx_hr_recruit_candidate_mobile` (`mobile`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 招聘候选人表';

CREATE TABLE IF NOT EXISTS `hr_recruit_interview` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '面试编号',
  `candidate_id` bigint NOT NULL COMMENT '候选人编号',
  `type` tinyint DEFAULT NULL COMMENT '面试方式：1现场 2电话 3视频',
  `stage_number` int DEFAULT NULL COMMENT '面试轮次',
  `interview_employee_id` bigint DEFAULT NULL COMMENT '主面试官员工编号',
  `other_interview_employee_ids` json DEFAULT NULL COMMENT '其他面试官员工编号数组',
  `interview_time` datetime DEFAULT NULL COMMENT '面试时间',
  `address` varchar(255) DEFAULT NULL COMMENT '面试地址',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `result` tinyint NOT NULL DEFAULT 1 COMMENT '面试结果：1未完成 2通过 3未通过 4取消',
  `evaluate` varchar(512) DEFAULT NULL COMMENT '面试评价',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_recruit_interview_candidate` (`candidate_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 招聘面试表';

CREATE TABLE IF NOT EXISTS `hr_recruit_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招聘配置编号',
  `kind` tinyint NOT NULL COMMENT '配置种类：1淘汰原因',
  `content` json DEFAULT NULL COMMENT '配置内容（淘汰原因为字符串数组）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_recruit_config_kind` (`kind`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 招聘配置表';
