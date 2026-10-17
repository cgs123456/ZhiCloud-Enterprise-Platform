-- V204 验证：MES APS/MRP 状态与优先级字典种子（§12.246）

-- 1. 3 个新增字典类型全部存在
SELECT CASE WHEN COUNT(DISTINCT type) = 3 THEN 'V204_MES_DICT_TYPES_PASS'
            ELSE CONCAT('V204_MES_DICT_TYPES_FAIL ', COUNT(DISTINCT type)) END AS check_result
  FROM system_dict_type
 WHERE deleted = b'0'
   AND type IN ('mes_pro_aps_plan_status','mes_pro_aps_plan_priority','mes_pro_mrp_plan_status');

-- 2. 11 条字典数据全部存在
SELECT CASE WHEN COUNT(*) = 11 THEN 'V204_MES_DICT_DATA_PASS'
            ELSE CONCAT('V204_MES_DICT_DATA_FAIL ', COUNT(*)) END AS check_result
  FROM system_dict_data
 WHERE deleted = b'0'
   AND dict_type IN ('mes_pro_aps_plan_status','mes_pro_aps_plan_priority','mes_pro_mrp_plan_status');

-- 3. 取值逐一对齐后端枚举
--    期望 11 = aps 状态 5 (0..4) + aps 优先级 3 (1..3) + mrp 状态 3 (0..2)
SELECT CASE WHEN COUNT(*) = 11 THEN 'V204_MES_DICT_VALUES_PASS'
            ELSE CONCAT('V204_MES_DICT_VALUES_FAIL ', COUNT(*)) END AS check_result
  FROM system_dict_data
 WHERE deleted = b'0'
   AND (
         (dict_type = 'mes_pro_aps_plan_status'   AND value IN ('0','1','2','3','4'))
      OR (dict_type = 'mes_pro_aps_plan_priority' AND value IN ('1','2','3'))
      OR (dict_type = 'mes_pro_mrp_plan_status'   AND value IN ('0','1','2'))
   );

-- 4. 三类内 (dict_type,value) 无重复（幂等反证：重跑不产生重复行）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V204_MES_DICT_NO_DUP_PASS'
            ELSE CONCAT('V204_MES_DICT_NO_DUP_FAIL ', COUNT(*)) END AS check_result
  FROM (
    SELECT dict_type, value FROM system_dict_data
     WHERE deleted = b'0'
       AND dict_type IN ('mes_pro_aps_plan_status','mes_pro_aps_plan_priority','mes_pro_mrp_plan_status')
     GROUP BY dict_type, value HAVING COUNT(*) > 1
  ) dup;
