-- V156 验证：IM 即时通讯 im_* 17 张表全部存在
SELECT CASE WHEN COUNT(*) = 17 THEN 'V156_IM_TABLES PASS'
            ELSE CONCAT('V156_IM_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name LIKE 'im\_%';

-- 私聊消息表唯一键（sender_id, client_message_id, tenant_id）存在
SELECT CASE WHEN COUNT(*) = 3 THEN 'V156_IM_PM_UK PASS'
            ELSE CONCAT('V156_IM_PM_UK FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'im_private_message'
   AND index_name = 'uk_im_private_message_sender_client';

-- 群成员表唯一键（group_id, user_id, tenant_id）存在
SELECT CASE WHEN COUNT(*) = 3 THEN 'V156_IM_GM_UK PASS'
            ELSE CONCAT('V156_IM_GM_UK FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'im_group_member'
   AND index_name = 'uk_im_group_member';

-- 全部 17 张表都有 tenant_id 列（IM DO 均继承 TenantBaseDO）
SELECT CASE WHEN COUNT(*) = 17 THEN 'V156_IM_TENANT_COLS PASS'
            ELSE CONCAT('V156_IM_TENANT_COLS FAIL ', COUNT(*)) END AS check_result
  FROM (SELECT table_name FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name LIKE 'im\_%' AND column_name = 'tenant_id'
        GROUP BY table_name) t;
