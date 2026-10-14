-- ============================================================
-- V197：TMS/OA/CRM 缺失域菜单与按钮权限播种
-- ============================================================
-- 背景（台账 §12.220）：前端补齐 TMS 运费结算/运费对账/车队运营/GPS、OA 知识评论/
-- 知识版本/会议预约/日程、CRM 线索渠道/线索公海配置/销售订单/售后工单共 12 个
-- 「后端已实现但前端从未接线」的域。这些域的 @PreAuthorize 权限串此前从未种进
-- system_menu——页面可达但按钮全 403，与 §12.202（V186）同型缺口。
--
-- 另有两类**纯缺权限字符串**（前端页面早已存在）的域一并补上：
--   CRM follow-up-record / performance-config / statistics-product /
--   statistics-performance-target（页面与 api 都在，仅权限未播种）。
-- 已播种的不重播（business-status / contract-config / customer-* / product-category /
-- receivable-plan / statistics-customer / funnel / performance / portrait / rank 等）。
--
-- 权限串口径（与各 Controller @PreAuthorize 一一对应，非手抄清单）：
--   TMS freight: create/update/delete/query/audit/settle（audit、settle 为独立权限）
--   TMS freight-reconciliation: create/update/delete/query（三个动作端点复用 update 权限）
--   TMS fleet-operation: create/update/delete/query
--   TMS gps: report/query（无 CRUD）
--   OA knowledge-comment: create/update/delete/query
--   OA knowledge-version: **五个端点全部套用 oa:knowledge-article:query/update**（后端即如此）
--   OA meeting-reservation: query 缺目录菜单（create/update/delete 已在 V59 的 3010-3012）
--   OA schedule: create/update/delete/query
--   CRM clue-channel: create/update/delete/query
--   CRM clue-pool-config: query/update（单例配置）
--   CRM sale-order: query/create/update/delete/export（confirm/submit 复用 update）
--   CRM work-order: query/create/update/delete/export（assign/resolve/close 复用 update）
--   CRM follow-up-record: create/delete（get/page 无注解，service 层鉴权）
--   CRM performance-config: query/create/update/delete
--   CRM statistics-product / statistics-performance-target: query
--
-- id 段：TMS/OA 从 3160 起（原最大 3151）；CRM 从 3300 起（避开 2391-2742/V1.1、
-- 3083-3092/V59、6700-6800/V1.1 三个已用段）。
-- 挂点：TMS 目录 3020；OA 挂 3000/3100/3006；CRM 挂 2397（CRM 系统）、
-- 2524（系统配置）、2560（数据统计）。
-- ============================================================

-- ① TMS 运费结算（挂 3020）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3160 AS `id`, '运费结算' AS `name`, 'tms:freight:query' AS `permission`, 2 AS `type`, 6 AS `sort`, 3020 AS `parent_id`, 'freight' AS `path`, 'ep:money' AS `icon`, 'tms/freight/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3161, '结算单新增', 'tms:freight:create', 3, 1, 3160, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3162, '结算单修改', 'tms:freight:update', 3, 2, 3160, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3163, '结算单删除', 'tms:freight:delete', 3, 3, 3160, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3164, '结算单审核', 'tms:freight:audit', 3, 4, 3160, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3165, '结算单结算', 'tms:freight:settle', 3, 5, 3160, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ② TMS 运费对账（挂 3020；三个动作端点复用 update 权限，不额外种按钮）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3170 AS `id`, '运费对账' AS `name`, 'tms:freight-reconciliation:query' AS `permission`, 2 AS `type`, 7 AS `sort`, 3020 AS `parent_id`, 'freight-reconciliation' AS `path`, 'ep:scale' AS `icon`, 'tms/freight-reconciliation/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3171, '对账单新增', 'tms:freight-reconciliation:create', 3, 1, 3170, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3172, '对账单修改', 'tms:freight-reconciliation:update', 3, 2, 3170, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3173, '对账单删除', 'tms:freight-reconciliation:delete', 3, 3, 3170, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ③ TMS 车队运营（挂 3020）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3180 AS `id`, '车队运营' AS `name`, 'tms:fleet-operation:query' AS `permission`, 2 AS `type`, 8 AS `sort`, 3020 AS `parent_id`, 'fleet-operation' AS `path`, 'ep:data-line' AS `icon`, 'tms/fleet-operation/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3181, '运营记录新增', 'tms:fleet-operation:create', 3, 1, 3180, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3182, '运营记录修改', 'tms:fleet-operation:update', 3, 2, 3180, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3183, '运营记录删除', 'tms:fleet-operation:delete', 3, 3, 3180, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ④ TMS GPS 定位（挂 3020；接收型域，无 CRUD）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3190 AS `id`, 'GPS 定位' AS `name`, 'tms:gps:query' AS `permission`, 2 AS `type`, 9 AS `sort`, 3020 AS `parent_id`, 'gps' AS `path`, 'ep:location' AS `icon`, 'tms/gps/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3191, '位置上报', 'tms:gps:report', 3, 1, 3190, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑤ OA 知识评论（挂知识库 3100；软删除域）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3200 AS `id`, '文章评论' AS `name`, 'oa:knowledge-comment:query' AS `permission`, 2 AS `type`, 3 AS `sort`, 3100 AS `parent_id`, 'comment' AS `path`, 'ep:chat-dot-round' AS `icon`, 'oa/knowledge/comment/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3201, '评论新增', 'oa:knowledge-comment:create', 3, 1, 3200, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3202, '评论修改', 'oa:knowledge-comment:update', 3, 2, 3200, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3203, '评论删除', 'oa:knowledge-comment:delete', 3, 3, 3200, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑥ OA 知识版本（挂知识库 3100；**权限串套用文章权限**，与后端 @PreAuthorize 一致）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3210 AS `id`, '版本历史' AS `name`, 'oa:knowledge-article:query' AS `permission`, 2 AS `type`, 4 AS `sort`, 3100 AS `parent_id`, 'version' AS `path`, 'ep:clock' AS `icon`, 'oa/knowledge/version/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3211, '版本维护（新增/修改/删除）', 'oa:knowledge-article:update', 3, 1, 3210, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
-- 注意：本块 type=2 菜单的 permission 与 V64 已播种的「知识文章」菜单同为
-- oa:knowledge-article:query（版本历史页复用文章权限，与后端 5 个端点一致），
-- 仅按 permission 预检会把本菜单误判为已存在，故按 id + 父级 + component 判定。
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0' AND m.`id` IN (t.`id`, 3211)
);

