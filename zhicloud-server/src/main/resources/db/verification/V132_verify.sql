-- V132 验证：会员等级与分组参照数据可用
SELECT CASE WHEN COUNT(*) = 5 THEN 'V132_LEVEL_COUNT PASS'
            ELSE CONCAT('V132_LEVEL_COUNT FAIL ', COUNT(*)) END AS check_result
  FROM member_level
 WHERE tenant_id = 1 AND deleted = b'0'
   AND level IN (1,2,3,4,5);

SELECT CASE WHEN COUNT(*) = 0 THEN 'V132_NO_DUPLICATE_LEVEL PASS'
            ELSE CONCAT('V132_NO_DUPLICATE_LEVEL FAIL ', COUNT(*)) END AS check_result
  FROM (SELECT level FROM member_level WHERE tenant_id = 1 AND deleted = b'0'
         GROUP BY level HAVING COUNT(*) > 1) d;

-- 等级阶梯必须单调：经验阈值递增、折扣率递减，否则"升级到更高等级反而更贵"
SELECT CASE WHEN COUNT(*) = 0 THEN 'V132_EXPERIENCE_MONOTONIC PASS'
            ELSE CONCAT('V132_EXPERIENCE_MONOTONIC FAIL ', COUNT(*)) END AS check_result
  FROM member_level a
  JOIN member_level b
    ON b.tenant_id = a.tenant_id AND b.deleted = b'0' AND a.deleted = b'0'
   AND b.level = a.level + 1
 WHERE b.experience <= a.experience OR b.discount_percent >= a.discount_percent;

SELECT CASE WHEN COUNT(*) >= 3 THEN 'V132_GROUP_AVAILABLE PASS'
            ELSE CONCAT('V132_GROUP_AVAILABLE FAIL ', COUNT(*)) END AS check_result
  FROM member_group
 WHERE tenant_id = 1 AND deleted = b'0';

-- 会员的 level_id 必须指向真实等级（历史会员可能是 0，种子脚本已按经验值对齐）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V132_MEMBER_LEVEL_REFERENCE PASS'
            ELSE CONCAT('V132_MEMBER_LEVEL_REFERENCE FAIL ', COUNT(*)) END AS check_result
  FROM member_user u
  LEFT JOIN member_level l
    ON l.tenant_id = u.tenant_id AND l.deleted = b'0' AND l.id = u.level_id
 WHERE u.deleted = b'0' AND l.id IS NULL;
