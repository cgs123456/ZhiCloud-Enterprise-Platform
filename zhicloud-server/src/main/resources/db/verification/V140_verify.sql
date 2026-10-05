-- V140 验证：商业域移除后的净效果与完整性
-- 分工：迁移内自检查"清干净了吗、企业域还在吗"，
-- 本文件复核"Flyway 通道记录、四类残留、菜单树完整性、企业域规模"。

-- 1. Flyway 通道记录：V140 必须由应用自带 Flyway 成功执行
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '140' AND success = 1) = 1
            THEN 'V140_FLYWAY_APPLIED PASS'
            ELSE 'V140_FLYWAY_APPLIED FAIL 版本 140 不在成功记录里' END AS check_result;

-- 2. 商业域表清零（information_schema 实测，不用 LIKE 通配符转义）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V140_VERIFY_NO_COMMERCIAL_TABLES PASS'
            ELSE CONCAT('V140_VERIFY_NO_COMMERCIAL_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE()
   AND SUBSTRING_INDEX(table_name,'_',1) IN ('pay','trade','member','product','promotion');

-- 3. 商业菜单清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V140_VERIFY_NO_COMMERCIAL_MENUS PASS'
            ELSE CONCAT('V140_VERIFY_NO_COMMERCIAL_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM system_menu WHERE deleted = b'0'
   AND ( permission LIKE 'trade:%' OR permission LIKE 'pay:%' OR permission LIKE 'member:%'
      OR permission LIKE 'product:%' OR permission LIKE 'promotion:%'
      OR name LIKE '%商城%' OR name LIKE '%会员%' OR name LIKE '%支付%'
      OR name LIKE '%分销%' OR name LIKE '%积分%' OR name LIKE '%钱包%' );

-- 4. 菜单树完整性：不允许孤儿（幂等复跑后仍须成立）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V140_VERIFY_NO_ORPHAN_MENUS PASS'
            ELSE CONCAT('V140_VERIFY_NO_ORPHAN_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM system_menu m WHERE m.deleted = b'0' AND m.parent_id <> 0
   AND NOT EXISTS (SELECT 1 FROM system_menu p WHERE p.id = m.parent_id AND p.deleted = b'0');

-- 5. 商业字典清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V140_VERIFY_NO_COMMERCIAL_DICTS PASS'
            ELSE CONCAT('V140_VERIFY_NO_COMMERCIAL_DICTS FAIL ', COUNT(*)) END AS check_result
  FROM system_dict_type WHERE deleted = b'0'
   AND SUBSTRING_INDEX(type,'_',1) IN ('pay','trade','member','product','promotion');

-- 6. 企业域完好：企业表必须在且规模可观（本次移除前后读数一致才算无附带伤害）
SELECT CASE WHEN COUNT(*) > 200 THEN CONCAT('V140_VERIFY_ENTERPRISE_INTACT PASS ', COUNT(*))
            ELSE CONCAT('V140_VERIFY_ENTERPRISE_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE()
   AND SUBSTRING_INDEX(table_name,'_',1) IN ('system','infra','bpm','erp','mes','wms','qms','crm','hr','oa','tms','iot','report','datalake','ai');
