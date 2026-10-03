-- V131 验证：HR 假期类型参照数据可用
SELECT CASE WHEN COUNT(*) = 8 THEN 'V131_LEAVE_TYPE_COUNT PASS'
            ELSE CONCAT('V131_LEAVE_TYPE_COUNT FAIL ', COUNT(*)) END AS check_result
  FROM hr_leave_type
 WHERE tenant_id = 1 AND deleted = b'0'
   AND code IN ('ANNUAL','SICK','PERSONAL','COMPENSATORY','MARRIAGE','MATERNITY','PATERNITY','BEREAVEMENT');

SELECT CASE WHEN COUNT(*) = 0 THEN 'V131_NO_DUPLICATE_CODE PASS'
            ELSE CONCAT('V131_NO_DUPLICATE_CODE FAIL ', COUNT(*)) END AS check_result
  FROM (SELECT code FROM hr_leave_type WHERE tenant_id = 1 AND deleted = b'0'
         GROUP BY code HAVING COUNT(*) > 1) d;

SELECT CASE WHEN COUNT(*) >= 1 THEN 'V131_PAID_TYPE_AVAILABLE PASS'
            ELSE 'V131_PAID_TYPE_AVAILABLE FAIL' END AS check_result
  FROM hr_leave_type
 WHERE tenant_id = 1 AND deleted = b'0' AND is_paid = 1;
