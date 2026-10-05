-- ============================================================
-- V140：整体移除商业域（会员/积分/充值/钱包/佣金/商城交易）
-- ============================================================
-- 所有者裁决（2026-10-05）：本项目为个人自研、不做商用，「商业层面」=
-- 会员/积分/充值/钱包/佣金等商城资金会员域，整体移除。
--
-- 依赖探测读数（移除可行性依据，全部实测）：
--   1. 企业域 15 个模块（erp/mes/wms/qms/crm/hr/oa/tms/iot/bpm/report/datalake/ai/im/mp）
--      对 member/pay/trade/product/promotion 的 import 数 = 0；
--   2. iot 与 mall 无任何接线（「② iot+mall 最小闭环」是两条独立验证，非集成）；
--   3. 全库视图无一 FROM 商业表（V126 三张视图只读 crm/hr/oa/qms/wms；
--      正则初筛命中的 v_qms_inspection_status_daily 只是 QMS 表上有名为
--      product_id 的列，属列名误报，已逐一排除）；
--   4. 租户套餐 menu_ids CSV 对商业菜单引用 = 0；
--   5. demo-data/ 对商城/会员/积分零命中（商城演示数据全部在 V123 迁移内种子）。
--
-- 为什么用「反向清理」而不是改历史迁移：既有 V*.sql 正文已被哈希钉住不许改，
-- 且 V1.1/V123/V125 里的商业菜单与建表是既有事实；本迁移在其后执行达成净效果，
-- 从零建库同样收敛到无商业域。
--
-- 菜单删除用递归 CTE 闭包（目录 + 全部子孙），避免依赖每个子菜单都命中前缀/名称；
-- 表与字典用 SUBSTRING_INDEX 前缀判定，不用 LIKE 通配符转义。
--
-- 移除的是：DROP 74 张表 + 商业菜单及角色引用 + 商业字典。**不触碰企业域任何表。**

-- 1. 商业域表（information_schema 实测清单，共 74 张）
-- member（11 张）
DROP TABLE IF EXISTS `member_address`;
DROP TABLE IF EXISTS `member_config`;
DROP TABLE IF EXISTS `member_experience_record`;
DROP TABLE IF EXISTS `member_group`;
DROP TABLE IF EXISTS `member_level`;
DROP TABLE IF EXISTS `member_level_record`;
DROP TABLE IF EXISTS `member_point_record`;
DROP TABLE IF EXISTS `member_sign_in_config`;
DROP TABLE IF EXISTS `member_sign_in_record`;
DROP TABLE IF EXISTS `member_tag`;
DROP TABLE IF EXISTS `member_user`;
-- pay（14 张）
DROP TABLE IF EXISTS `pay_app`;
DROP TABLE IF EXISTS `pay_channel`;
DROP TABLE IF EXISTS `pay_demo_order`;
DROP TABLE IF EXISTS `pay_demo_withdraw`;
DROP TABLE IF EXISTS `pay_notify_log`;
DROP TABLE IF EXISTS `pay_notify_task`;
DROP TABLE IF EXISTS `pay_order`;
DROP TABLE IF EXISTS `pay_order_extension`;
DROP TABLE IF EXISTS `pay_refund`;
DROP TABLE IF EXISTS `pay_transfer`;
DROP TABLE IF EXISTS `pay_wallet`;
DROP TABLE IF EXISTS `pay_wallet_recharge`;
DROP TABLE IF EXISTS `pay_wallet_recharge_package`;
DROP TABLE IF EXISTS `pay_wallet_transaction`;
-- product（10 张）
DROP TABLE IF EXISTS `product_brand`;
DROP TABLE IF EXISTS `product_browse_history`;
DROP TABLE IF EXISTS `product_category`;
DROP TABLE IF EXISTS `product_comment`;
DROP TABLE IF EXISTS `product_favorite`;
DROP TABLE IF EXISTS `product_property`;
DROP TABLE IF EXISTS `product_property_value`;
DROP TABLE IF EXISTS `product_sku`;
DROP TABLE IF EXISTS `product_spu`;
DROP TABLE IF EXISTS `product_statistics`;
-- promotion（23 张）
DROP TABLE IF EXISTS `promotion_article`;
DROP TABLE IF EXISTS `promotion_article_category`;
DROP TABLE IF EXISTS `promotion_banner`;
DROP TABLE IF EXISTS `promotion_bargain_activity`;
DROP TABLE IF EXISTS `promotion_bargain_help`;
DROP TABLE IF EXISTS `promotion_bargain_record`;
DROP TABLE IF EXISTS `promotion_combination_activity`;
DROP TABLE IF EXISTS `promotion_combination_product`;
DROP TABLE IF EXISTS `promotion_combination_record`;
DROP TABLE IF EXISTS `promotion_coupon`;
DROP TABLE IF EXISTS `promotion_coupon_template`;
DROP TABLE IF EXISTS `promotion_discount_activity`;
DROP TABLE IF EXISTS `promotion_discount_product`;
DROP TABLE IF EXISTS `promotion_diy_page`;
DROP TABLE IF EXISTS `promotion_diy_template`;
DROP TABLE IF EXISTS `promotion_kefu_conversation`;
DROP TABLE IF EXISTS `promotion_kefu_message`;
DROP TABLE IF EXISTS `promotion_point_activity`;
DROP TABLE IF EXISTS `promotion_point_product`;
DROP TABLE IF EXISTS `promotion_reward_activity`;
DROP TABLE IF EXISTS `promotion_seckill_activity`;
DROP TABLE IF EXISTS `promotion_seckill_config`;
DROP TABLE IF EXISTS `promotion_seckill_product`;
-- trade（16 张）
DROP TABLE IF EXISTS `trade_after_sale`;
DROP TABLE IF EXISTS `trade_after_sale_log`;
DROP TABLE IF EXISTS `trade_brokerage_record`;
DROP TABLE IF EXISTS `trade_brokerage_user`;
DROP TABLE IF EXISTS `trade_brokerage_withdraw`;
DROP TABLE IF EXISTS `trade_cart`;
DROP TABLE IF EXISTS `trade_config`;
DROP TABLE IF EXISTS `trade_delivery_express`;
DROP TABLE IF EXISTS `trade_delivery_express_template`;
DROP TABLE IF EXISTS `trade_delivery_express_template_charge`;
DROP TABLE IF EXISTS `trade_delivery_express_template_free`;
DROP TABLE IF EXISTS `trade_delivery_pick_up_store`;
DROP TABLE IF EXISTS `trade_order`;
DROP TABLE IF EXISTS `trade_order_item`;
DROP TABLE IF EXISTS `trade_order_log`;
DROP TABLE IF EXISTS `trade_statistics`;

