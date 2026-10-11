-- V172: HRM 招聘域菜单与按钮权限 + 招聘字典种子
-- ============================================================
-- 一、菜单（挂 9100 /hrm 目录下，id 9190-9199 段；避开 9100-9186）
--     来源：views/hrm/recruit 页面 v-hasPermi/checkPermi 与后端 @PreAuthorize 双侧交集。
--     权限口径：post/{query,create,update}、channel/{query,create,update,delete}、
--               candidate/{query,create,update,delete}、interview/{create,update}、
--               config/{query,update}（淘汰原因配置）。
--     候选人「转入员工」复用 hrm:employee:update（员工域菜单已种）。
-- 二、字典 10 个（前端 DICT_TYPE 常量逐项对齐）。
-- 写法沿用 V158（NOT EXISTS 预检、只认 deleted=b'0'）；menu id 9190-9199 段。
-- 幂等：菜单按 (permission,type,parent_id) 预检；字典按 type 预检；字典数据按 (dict_type,value) 预检。
-- MySQL 8.4。

-- ========== 一、菜单 ==========

INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9190 AS `id`, '招聘职位', 'hrm:recruit:post:query' AS `permission`, 2 AS `type`, 9 AS `sort`, 9100 AS `parent_id`, 'recruit/post' AS `path`, 'ep:briefcase' AS `icon`, 'hrm/recruit/post/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9191, '职位新增', 'hrm:recruit:post:create', 3, 1, 9190, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9192, '职位修改', 'hrm:recruit:post:update', 3, 2, 9190, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9193, '候选人', 'hrm:recruit:candidate:query', 2, 10, 9100, 'recruit/candidate', 'ep:user-filled', 'hrm/recruit/candidate/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9194, '候选人新增', 'hrm:recruit:candidate:create', 3, 1, 9193, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9195, '候选人修改', 'hrm:recruit:candidate:update', 3, 2, 9193, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9196, '候选人删除', 'hrm:recruit:candidate:delete', 3, 3, 9193, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9197, '面试新增', 'hrm:recruit:interview:create', 3, 4, 9193, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9198, '面试修改', 'hrm:recruit:interview:update', 3, 5, 9193, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9199, '招聘渠道', 'hrm:recruit:channel:query', 2, 11, 9100, 'recruit/channel', 'ep:share', 'hrm/recruit/channel/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- 招聘渠道/配置按钮（渠道挂在 9199 下；配置按钮挂招聘渠道菜单便于授权）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9200 AS `id`, '渠道新增', 'hrm:recruit:channel:create' AS `permission`, 3 AS `type`, 1 AS `sort`, 9199 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9201, '渠道修改', 'hrm:recruit:channel:update', 3, 2, 9199, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9202, '渠道删除', 'hrm:recruit:channel:delete', 3, 3, 9199, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9203, '淘汰原因配置', 'hrm:recruit:config:update', 3, 4, 9199, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` = 3 AND m.`permission` = t.`permission` AND m.`type` = 3) )
);

