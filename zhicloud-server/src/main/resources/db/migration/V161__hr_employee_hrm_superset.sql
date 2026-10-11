-- V161: HR/HRM 融合第一步（纯加性）——hr_employee 增补 HRM 员工档案超集列 + 组织树合一到 system_dept
-- ============================================================
-- 设计（台账 §12.184）：本刀 **只加列 + 回填 + 组织树种子**，不改名、不删列、不动旧列数据——
--   * 旧列（emp_no/gender/phone/id_card/birth_date/hire_date/leave_date/status/employment_type）
--     原样保留：demo 工具链（schema-snapshot 黄金测试、生成器 fail-closed、invariants 断言）
--     零冲击；老 /hr 接口在其退役前继续可读。
--   * HRM 规范字段用**新列**（含双状态轴 entry_status/employee_status/type 与重定值域的 sex），
--     由回填保证新旧列语义一致；同域镜像字段（jobNumber/mobile/idNumber）不加列——
--     Java DO 直接以 @TableField 映射 emp_no/phone/id_card，单一真相、零镜像、零漂移。
--   * 旧列的清理退役（DROP/RENAME + demo 工具链快照重导）= 后继迁移，在 /hrm 员工链验证通、
--     旧页面/接口删除后执行；本迁移不构成破坏性中间态。
--   * 组织树合一本轮即完成：hr_department 存活行按**原 id** 迁入 system_dept（HRM dept 页本就调
--     @/api/system/dept；id 同值故 hr_employee.dept_id / hr_position.dept_id 无需重指）。
--     hr_department 表与数据保留为存档（仅 Java 栈退役），避免同时动两套断言面。
-- MySQL 8.4。回填 UPDATE 为确定值可重放；种子 WHERE NOT EXISTS。

-- 一、增补 HRM 超集列（值域见 V163 字典；与前端 api/hrm/employee 契约逐一对应）
ALTER TABLE `hr_employee`
  ADD COLUMN `sex` tinyint DEFAULT NULL COMMENT '性别：1男 2女（对齐 system_user_sex；由 gender 重映射）' AFTER `name`,
  ADD COLUMN `birthday` datetime DEFAULT NULL COMMENT '出生时间（由 birth_date 升级）',
  ADD COLUMN `entry_status` tinyint NOT NULL DEFAULT 1 COMMENT '入职状态：1在职 2待入职 3待离职 4离职（老 status 映射）',
  ADD COLUMN `employee_status` tinyint NOT NULL DEFAULT 1 COMMENT '员工状态：1正式 2试用 3实习 4兼职 5劳务 6顾问 7返聘 8外包（老 employment_type 映射）',
  ADD COLUMN `type` tinyint NOT NULL DEFAULT 1 COMMENT '聘用形式：1正式 2非正式（老 employment_type 折叠）',
  ADD COLUMN `entry_time` datetime DEFAULT NULL COMMENT '入职时间（由 hire_date 升级）',
  ADD COLUMN `leave_time` datetime DEFAULT NULL COMMENT '离职时间（由 leave_date 升级）',
  ADD COLUMN `user_id` bigint DEFAULT NULL COMMENT '绑定后台用户编号（system_users.id）',
  ADD COLUMN `leader_employee_id` bigint DEFAULT NULL COMMENT '直属上级员工编号（自引用 hr_employee.id）',
  ADD COLUMN `country` varchar(32) DEFAULT NULL COMMENT '国家或地区',
  ADD COLUMN `nation` varchar(16) DEFAULT NULL COMMENT '民族',
  ADD COLUMN `id_type` tinyint DEFAULT NULL COMMENT '证件类型：1身份证 2港澳通行证 3台湾通行证 4护照 5其他',
  ADD COLUMN `native_place` varchar(64) DEFAULT NULL COMMENT '籍贯',
  ADD COLUMN `address` varchar(128) DEFAULT NULL COMMENT '户籍地址',
  ADD COLUMN `highest_education` tinyint DEFAULT NULL COMMENT '最高学历：1小学..12其他（见 hrm_employee_education 字典）',
  ADD COLUMN `probation` int DEFAULT NULL COMMENT '试用期（月）',
  ADD COLUMN `regular_time` datetime DEFAULT NULL COMMENT '转正时间',
  ADD COLUMN `post_name` varchar(64) DEFAULT NULL COMMENT '职位名称',
  ADD COLUMN `post_level` varchar(64) DEFAULT NULL COMMENT '岗位职级',
  ADD COLUMN `work_city` varchar(64) DEFAULT NULL COMMENT '工作城市',
  ADD COLUMN `work_address` varchar(128) DEFAULT NULL COMMENT '工作地点',
  ADD COLUMN `work_detail_address` varchar(255) DEFAULT NULL COMMENT '工作详细地址',
  ADD COLUMN `channel_id` bigint DEFAULT NULL COMMENT '招聘渠道编号（hrm 招聘域稍后接线）',
  ADD COLUMN `candidate_id` bigint DEFAULT NULL COMMENT '招聘候选人编号（建档来源）',
  ADD COLUMN `company_age_start_time` datetime DEFAULT NULL COMMENT '司龄开始时间',
  ADD COLUMN `salary_card_number` varchar(64) DEFAULT NULL COMMENT '工资卡号',
  ADD COLUMN `salary_card_area_id` bigint DEFAULT NULL COMMENT '开户地区编号',
  ADD COLUMN `salary_card_area_name` varchar(64) DEFAULT NULL COMMENT '开户地区名称',
  ADD COLUMN `salary_card_bank_name` varchar(128) DEFAULT NULL COMMENT '银行名称',
  ADD COLUMN `salary_card_bank_branch_name` varchar(128) DEFAULT NULL COMMENT '开户支行名称',
  ADD COLUMN `social_security_number` varchar(64) DEFAULT NULL COMMENT '个人社保账号',
  ADD COLUMN `accumulation_fund_number` varchar(64) DEFAULT NULL COMMENT '个人公积金账号';