-- 2. 商业菜单闭包（含角色关联），先收后删
CREATE TEMPORARY TABLE tmp_commercial_menu (id BIGINT PRIMARY KEY);
INSERT INTO tmp_commercial_menu
WITH RECURSIVE comm AS (
    SELECT id FROM system_menu WHERE deleted = b'0'
      AND ( permission LIKE 'trade:%' OR permission LIKE 'pay:%' OR permission LIKE 'member:%'
         OR permission LIKE 'product:%' OR permission LIKE 'promotion:%'
         OR name LIKE '%商城%' OR name LIKE '%会员%' OR name LIKE '%支付%'
         OR name LIKE '%分销%' OR name LIKE '%积分%' OR name LIKE '%钱包%' )
    UNION ALL
    SELECT c.id FROM system_menu c JOIN comm p ON c.parent_id = p.id AND c.deleted = b'0'
) SELECT DISTINCT id FROM comm;  -- 基础命中与父链递归会从两条路产出同一 id，DISTINCT 收口（真机踩过：Duplicate entry '1132'）
DELETE rm FROM system_role_menu rm JOIN tmp_commercial_menu t ON rm.menu_id = t.id;
DELETE m FROM system_menu m JOIN tmp_commercial_menu t ON m.id = t.id;
DROP TEMPORARY TABLE tmp_commercial_menu;

-- 3. 商业字典（先数据后类型）
DELETE FROM system_dict_data WHERE SUBSTRING_INDEX(dict_type,'_',1) IN ('pay','trade','member','product','promotion');
DELETE FROM system_dict_type WHERE deleted = b'0' AND SUBSTRING_INDEX(type,'_',1) IN ('pay','trade','member','product','promotion');

-- 自检 1：商业域表清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V140_NO_COMMERCIAL_TABLES PASS'
            ELSE CONCAT('V140_NO_COMMERCIAL_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE()
   AND SUBSTRING_INDEX(table_name,'_',1) IN ('pay','trade','member','product','promotion');

-- 自检 2：商业菜单清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V140_NO_COMMERCIAL_MENUS PASS'
            ELSE CONCAT('V140_NO_COMMERCIAL_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM system_menu WHERE deleted = b'0'
   AND ( permission LIKE 'trade:%' OR permission LIKE 'pay:%' OR permission LIKE 'member:%'
      OR permission LIKE 'product:%' OR permission LIKE 'promotion:%'
      OR name LIKE '%商城%' OR name LIKE '%会员%' OR name LIKE '%支付%'
      OR name LIKE '%分销%' OR name LIKE '%积分%' OR name LIKE '%钱包%' );

-- 自检 3：菜单树无孤儿（父被删子残留会在这暴露）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V140_NO_ORPHAN_MENUS PASS'
            ELSE CONCAT('V140_NO_ORPHAN_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM system_menu m WHERE m.deleted = b'0' AND m.parent_id <> 0
   AND NOT EXISTS (SELECT 1 FROM system_menu p WHERE p.id = m.parent_id AND p.deleted = b'0');

-- 自检 4：商业字典清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V140_NO_COMMERCIAL_DICTS PASS'
            ELSE CONCAT('V140_NO_COMMERCIAL_DICTS FAIL ', COUNT(*)) END AS check_result
  FROM system_dict_type WHERE deleted = b'0'
   AND SUBSTRING_INDEX(type,'_',1) IN ('pay','trade','member','product','promotion');

-- 自检 5：企业域完好（防止误删——企业域表必须在且数量可观）
SELECT CASE WHEN COUNT(*) > 200 THEN CONCAT('V140_ENTERPRISE_TABLES_INTACT PASS ', COUNT(*))
            ELSE CONCAT('V140_ENTERPRISE_TABLES_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE()
   AND SUBSTRING_INDEX(table_name,'_',1) IN ('system','infra','bpm','erp','mes','wms','qms','crm','hr','oa','tms','iot','report','datalake','ai');
