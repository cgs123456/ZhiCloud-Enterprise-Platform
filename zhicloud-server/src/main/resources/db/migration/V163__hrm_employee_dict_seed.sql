-- V163: HR/HRM 融合第一步——员工域字典种子 + 档案填写通知模板 + 字段显示配置默认行
-- ============================================================
-- 说明：HRM 员工列表/表单的下拉走数据字典（getIntDictOptions），库里无 hrm_employee_% 字典即
--       「可见但选项为空」（§12.183 同型缺口），本迁移补齐。取值逐一对齐前端
--       views/hrm/utils/constants.ts（HrmEmployeeEntryStatus/HrmEmployeeStatus/HrmEmployeeType/
--       HrmEmployeeEducation），与后端 V161 新列值域一致。
--       sex 复用平台既有 system_user_sex（1男 2女），不新建。
--       通知模板 send-profile-fill-message 依赖（system 通知服务按 code 查模板，缺行即失败）。
--       字段配置表（hr_employee_field_config）预置默认全字段可见行——前端 EmployeeForm 的
--       createFieldVisibleMap 语义：配置返回**空数组**时字段全部隐藏（`visibleFields.has(name)` 为假），
--       故必须种默认行，新建员工表单才可用。
-- 幂等：system_dict_type 按 (type)、system_dict_data 按 (dict_type,value)、system_notify_template 按 (code)、
--       hr_employee_field_config 按 (kind,entry_status,name) 预检（uk 已建，INSERT IGNORE 等价）。
-- id 号段：字典 type=1062010+/data=1062011+（避开 V159 的 1062001-1062012）；通知模板 id=3011。

-- 一、字典类型
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT t.* FROM (
  SELECT 1062021 AS `id`, 'HRM 员工入职状态' AS `name`, 'hrm_employee_entry_status' AS `type`, 0 AS `status`, 'HRM 员工入职状态：1在职 2待入职 3待离职 4离职' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, '1970-01-01 00:00:00' AS `deleted_time`
  UNION ALL SELECT 1062022, 'HRM 员工状态', 'hrm_employee_status', 0, 'HRM 员工状态：1正式 2试用 3实习 4兼职 5劳务 6顾问 7返聘 8外包', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1062023, 'HRM 聘用形式', 'hrm_employee_type', 0, 'HRM 聘用形式：1正式 2非正式', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1062024, 'HRM 员工学历', 'hrm_employee_education', 0, 'HRM 员工最高学历 1-12', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
) t
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` d WHERE d.`deleted` = b'0' AND d.`type` = t.`type`);

-- 二、字典数据
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  SELECT 1062031 AS `id`, 1 AS `sort`, '在职' AS `label`, '1' AS `value`, 'hrm_employee_entry_status' AS `dict_type`, 0 AS `status`, 'success' AS `color_type`, '' AS `css_class`, '' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 1062032, 2, '待入职', '2', 'hrm_employee_entry_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062033, 3, '待离职', '3', 'hrm_employee_entry_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062034, 4, '离职', '4', 'hrm_employee_entry_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062041, 1, '正式', '1', 'hrm_employee_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062042, 2, '试用', '2', 'hrm_employee_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062043, 3, '实习', '3', 'hrm_employee_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062044, 4, '兼职', '4', 'hrm_employee_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062045, 5, '劳务', '5', 'hrm_employee_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062046, 6, '顾问', '6', 'hrm_employee_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062047, 7, '返聘', '7', 'hrm_employee_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062048, 8, '外包', '8', 'hrm_employee_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062051, 1, '正式', '1', 'hrm_employee_type', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062052, 2, '非正式', '2', 'hrm_employee_type', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062061, 1, '小学', '1', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062062, 2, '初中', '2', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062063, 3, '中专', '3', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062064, 4, '中职', '4', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062065, 5, '技校', '5', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062066, 6, '高中', '6', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062067, 7, '大专', '7', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062068, 8, '本科', '8', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062069, 9, '硕士', '9', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062070, 10, '博士', '10', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062071, 11, '博士后', '11', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062072, 12, '其他', '12', 'hrm_employee_education', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` d WHERE d.`deleted` = b'0' AND d.`dict_type` = t.`dict_type` AND d.`value` = t.`value`);

