-- ============================================================
-- V131：HR 假期类型基础数据（租户 1）
-- ============================================================
-- 真机跑请假审批链时发现 `hr_leave_type` 是空表：没有假期类型就建不出请假单，
-- 也就是「员工请假」这条业务流在初始化后的系统里根本走不起来（员工/余额属于组织数据，
-- 必须由各企业自己录入，所以本脚本只补「配置型」参照数据，不伪造员工）。
-- 幂等：按 (tenant_id, code) 预检，重复执行不会插第二份；deleted = 0 才视作已存在。
-- ============================================================

INSERT INTO `hr_leave_type` (`name`, `code`, `is_paid`, `deduct_salary`, `remark`,
                             `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.* FROM (
    SELECT '年假' AS `name`, 'ANNUAL' AS `code`, 1 AS `is_paid`, 0 AS `deduct_salary`,
           '法定带薪年休假' AS `remark`, '1' AS `creator`, NOW() AS `create_time`, '1' AS `updater`,
           NOW() AS `update_time`, b'0' AS `deleted`, 1 AS `tenant_id`
    UNION ALL SELECT '病假', 'SICK', 1, 0, '凭医院证明的带薪病假', '1', NOW(), '1', NOW(), b'0', 1
    UNION ALL SELECT '事假', 'PERSONAL', 0, 1, '无薪事假，按天扣薪', '1', NOW(), '1', NOW(), b'0', 1
    UNION ALL SELECT '调休', 'COMPENSATORY', 1, 0, '加班转调休', '1', NOW(), '1', NOW(), b'0', 1
    UNION ALL SELECT '婚假', 'MARRIAGE', 1, 0, '法定婚假', '1', NOW(), '1', NOW(), b'0', 1
    UNION ALL SELECT '产假', 'MATERNITY', 1, 0, '法定产假', '1', NOW(), '1', NOW(), b'0', 1
    UNION ALL SELECT '陪产假', 'PATERNITY', 1, 0, '法定陪产假', '1', NOW(), '1', NOW(), b'0', 1
    UNION ALL SELECT '丧假', 'BEREAVEMENT', 1, 0, '直系亲属丧假', '1', NOW(), '1', NOW(), b'0', 1
) t
WHERE NOT EXISTS (
    SELECT 1 FROM `hr_leave_type` e
     WHERE e.`tenant_id` = t.`tenant_id` AND e.`code` = t.`code` AND e.`deleted` = b'0'
);

-- 自检：8 个常用类型都要在，且至少有一个是「不带薪」（事假），否则余额扣减分支永远走不到
SELECT CASE WHEN COUNT(*) = 8 THEN 'V131_LEAVE_TYPE_COUNT PASS'
            ELSE CONCAT('V131_LEAVE_TYPE_COUNT FAIL ', COUNT(*)) END AS check_result
  FROM `hr_leave_type`
 WHERE `tenant_id` = 1 AND `deleted` = b'0'
   AND `code` IN ('ANNUAL','SICK','PERSONAL','COMPENSATORY','MARRIAGE','MATERNITY','PATERNITY','BEREAVEMENT');

SELECT CASE WHEN COUNT(*) = 1 THEN 'V131_UNPAID_TYPE_EXISTS PASS'
            ELSE CONCAT('V131_UNPAID_TYPE_EXISTS FAIL ', COUNT(*)) END AS check_result
  FROM `hr_leave_type`
 WHERE `tenant_id` = 1 AND `deleted` = b'0' AND `code` = 'PERSONAL' AND `is_paid` = 0 AND `deduct_salary` = 1;