-- ========== 二、字典类型（10 个） ==========

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT t.* FROM (
  SELECT 'HRM 招聘职位状态' AS `name`, 'hrm_recruit_post_status' AS `type`, 0 AS `status`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, NULL AS `deleted_time`
  UNION ALL SELECT 'HRM 招聘工作性质', 'hrm_recruit_job_nature', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 招聘工作经验', 'hrm_recruit_work_time', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 招聘职位学历要求', 'hrm_recruit_post_education', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 招聘薪资单位', 'hrm_recruit_salary_unit', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 招聘紧急程度', 'hrm_recruit_emergency_level', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 招聘候选人状态', 'hrm_recruit_candidate_status', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 招聘候选人学历', 'hrm_recruit_candidate_education', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 招聘面试方式', 'hrm_recruit_interview_type', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 招聘面试结果', 'hrm_recruit_interview_result', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_type` e WHERE e.`type` = t.`type` AND e.`deleted` = b'0'
);

-- ========== 三、字典数据（值域对齐前端 constants.ts 枚举） ==========

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  -- 职位状态（HrmRecruitPostStatus：0停止 1招聘中）
  SELECT 1 AS `sort`, '停止招聘' AS `label`, '0' AS `value`, 'hrm_recruit_post_status' AS `dict_type`, 0 AS `status`, 'info' AS `color_type`, '' AS `css_class`, '' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 2, '招聘中', '1', 'hrm_recruit_post_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 工作性质（1全职 2实习 3兼职）
  UNION ALL SELECT 1, '全职', '1', 'hrm_recruit_job_nature', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '实习', '2', 'hrm_recruit_job_nature', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '兼职', '3', 'hrm_recruit_job_nature', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 工作经验（1不限 2一年以下 3一至三年 4三至五年 5五至十年 6十年以上）
  UNION ALL SELECT 1, '不限', '1', 'hrm_recruit_work_time', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '1年以下', '2', 'hrm_recruit_work_time', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '1-3年', '3', 'hrm_recruit_work_time', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '3-5年', '4', 'hrm_recruit_work_time', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 5, '5-10年', '5', 'hrm_recruit_work_time', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 6, '10年以上', '6', 'hrm_recruit_work_time', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 学历要求（1不限 2高中及以上 3大专及以上 4本科及以上 5硕士及以上 6博士）
  UNION ALL SELECT 1, '不限', '1', 'hrm_recruit_post_education', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '高中及以上', '2', 'hrm_recruit_post_education', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '大专及以上', '3', 'hrm_recruit_post_education', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '本科及以上', '4', 'hrm_recruit_post_education', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 5, '硕士及以上', '5', 'hrm_recruit_post_education', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 6, '博士', '6', 'hrm_recruit_post_education', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 薪资单位（1元/月 2元/年）
  UNION ALL SELECT 1, '元/月', '1', 'hrm_recruit_salary_unit', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '元/年', '2', 'hrm_recruit_salary_unit', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 紧急程度（1紧急 2一般）
  UNION ALL SELECT 1, '紧急', '1', 'hrm_recruit_emergency_level', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '一般', '2', 'hrm_recruit_emergency_level', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 候选人状态（1新候选人 2初选通过 3安排面试 4面试通过 5已发Offer 6待入职 7已淘汰 8已入职）
  UNION ALL SELECT 1, '新候选人', '1', 'hrm_recruit_candidate_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '初选通过', '2', 'hrm_recruit_candidate_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '安排面试', '3', 'hrm_recruit_candidate_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '面试通过', '4', 'hrm_recruit_candidate_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 5, '已发Offer', '5', 'hrm_recruit_candidate_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 6, '待入职', '6', 'hrm_recruit_candidate_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 7, '已淘汰', '7', 'hrm_recruit_candidate_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 8, '已入职', '8', 'hrm_recruit_candidate_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 候选人学历（1小学 2初中 3高中 4大专 5本科 6硕士 7博士）
  UNION ALL SELECT 1, '小学', '1', 'hrm_recruit_candidate_education', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '初中', '2', 'hrm_recruit_candidate_education', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '高中', '3', 'hrm_recruit_candidate_education', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '大专', '4', 'hrm_recruit_candidate_education', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 5, '本科', '5', 'hrm_recruit_candidate_education', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 6, '硕士', '6', 'hrm_recruit_candidate_education', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 7, '博士', '7', 'hrm_recruit_candidate_education', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 面试方式（1现场 2电话 3视频）
  UNION ALL SELECT 1, '现场面试', '1', 'hrm_recruit_interview_type', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '电话面试', '2', 'hrm_recruit_interview_type', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '视频面试', '3', 'hrm_recruit_interview_type', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 面试结果（1未完成 2通过 3未通过 4取消）
  UNION ALL SELECT 1, '未完成', '1', 'hrm_recruit_interview_result', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '通过', '2', 'hrm_recruit_interview_result', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '未通过', '3', 'hrm_recruit_interview_result', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '取消', '4', 'hrm_recruit_interview_result', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data` e
  WHERE e.`dict_type` = t.`dict_type` AND e.`value` = t.`value` AND e.`deleted` = b'0'
);
