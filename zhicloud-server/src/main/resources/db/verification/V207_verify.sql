-- V207 验证：MES MRP「转采购/生产建议」字典增量（§12.262）

-- 1. 两行字典数据全部存在（mrp_plan_status=3 / work_order_source_type=3）
SELECT CASE WHEN COUNT(*) = 2 THEN 'V207_MRP_CONVERT_DICT_DATA_PASS'
            ELSE CONCAT('V207_MRP_CONVERT_DICT_DATA_FAIL ', COUNT(*)) END AS check_result
  FROM system_dict_data
 WHERE deleted = b'0'
   AND (   (dict_type = 'mes_pro_mrp_plan_status'        AND value = '3')
        OR (dict_type = 'mes_pro_work_order_source_type' AND value = '3'));

-- 2. 两类型内 (dict_type,value) 无重复（幂等反证：重跑不产生重复行）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V207_MRP_CONVERT_DICT_NO_DUP_PASS'
            ELSE CONCAT('V207_MRP_CONVERT_DICT_NO_DUP_FAIL ', COUNT(*)) END AS check_result
  FROM (
    SELECT dict_type, value FROM system_dict_data
     WHERE deleted = b'0'
       AND dict_type IN ('mes_pro_mrp_plan_status','mes_pro_work_order_source_type')
     GROUP BY dict_type, value HAVING COUNT(*) > 1
  ) dup;

-- 3. label 逐一对齐后端枚举（已转建议 / MRP 计划）
SELECT CASE WHEN COUNT(*) = 2 THEN 'V207_MRP_CONVERT_DICT_LABEL_PASS'
            ELSE CONCAT('V207_MRP_CONVERT_DICT_LABEL_FAIL ', COUNT(*)) END AS check_result
  FROM system_dict_data
 WHERE deleted = b'0'
   AND (   (dict_type = 'mes_pro_mrp_plan_status'        AND value = '3' AND label = '已转建议')
        OR (dict_type = 'mes_pro_work_order_source_type' AND value = '3' AND label = 'MRP 计划'));