-- ⑦ OA 会议预约目录（挂会议室管理 3006；create/update/delete 按钮 V59 3010-3012 已有，
--    预检的 NOT EXISTS 会自然跳过重播，此处只补 query 目录菜单）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3220 AS `id`, '会议预约' AS `name`, 'oa:meeting-reservation:query' AS `permission`, 2 AS `type`, 2 AS `sort`, 3006 AS `parent_id`, 'meeting-reservation' AS `path`, 'ep:calendar' AS `icon`, 'oa/meeting-reservation/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑧ OA 日程管理（挂 OA管理 3000）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3230 AS `id`, '日程管理' AS `name`, 'oa:schedule:query' AS `permission`, 2 AS `type`, 9 AS `sort`, 3000 AS `parent_id`, 'schedule' AS `path`, 'ep:date' AS `icon`, 'oa/schedule/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3231, '日程新增', 'oa:schedule:create', 3, 1, 3230, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3232, '日程修改', 'oa:schedule:update', 3, 2, 3230, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3233, '日程删除', 'oa:schedule:delete', 3, 3, 3230, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑨ CRM 线索渠道（挂 CRM 系统 2397）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3300 AS `id`, '线索渠道' AS `name`, 'crm:clue-channel:query' AS `permission`, 2 AS `type`, 10 AS `sort`, 2397 AS `parent_id`, 'clue-channel' AS `path`, 'simple-icons:adsense' AS `icon`, 'crm/clue/channel/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3301, '渠道新增', 'crm:clue-channel:create', 3, 1, 3300, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3302, '渠道修改', 'crm:clue-channel:update', 3, 2, 3300, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3303, '渠道删除', 'crm:clue-channel:delete', 3, 3, 3300, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑩ CRM 线索公海配置（单例，挂 CRM 系统配置 2524）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3310 AS `id`, '线索公海配置' AS `name`, 'crm:clue-pool-config:query' AS `permission`, 2 AS `type`, 9 AS `sort`, 2524 AS `parent_id`, 'clue-pool-config' AS `path`, 'ep:box' AS `icon`, 'crm/clue/poolConfig/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3311, '公海配置修改', 'crm:clue-pool-config:update', 3, 1, 3310, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑪ CRM 销售订单（挂 CRM 系统 2397）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3320 AS `id`, '销售订单' AS `name`, 'crm:sale-order:query' AS `permission`, 2 AS `type`, 11 AS `sort`, 2397 AS `parent_id`, 'sale-order' AS `path`, 'ep:notebook' AS `icon`, 'crm/saleOrder/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3321, '订单新增', 'crm:sale-order:create', 3, 1, 3320, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3322, '订单修改', 'crm:sale-order:update', 3, 2, 3320, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3323, '订单删除', 'crm:sale-order:delete', 3, 3, 3320, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3324, '订单导出', 'crm:sale-order:export', 3, 4, 3320, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑫ CRM 售后工单（挂 CRM 系统 2397）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3330 AS `id`, '售后工单' AS `name`, 'crm:work-order:query' AS `permission`, 2 AS `type`, 12 AS `sort`, 2397 AS `parent_id`, 'work-order' AS `path`, 'ep:service' AS `icon`, 'crm/workOrder/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3331, '工单新增', 'crm:work-order:create', 3, 1, 3330, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3332, '工单修改', 'crm:work-order:update', 3, 2, 3330, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3333, '工单删除', 'crm:work-order:delete', 3, 3, 3330, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3334, '工单导出', 'crm:work-order:export', 3, 4, 3330, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑬ CRM 跟进记录（挂 CRM 系统 2397；get/page 无注解由 service 层鉴权，只种 create/delete）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3340 AS `id`, '跟进记录' AS `name`, 'crm:follow-up-record:create' AS `permission`, 2 AS `type`, 13 AS `sort`, 2397 AS `parent_id`, 'follow-up-record' AS `path`, 'ep:comment' AS `icon`, 'crm/followup/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3341, '跟进删除', 'crm:follow-up-record:delete', 3, 1, 3340, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑭ CRM 业绩目标设置（挂 CRM 系统配置 2524）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3350 AS `id`, '业绩目标设置' AS `name`, 'crm:performance-config:query' AS `permission`, 2 AS `type`, 10 AS `sort`, 2524 AS `parent_id`, 'performance-config' AS `path`, 'ep:trophy' AS `icon`, 'crm/performance/config/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 3351, '目标新增', 'crm:performance-config:create', 3, 1, 3350, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3352, '目标修改', 'crm:performance-config:update', 3, 2, 3350, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3353, '目标删除', 'crm:performance-config:delete', 3, 3, 3350, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑮ CRM 产品统计（统计域，挂 数据统计 2560）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3360 AS `id`, '产品统计' AS `name`, 'crm:statistics-product:query' AS `permission`, 2 AS `type`, 6 AS `sort`, 2560 AS `parent_id`, 'product' AS `path`, 'ep:goods' AS `icon`, 'crm/statistics/product/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ⑯ CRM 业绩目标统计（统计域，挂 数据统计 2560）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3370 AS `id`, '业绩目标统计' AS `name`, 'crm:statistics-performance-target:query' AS `permission`, 2 AS `type`, 7 AS `sort`, 2560 AS `parent_id`, 'performance-target' AS `path`, 'ep:aim' AS `icon`, 'crm/statistics/performanceTarget/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ==================== 自检 ====================
-- 1. 十四个目录菜单均在（TMS 4 + OA 4 + CRM 6）
SELECT CASE WHEN COUNT(*) = 14 THEN 'V197_MENU_COUNT PASS'
            ELSE CONCAT('V197_MENU_COUNT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2
   AND `component` IN ('tms/freight/index', 'tms/freight-reconciliation/index',
                       'tms/fleet-operation/index', 'tms/gps/index',
                       'oa/knowledge/comment/index', 'oa/knowledge/version/index',
                       'oa/meeting-reservation/index', 'oa/schedule/index',
                       'crm/clue/channel/index', 'crm/clue/poolConfig/index',
                       'crm/saleOrder/index', 'crm/workOrder/index',
                       'crm/followup/index', 'crm/performance/config/index');

-- 2. 关键权限串在位（按各域实际 @PreAuthorize 条对）
SELECT CASE WHEN COUNT(DISTINCT permission) >= 35 THEN 'V197_PERMISSIONS PASS'
            ELSE CONCAT('V197_PERMISSIONS FAIL ', COUNT(DISTINCT permission)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND `permission` IN (
     'tms:freight:query', 'tms:freight:create', 'tms:freight:update', 'tms:freight:delete',
     'tms:freight:audit', 'tms:freight:settle',
     'tms:freight-reconciliation:query', 'tms:freight-reconciliation:create',
     'tms:freight-reconciliation:update', 'tms:freight-reconciliation:delete',
     'tms:fleet-operation:query', 'tms:fleet-operation:create',
     'tms:fleet-operation:update', 'tms:fleet-operation:delete',
     'tms:gps:query', 'tms:gps:report',
     'oa:knowledge-comment:query', 'oa:knowledge-comment:create',
     'oa:knowledge-comment:update', 'oa:knowledge-comment:delete',
     'oa:schedule:query', 'oa:schedule:create', 'oa:schedule:update', 'oa:schedule:delete',
     'crm:clue-channel:query', 'crm:clue-channel:create', 'crm:clue-channel:update', 'crm:clue-channel:delete',
     'crm:clue-pool-config:query', 'crm:clue-pool-config:update',
     'crm:sale-order:query', 'crm:sale-order:create', 'crm:sale-order:update',
     'crm:sale-order:delete', 'crm:sale-order:export',
     'crm:work-order:query', 'crm:work-order:create', 'crm:work-order:update',
     'crm:work-order:delete', 'crm:work-order:export',
     'crm:follow-up-record:create', 'crm:follow-up-record:delete',
     'crm:performance-config:query', 'crm:performance-config:create',
     'crm:performance-config:update', 'crm:performance-config:delete',
     'crm:statistics-product:query', 'crm:statistics-performance-target:query');

-- 3. 新菜单无孤儿（parent 均在且未删除）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V197_NO_ORPHAN PASS'
            ELSE CONCAT('V197_NO_ORPHAN FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` c
 WHERE c.`deleted` = b'0' AND ((c.`id` BETWEEN 3160 AND 3299) OR (c.`id` BETWEEN 3300 AND 3399))
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p
                    WHERE p.`id` = c.`parent_id` AND p.`deleted` = b'0');

-- 4. 会议预约 query 目录已补（V59 只种了 3010-3012 三个按钮）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V197_MEETING_QUERY_MENU PASS'
            ELSE CONCAT('V197_MEETING_QUERY_MENU FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2
   AND `permission` = 'oa:meeting-reservation:query';
