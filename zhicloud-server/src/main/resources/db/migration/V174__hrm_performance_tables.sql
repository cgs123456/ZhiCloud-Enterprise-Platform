-- V174: HRM 绩效域建表——考核模板/结果模板/绩效计划/员工绩效考核/考核阶段/考核指标
-- ============================================================
-- 来源：zhicloud-ui-admin-vue3/src/api/hrm/performance/{config/assessment-template,config/result-template,plan,assessment} 契约。
-- 设计要点（嵌套配置用 json 列整体存，与考勤组同法，控制表数量）：
--   * hr_performance_assessment_template：考核模板（dimensions 维度数组含 quotas 指标数组 → json）
--   * hr_performance_result_template：结果模板（levels 等级数组 → json）
--   * hr_performance_plan：绩效计划（scopes 考评范围、assessment_config 考核配置快照、result_config 结果配置快照、
--     review_stages/result_audit_stages/appeal_stages 阶段节点、target_confirmation_stage → json）
--   * hr_performance_assessment：员工绩效考核（一计划一员工一条；维度/指标评分为明细行）
--   * hr_performance_assessment_dimension：考核维度（明细）
--   * hr_performance_assessment_quota：考核指标（明细，含自评/他评/最终分）
--   * hr_performance_assessment_stage：考核阶段（员工维度的阶段流转，含处理人/状态/得分）
-- 全部租户表（TenantBaseDO）；幂等：CREATE TABLE IF NOT EXISTS。
-- 与旧 /hr 绩效表（hr_performance）并存，旧表待旧页面退役后清理。
-- MySQL 8.4。

CREATE TABLE IF NOT EXISTS `hr_performance_assessment_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核模板编号',
  `name` varchar(64) NOT NULL COMMENT '模板名称',
  `illustrate` varchar(512) DEFAULT NULL COMMENT '模板说明',
  `score_calculation` tinyint NOT NULL DEFAULT 1 COMMENT '计分方式',
  `upper_limit_type` tinyint NOT NULL DEFAULT 1 COMMENT '分数上限类型',
  `upper_limit_score` decimal(8, 2) DEFAULT NULL COMMENT '分数上限',
  `dimensions` json DEFAULT NULL COMMENT '考核维度数组（含 quotas 指标数组）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 绩效考核模板表';

CREATE TABLE IF NOT EXISTS `hr_performance_result_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结果模板编号',
  `name` varchar(64) NOT NULL COMMENT '结果模板名称',
  `levels` json DEFAULT NULL COMMENT '结果等级数组 [{name,minScore,maxScore,coefficient}]',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0开启 1关闭',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 绩效结果模板表';

