-- ============================================================
-- V209：FMS 期初余额重建——ERP 账套口径新表 + 菜单播种（C6③，ADR 265）
-- ============================================================
-- 背景（台账 §12.180/§12.210、ADR 265）：FMS/ERP 财务融合（V190）删掉的死页
-- `fms/config/initial-balance`（期初余额）按重建矩阵 C6③ 重建。设计裁定：
--   1. 单一真相落 ERP 模块新表 erp_gl_initial_balance（账套口径），FMS 前端页
--      直连 ERP /erp/gl-initial-balance/* 端点（V194 科目先例，不建平行表/代理层）；
--   2. 粒度 (tenant_id, account_set_id, account_id)：多账套隔离的关键。
--      erp_gl_account 的 opening_*/current_*/closing_* 是租户口径、无账套与期间
--      维度，不能承载本需求——两轨并存、暂不互写（ADR 265 §3 裁定 3，已登记）；
--      account_set_id 弱引用 fms_account_set（无外键，V191 账簿先例）；
--   3. 一期仅金额式：数量核算 / 辅助核算依赖链（科目配置 → 分录列 → 录入 UI）
--      全链未建，整链砍掉并登记；「年初余额」为派生只读，不入库；
--   4. 并发安全：唯一键导 upsert 幂等（V199 唯一键口径）；保存护栏=账套经账簿链
--      无已审核凭证（服务端强校验，前端仅按钮可用性）。
-- 索引取舍：账套维度查询 (tenant_id, account_set_id) 由唯一键**前缀**承载，
-- 不单建冗余索引（只增写放大，无查询收益）。
-- 本迁移内容：建表 + 自检 +「期初余额」菜单播种（挂 FMS 目录 9000）。
-- 写法沿用 V194 口径：幂等插入（id + permission+type 预检），system_menu 无 tenant_id。
-- ============================================================

-- 1. 建表：ERP 期初余额（账套口径，金额式）
CREATE TABLE IF NOT EXISTS `erp_gl_initial_balance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT 'FMS 账套编号（弱引用 fms_account_set.id，无外键）',
  `account_id` bigint NOT NULL COMMENT '会计科目编号（erp_gl_account.id，末级科目）',
  `opening_amount` decimal(20,4) NOT NULL DEFAULT 0 COMMENT '期初余额',
  `year_debit_amount` decimal(20,4) NOT NULL DEFAULT 0 COMMENT '本年累计借方发生额',
  `year_credit_amount` decimal(20,4) NOT NULL DEFAULT 0 COMMENT '本年累计贷方发生额',
  `profit_loss_amount` decimal(20,4) NOT NULL DEFAULT 0 COMMENT '实际损益发生额（损益类科目）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_erp_gl_initial_balance_tenant_set_account` (`tenant_id`, `account_set_id`, `account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 期初余额表（账套口径）';

-- 2. 自检 1：新表在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V209_TABLE_PASS'
            ELSE CONCAT('V209_TABLE_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_initial_balance';

-- 3. 自检 2：四金额列齐
SELECT CASE WHEN COUNT(*) = 4 THEN 'V209_AMOUNT_COLUMNS_PASS'
            ELSE CONCAT('V209_AMOUNT_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_initial_balance'
   AND column_name IN ('opening_amount', 'year_debit_amount', 'year_credit_amount', 'profit_loss_amount');

-- 4. 自检 3：唯一键在（COUNT(DISTINCT index_name) 口径，V192 固化）
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V209_UNIQUE_KEY_PASS'
            ELSE CONCAT('V209_UNIQUE_KEY_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_initial_balance'
   AND index_name = 'uk_erp_gl_initial_balance_tenant_set_account';

-- 5. 期初余额菜单与按钮（FMS 树 9090；权限串 erp:gl-initial-balance:* 与 ERP 后端
--    @PreAuthorize 一一对应——V194 口径：权限的单一真相在 ERP，不造 fms:* 平行串；
--    模板下载复用 import 权限，不单设按钮）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9090 AS `id`, '期初余额' AS `name`, 'erp:gl-initial-balance:query' AS `permission`, 2 AS `type`, 9 AS `sort`, 9000 AS `parent_id`, 'config/initial-balance' AS `path`, 'ep:money' AS `icon`, 'fms/config/initial-balance/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9091, '期初余额保存', 'erp:gl-initial-balance:update', 3, 1, 9090, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9092, '期初余额导入', 'erp:gl-initial-balance:import', 3, 2, 9090, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9093, '期初余额导出', 'erp:gl-initial-balance:export', 3, 3, 9090, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- 6. 自检 4：菜单与按钮权限串在位（1 菜单 + 3 按钮 = 4 个唯一 permission）
SELECT CASE WHEN COUNT(DISTINCT permission) = 4 THEN 'V209_MENU_PASS'
            ELSE CONCAT('V209_MENU_FAIL ', COUNT(DISTINCT permission)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND `permission` IN ('erp:gl-initial-balance:query', 'erp:gl-initial-balance:update',
                        'erp:gl-initial-balance:import', 'erp:gl-initial-balance:export');

-- 7. 自检 5：菜单组件指向前端页面；FMS 侧不得出现平行权限串（防回归）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V209_MENU_COMPONENT_PASS'
            ELSE CONCAT('V209_MENU_COMPONENT_FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `component` = 'fms/config/initial-balance/index';
SELECT CASE WHEN COUNT(*) = 0 THEN 'V209_NO_PARALLEL_PERMISSION_PASS'
            ELSE CONCAT('V209_NO_PARALLEL_PERMISSION_FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `permission` LIKE 'fms:config:initial-balance%';