-- 三、档案填写通知模板（type=1 通知消息，对齐 system 既有取值）
INSERT INTO `system_notify_template` (`id`, `name`, `code`, `nickname`, `content`, `type`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  SELECT 3011 AS `id`, '员工档案填写提醒' AS `name`, 'hrm_employee_profile_fill' AS `code`, '人事系统' AS `nickname`,
         '您好 {name}，请及时登录人事系统完善您的员工档案。' AS `content`,
         1 AS `type`, '["name"]' AS `params`, 0 AS `status`, 'HRM 发送填写档案通知使用' AS `remark`,
         'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
) t
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template` m WHERE m.`deleted` = b'0' AND m.`code` = t.`code`);

-- 四、员工字段显示配置默认行（档案 kind=1 entry_status=NULL 一行一条；新建 kind=2 按在职/待入职两套）
-- 名单取自前端 isFieldVisible(...) 的全部字段名；默认全部 visible=1、archive editable=1、锁定位 0。
INSERT INTO `hr_employee_field_config` (`kind`, `entry_status`, `name`, `title`, `group_name`, `visible`, `editable`, `visible_locked`, `editable_locked`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.`kind`, t.`entry_status`, t.`name`, t.`title`, t.`group_name`, b'1', b'1', b'0', b'0', t.`sort`, 'system', NOW(), 'system', NOW(), b'0', 1
FROM (
  -- kind=1 档案字段（分组按详情页基本信息/岗位信息/离职信息三块）
  SELECT 1 AS `kind`, NULL AS `entry_status`, 'name' AS `name`, '员工姓名' AS `title`, '基本信息' AS `group_name`, 1 AS `sort`
  UNION ALL SELECT 1, NULL, 'jobNumber', '工号', '基本信息', 2
  UNION ALL SELECT 1, NULL, 'userId', '后台用户', '基本信息', 3
  UNION ALL SELECT 1, NULL, 'mobile', '手机号', '基本信息', 4
  UNION ALL SELECT 1, NULL, 'email', '邮箱', '基本信息', 5
  UNION ALL SELECT 1, NULL, 'country', '国家或地区', '基本信息', 6
  UNION ALL SELECT 1, NULL, 'nation', '民族', '基本信息', 7
  UNION ALL SELECT 1, NULL, 'idType', '证件类型', '基本信息', 8
  UNION ALL SELECT 1, NULL, 'idNumber', '证件号码', '基本信息', 9
  UNION ALL SELECT 1, NULL, 'sex', '性别', '基本信息', 10
  UNION ALL SELECT 1, NULL, 'nativePlace', '籍贯', '基本信息', 11
  UNION ALL SELECT 1, NULL, 'birthday', '出生时间', '基本信息', 12
  UNION ALL SELECT 1, NULL, 'age', '年龄', '基本信息', 13
  UNION ALL SELECT 1, NULL, 'address', '户籍地址', '基本信息', 14
  UNION ALL SELECT 1, NULL, 'highestEducation', '最高学历', '基本信息', 15
  UNION ALL SELECT 1, NULL, 'deptId', '部门', '岗位信息', 21
  UNION ALL SELECT 1, NULL, 'leaderEmployeeId', '直属上级', '岗位信息', 22
  UNION ALL SELECT 1, NULL, 'entryStatus', '入职状态', '岗位信息', 23
  UNION ALL SELECT 1, NULL, 'status', '员工状态', '岗位信息', 24
  UNION ALL SELECT 1, NULL, 'type', '聘用形式', '岗位信息', 25
  UNION ALL SELECT 1, NULL, 'entryTime', '入职时间', '岗位信息', 26
  UNION ALL SELECT 1, NULL, 'probation', '试用期', '岗位信息', 27
  UNION ALL SELECT 1, NULL, 'regularTime', '转正时间', '岗位信息', 28
  UNION ALL SELECT 1, NULL, 'leaveTime', '离职时间', '岗位信息', 29
  UNION ALL SELECT 1, NULL, 'postName', '职位名称', '岗位信息', 30
  UNION ALL SELECT 1, NULL, 'postLevel', '岗位职级', '岗位信息', 31
  UNION ALL SELECT 1, NULL, 'workCity', '工作城市', '岗位信息', 32
  UNION ALL SELECT 1, NULL, 'workAddress', '工作地点', '岗位信息', 33
  UNION ALL SELECT 1, NULL, 'workDetailAddress', '工作详细地址', '岗位信息', 34
  UNION ALL SELECT 1, NULL, 'channelId', '招聘渠道', '岗位信息', 35
  UNION ALL SELECT 1, NULL, 'companyAge', '司龄', '岗位信息', 36
  UNION ALL SELECT 1, NULL, 'companyAge', '司龄', '岗位信息', 36
  UNION ALL SELECT 1, NULL, 'companyAgeStartTime', '司龄开始时间', '岗位信息', 37
  UNION ALL SELECT 1, NULL, 'candidateId', '候选人编号', '岗位信息', 38
  UNION ALL SELECT 1, NULL, 'remark', '备注', '离职信息', 41
  -- kind=2 新建员工字段：在职(1)与待入职(2)两套，名单一致
  UNION ALL SELECT 2, 1, 'name', '员工姓名', '基本信息', 1
  UNION ALL SELECT 2, 1, 'jobNumber', '工号', '基本信息', 2
  UNION ALL SELECT 2, 1, 'userId', '后台用户', '基本信息', 3
  UNION ALL SELECT 2, 1, 'mobile', '手机号', '基本信息', 4
  UNION ALL SELECT 2, 1, 'email', '邮箱', '基本信息', 5
  UNION ALL SELECT 2, 1, 'country', '国家或地区', '基本信息', 6
  UNION ALL SELECT 2, 1, 'nation', '民族', '基本信息', 7
  UNION ALL SELECT 2, 1, 'idType', '证件类型', '基本信息', 8
  UNION ALL SELECT 2, 1, 'idNumber', '证件号码', '基本信息', 9
  UNION ALL SELECT 2, 1, 'sex', '性别', '基本信息', 10
  UNION ALL SELECT 2, 1, 'nativePlace', '籍贯', '基本信息', 11
  UNION ALL SELECT 2, 1, 'birthday', '出生时间', '基本信息', 12
  UNION ALL SELECT 2, 1, 'age', '年龄', '基本信息', 13
  UNION ALL SELECT 2, 1, 'highestEducation', '最高学历', '基本信息', 14
  UNION ALL SELECT 2, 1, 'address', '户籍地址', '基本信息', 15
  UNION ALL SELECT 2, 1, 'deptId', '部门', '岗位信息', 21
  UNION ALL SELECT 2, 1, 'leaderEmployeeId', '直属上级', '岗位信息', 22
  UNION ALL SELECT 2, 1, 'entryStatus', '入职状态', '岗位信息', 23
  UNION ALL SELECT 2, 1, 'status', '员工状态', '岗位信息', 24
  UNION ALL SELECT 2, 1, 'type', '聘用形式', '岗位信息', 25
  UNION ALL SELECT 2, 1, 'entryTime', '入职时间', '岗位信息', 26
  UNION ALL SELECT 2, 1, 'probation', '试用期', '岗位信息', 27
  UNION ALL SELECT 2, 1, 'regularTime', '转正时间', '岗位信息', 28
  UNION ALL SELECT 2, 1, 'leaveTime', '离职时间', '岗位信息', 29
  UNION ALL SELECT 2, 1, 'postName', '职位名称', '岗位信息', 30
  UNION ALL SELECT 2, 1, 'postLevel', '岗位职级', '岗位信息', 31
  UNION ALL SELECT 2, 1, 'workCity', '工作城市', '岗位信息', 32
  UNION ALL SELECT 2, 1, 'workAddress', '工作地点', '岗位信息', 33
  UNION ALL SELECT 2, 1, 'workDetailAddress', '工作详细地址', '岗位信息', 34
  UNION ALL SELECT 2, 1, 'channelId', '招聘渠道', '岗位信息', 35
  UNION ALL SELECT 2, 1, 'companyAge', '司龄', '岗位信息', 36
  UNION ALL SELECT 2, 1, 'companyAgeStartTime', '司龄开始时间', '岗位信息', 37
  UNION ALL SELECT 2, 1, 'candidateId', '候选人编号', '岗位信息', 38
  UNION ALL SELECT 2, 1, 'remark', '备注', '岗位信息', 41
  UNION ALL SELECT 2, 2, 'name', '员工姓名', '基本信息', 1
  UNION ALL SELECT 2, 2, 'jobNumber', '工号', '基本信息', 2
  UNION ALL SELECT 2, 2, 'userId', '后台用户', '基本信息', 3
  UNION ALL SELECT 2, 2, 'mobile', '手机号', '基本信息', 4
  UNION ALL SELECT 2, 2, 'email', '邮箱', '基本信息', 5
  UNION ALL SELECT 2, 2, 'country', '国家或地区', '基本信息', 6
  UNION ALL SELECT 2, 2, 'nation', '民族', '基本信息', 7
  UNION ALL SELECT 2, 2, 'idType', '证件类型', '基本信息', 8
  UNION ALL SELECT 2, 2, 'idNumber', '证件号码', '基本信息', 9
  UNION ALL SELECT 2, 2, 'sex', '性别', '基本信息', 10
  UNION ALL SELECT 2, 2, 'nativePlace', '籍贯', '基本信息', 11
  UNION ALL SELECT 2, 2, 'birthday', '出生时间', '基本信息', 12
  UNION ALL SELECT 2, 2, 'age', '年龄', '基本信息', 13
  UNION ALL SELECT 2, 2, 'highestEducation', '最高学历', '基本信息', 14
  UNION ALL SELECT 2, 2, 'address', '户籍地址', '基本信息', 15
  UNION ALL SELECT 2, 2, 'deptId', '部门', '岗位信息', 21
  UNION ALL SELECT 2, 2, 'leaderEmployeeId', '直属上级', '岗位信息', 22
  UNION ALL SELECT 2, 2, 'entryStatus', '入职状态', '岗位信息', 23
  UNION ALL SELECT 2, 2, 'status', '员工状态', '岗位信息', 24
  UNION ALL SELECT 2, 2, 'type', '聘用形式', '岗位信息', 25
  UNION ALL SELECT 2, 2, 'entryTime', '入职时间', '岗位信息', 26
  UNION ALL SELECT 2, 2, 'probation', '试用期', '岗位信息', 27
  UNION ALL SELECT 2, 2, 'regularTime', '转正时间', '岗位信息', 28
  UNION ALL SELECT 2, 2, 'leaveTime', '离职时间', '岗位信息', 29
  UNION ALL SELECT 2, 2, 'postName', '职位名称', '岗位信息', 30
  UNION ALL SELECT 2, 2, 'postLevel', '岗位职级', '岗位信息', 31
  UNION ALL SELECT 2, 2, 'workCity', '工作城市', '岗位信息', 32
  UNION ALL SELECT 2, 2, 'workAddress', '工作地点', '岗位信息', 33
  UNION ALL SELECT 2, 2, 'workDetailAddress', '工作详细地址', '岗位信息', 34
  UNION ALL SELECT 2, 2, 'channelId', '招聘渠道', '岗位信息', 35
  UNION ALL SELECT 2, 2, 'companyAge', '司龄', '岗位信息', 36
  UNION ALL SELECT 2, 2, 'companyAgeStartTime', '司龄开始时间', '岗位信息', 37
  UNION ALL SELECT 2, 2, 'candidateId', '候选人编号', '岗位信息', 38
  UNION ALL SELECT 2, 2, 'remark', '备注', '岗位信息', 41
) t
WHERE NOT EXISTS (SELECT 1 FROM `hr_employee_field_config` c WHERE c.`deleted` = b'0' AND c.`kind` = t.`kind`
                    AND (c.`entry_status` = t.`entry_status` OR (c.`entry_status` IS NULL AND t.`entry_status` IS NULL))
                    AND c.`name` = t.`name`);