CREATE TABLE IF NOT EXISTS `hr_performance_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绩效计划编号',
  `name` varchar(128) NOT NULL COMMENT '计划名称',
  `cycle_type` tinyint DEFAULT NULL COMMENT '考核周期类型：1月度 2季度 3上半年 4下半年 5全年 6其他',
  `cycle` varchar(64) DEFAULT NULL COMMENT '考核周期',
  `quarter` tinyint DEFAULT NULL COMMENT '季度',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `description` varchar(512) DEFAULT NULL COMMENT '考核说明',
  `scopes` json DEFAULT NULL COMMENT '考评范围列表',
  `assessment_template_id` bigint DEFAULT NULL COMMENT '考核模板编号',
  `assessment_config` json DEFAULT NULL COMMENT '考核配置快照',
  `result_template_id` bigint DEFAULT NULL COMMENT '结果模板编号',
  `result_config` json DEFAULT NULL COMMENT '结果配置快照',
  `quota_setting_type` tinyint DEFAULT NULL COMMENT '指标制定方式',
  `target_confirmation` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否开启目标确认',
  `target_confirmation_stage` json DEFAULT NULL COMMENT '目标确认节点',
  `review_stages` json DEFAULT NULL COMMENT '评分阶段列表',
  `result_audit` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否开启结果审核',
  `result_audit_stages` json DEFAULT NULL COMMENT '结果审核节点列表',
  `result_confirmation` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否开启结果确认',
  `appeal_stages` json DEFAULT NULL COMMENT '申诉确认节点列表',
  `appeal_timeout_days` int DEFAULT NULL COMMENT '申诉超期天数',
  `appeal_timeout_action` tinyint DEFAULT NULL COMMENT '申诉超期处理动作',
  `sync_to_salary` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否同步薪资',
  `paid_for_month` varchar(16) DEFAULT NULL COMMENT '计薪月份',
  `stage_type` tinyint DEFAULT NULL COMMENT '当前阶段',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '计划状态：1草稿 2未开始 3进行中 4已归档 5已终止',
  `terminate_time` datetime DEFAULT NULL COMMENT '终止时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_perf_plan_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 绩效计划表';

CREATE TABLE IF NOT EXISTS `hr_performance_assessment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '员工绩效考核编号',
  `plan_id` bigint NOT NULL COMMENT '绩效计划编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '考核状态',
  `process_status` tinyint DEFAULT NULL COMMENT '流程状态',
  `stage_type` tinyint DEFAULT NULL COMMENT '当前阶段',
  `stage_sort` int DEFAULT NULL COMMENT '当前阶段排序',
  `score` decimal(8, 2) DEFAULT NULL COMMENT '绩效得分',
  `result_level` varchar(32) DEFAULT NULL COMMENT '结果等级',
  `coefficient` decimal(6, 2) DEFAULT NULL COMMENT '绩效系数',
  `target_confirmation_employee_id` bigint DEFAULT NULL COMMENT '目标确认人员工编号',
  `target_confirmation_result` tinyint DEFAULT NULL COMMENT '目标确认结果',
  `target_confirmation_comment` varchar(512) DEFAULT NULL COMMENT '目标确认意见',
  `target_confirmation_time` datetime DEFAULT NULL COMMENT '目标确认时间',
  `self_comment` varchar(512) DEFAULT NULL COMMENT '自评说明',
  `reviewer_comment` varchar(512) DEFAULT NULL COMMENT '评分说明',
  `result_comment` varchar(512) DEFAULT NULL COMMENT '结果说明',
  `appeal_status` tinyint NOT NULL DEFAULT 0 COMMENT '申诉状态：0无 1待处理 2已通过 3已驳回 4已取消',
  `appeal_reason` varchar(512) DEFAULT NULL COMMENT '申诉原因',
  `archive_time` datetime DEFAULT NULL COMMENT '归档时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_perf_assess_plan` (`plan_id`),
  KEY `idx_hr_perf_assess_employee` (`employee_id`),
  KEY `idx_hr_perf_assess_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 员工绩效考核表';

CREATE TABLE IF NOT EXISTS `hr_performance_assessment_dimension` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核维度编号',
  `assessment_id` bigint NOT NULL COMMENT '员工绩效考核编号',
  `name` varchar(64) DEFAULT NULL COMMENT '维度名称',
  `quota_type` tinyint DEFAULT NULL COMMENT '指标类型',
  `weight` decimal(6, 2) DEFAULT NULL COMMENT '维度权重',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `allow_edit` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否允许员工编辑指标',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_perf_dim_assessment` (`assessment_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 绩效考核维度表';

CREATE TABLE IF NOT EXISTS `hr_performance_assessment_quota` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核指标编号',
  `assessment_id` bigint NOT NULL COMMENT '员工绩效考核编号',
  `dimension_id` bigint NOT NULL COMMENT '考核维度编号',
  `allow_edit` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否允许员工编辑指标',
  `preset` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否预设指标',
  `dimension_name` varchar(64) DEFAULT NULL COMMENT '维度名称（冗余）',
  `name` varchar(128) DEFAULT NULL COMMENT '指标名称',
  `description` varchar(512) DEFAULT NULL COMMENT '指标说明',
  `standard` varchar(512) DEFAULT NULL COMMENT '评分标准',
  `dimension_weight` decimal(6, 2) DEFAULT NULL COMMENT '维度权重',
  `weight` decimal(6, 2) DEFAULT NULL COMMENT '指标权重',
  `score_type` tinyint DEFAULT NULL COMMENT '评分类型',
  `target_value` varchar(255) DEFAULT NULL COMMENT '目标值',
  `actual_value` varchar(255) DEFAULT NULL COMMENT '实际值',
  `self_score` decimal(8, 2) DEFAULT NULL COMMENT '自评分',
  `reviewer_score` decimal(8, 2) DEFAULT NULL COMMENT '他评分',
  `final_score` decimal(8, 2) DEFAULT NULL COMMENT '最终分',
  `comment` varchar(512) DEFAULT NULL COMMENT '备注',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_perf_quota_assessment` (`assessment_id`),
  KEY `idx_hr_perf_quota_dimension` (`dimension_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 绩效考核指标表';

CREATE TABLE IF NOT EXISTS `hr_performance_assessment_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核阶段编号',
  `assessment_id` bigint NOT NULL COMMENT '员工绩效考核编号',
  `type` tinyint NOT NULL COMMENT '业务阶段类型',
  `handler_employee_id` bigint DEFAULT NULL COMMENT '处理员工编号',
  `name` varchar(64) DEFAULT NULL COMMENT '阶段名称',
  `rater_type` tinyint DEFAULT NULL COMMENT '评分人类型',
  `weight` decimal(6, 2) DEFAULT NULL COMMENT '阶段权重',
  `scoring_type` tinyint DEFAULT NULL COMMENT '评分方式',
  `visible_content` tinyint DEFAULT NULL COMMENT '可见内容',
  `required_setting` bit(1) NOT NULL DEFAULT b'0' COMMENT '评语是否必填',
  `reject_authority` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否允许驳回',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '阶段状态',
  `score` decimal(8, 2) DEFAULT NULL COMMENT '阶段得分',
  `result_level` varchar(32) DEFAULT NULL COMMENT '阶段结果等级',
  `comment` varchar(512) DEFAULT NULL COMMENT '评分说明',
  `reject_reason` varchar(512) DEFAULT NULL COMMENT '驳回原因',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_perf_stage_assessment` (`assessment_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 绩效考核阶段表';

CREATE TABLE IF NOT EXISTS `hr_performance_process_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程记录编号',
  `biz_type` tinyint NOT NULL COMMENT '业务类型：1绩效计划 2员工考核',
  `biz_id` bigint NOT NULL COMMENT '业务编号',
  `title` varchar(128) DEFAULT NULL COMMENT '记录标题',
  `content` varchar(512) DEFAULT NULL COMMENT '记录内容',
  `source` varchar(16) DEFAULT NULL COMMENT '记录来源：ACTION/BUSINESS/STAGE',
  `status` tinyint DEFAULT NULL COMMENT '流程状态',
  `operator_employee_id` bigint DEFAULT NULL COMMENT '操作人员工编号',
  `operate_time` datetime DEFAULT NULL COMMENT '操作时间',
  `file_urls` json DEFAULT NULL COMMENT '附件地址列表',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_perf_record_biz` (`biz_type`, `biz_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 绩效流程记录表';