-- HRM 契约：工号仅对「在职」员工必填（待入职可先建档后补工号）。前端 EmployeeForm 条件校验同口径。
-- emp_no 放宽为可空（约束加宽，不损既有数据；uk 对 NULL 不去重，MySQL 语义）。
ALTER TABLE `hr_employee` MODIFY COLUMN `emp_no` varchar(64) NULL DEFAULT NULL COMMENT '工号（HRM jobNumber；在职必填由服务层校验）';

-- 二、回填：旧列 -> 新列（确定值，幂等）
-- sex：10男->1、20女->2（1/2 原样）
UPDATE `hr_employee` SET `sex` = CASE `gender` WHEN 10 THEN 1 WHEN 20 THEN 2 ELSE `gender` END WHERE `sex` IS NULL;
-- 日期列升级 datetime（date 00:00 时刻，epoch 毫秒序列化口径）
UPDATE `hr_employee` SET `birthday` = CAST(`birth_date` AS DATETIME) WHERE `birthday` IS NULL AND `birth_date` IS NOT NULL;
UPDATE `hr_employee` SET `entry_time` = CAST(`hire_date` AS DATETIME) WHERE `entry_time` IS NULL AND `hire_date` IS NOT NULL;
UPDATE `hr_employee` SET `leave_time` = CAST(`leave_date` AS DATETIME) WHERE `leave_time` IS NULL AND `leave_date` IS NOT NULL;
-- 双状态轴：entry_status（老 status 10在职->1、20离职->4、30停薪->1：HRM 无停薪态，归并在职；实测 0 行）
UPDATE `hr_employee` SET `entry_status` = CASE `status` WHEN 10 THEN 1 WHEN 20 THEN 4 WHEN 30 THEN 1 ELSE 1 END;
-- employee_status（老 employment_type 10全职->1、20兼职->4、30实习->3、40外包->8）
UPDATE `hr_employee` SET `employee_status` = CASE `employment_type` WHEN 10 THEN 1 WHEN 20 THEN 4 WHEN 30 THEN 3 WHEN 40 THEN 8 ELSE 1 END;
-- type（10全职->1 正式，其余->2 非正式）
UPDATE `hr_employee` SET `type` = CASE WHEN `employment_type` = 10 THEN 1 ELSE 2 END;
-- 在职且正式员工视为已转正（转正时间=入职时间）
UPDATE `hr_employee` SET `regular_time` = `entry_time` WHERE `entry_status` = 1 AND `type` = 1 AND `regular_time` IS NULL;
-- 离职行补离职时间
UPDATE `hr_employee` SET `leave_time` = `update_time` WHERE `entry_status` = 4 AND `leave_time` IS NULL;

-- 三、新列查询索引（job_number 唯一沿用 emp_no 既有唯一索引 uk_emp_no——DO 直接映射该列，无新列）
ALTER TABLE `hr_employee` ADD INDEX `idx_hr_employee_leader` (`leader_employee_id`);
ALTER TABLE `hr_employee` ADD INDEX `idx_hr_employee_user` (`user_id`);
ALTER TABLE `hr_employee` ADD INDEX `idx_hr_employee_entry_status` (`entry_status`);

-- 四、组织树合一：hr_department 存活行按原 id 迁入 system_dept
-- system_dept 无 code 列，code 不搬；status：hr 10启用 -> 平台 0开启；creator 保留（纳入既有 demo 清理口径）。
INSERT INTO `system_dept` (`id`, `name`, `parent_id`, `sort`, `leader_user_id`, `phone`, `email`, `status`,
                           `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT d.`id`, d.`name`, d.`parent_id`, d.`sort`, NULL, NULL, NULL, 0,
       d.`creator`, NOW(), d.`updater`, NOW(), b'0', d.`tenant_id`
  FROM `hr_department` d
 WHERE d.`deleted` = b'0'
   AND NOT EXISTS (SELECT 1 FROM `system_dept` s WHERE s.`id` = d.`id`);
