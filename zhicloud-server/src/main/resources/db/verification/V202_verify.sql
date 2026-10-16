-- V202 验证：QMS 状态字典种子（§12.236.10）

-- 1. 19 个新增字典类型全部存在
SELECT CASE WHEN COUNT(DISTINCT type) = 19 THEN 'V202_QMS_DICT_TYPES_PASS'
            ELSE CONCAT('V202_QMS_DICT_TYPES_FAIL ', COUNT(DISTINCT type)) END AS check_result
  FROM system_dict_type
 WHERE deleted = b'0'
   AND type IN ('qms_doc_status','qms_ncr_status','qms_ncr_disposition','qms_capa_status',
                'qms_complaint_status','qms_eight_d_status','qms_fmea_status',
                'qms_inspection_order_status','qms_instrument_status','qms_msa_status',
                'qms_scar_status','qms_supplier_audit_status','qms_audit_plan_status',
                'qms_training_plan_status','qms_qualification_status',
                'qms_training_record_status','qms_supplier_grade',
                 'qms_nc_status','qms_change_request_status');

-- 2. 79 条字典数据全部存在（V5 种的 qms_capa_% 不计入本迁移产物）
SELECT CASE WHEN COUNT(*) = 79 THEN 'V202_QMS_DICT_DATA_PASS'
            ELSE CONCAT('V202_QMS_DICT_DATA_FAIL ', COUNT(*)) END AS check_result
  FROM system_dict_data
 WHERE deleted = b'0'
   AND dict_type IN ('qms_doc_status','qms_ncr_status','qms_ncr_disposition','qms_capa_status',
                     'qms_complaint_status','qms_eight_d_status','qms_fmea_status',
                     'qms_inspection_order_status','qms_instrument_status','qms_msa_status',
                     'qms_scar_status','qms_supplier_audit_status','qms_audit_plan_status',
                     'qms_training_plan_status','qms_qualification_status',
                     'qms_training_record_status','qms_supplier_grade',
                 'qms_nc_status','qms_change_request_status');

-- 3. 关键状态值逐一对齐后端枚举（抽查四个最核心的域）
--    期望 16 = qms_doc_status 5 (10..50)
--            + qms_inspection_order_status 4 (10..40)
--            + qms_ncr_status 4 (10..40)
--            + qms_capa_status 3 (10..30)
SELECT CASE WHEN COUNT(*) = 16 THEN 'V202_QMS_DICT_VALUES_PASS'
            ELSE CONCAT('V202_QMS_DICT_VALUES_FAIL ', COUNT(*)) END AS check_result
  FROM system_dict_data
 WHERE deleted = b'0'
   AND (
        (dict_type = 'qms_doc_status'              AND value IN ('10','20','30','40','50'))
     OR (dict_type = 'qms_inspection_order_status' AND value IN ('10','20','30','40'))
     OR (dict_type = 'qms_ncr_status'              AND value IN ('10','20','30','40'))
     OR (dict_type = 'qms_capa_status'             AND value IN ('10','20','30'))
   );

-- 4. 唯一键无重复 (dict_type,value)（幂等性反证：重跑不会产生重复行）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V202_QMS_DICT_NO_DUP_PASS'
            ELSE CONCAT('V202_QMS_DICT_NO_DUP_FAIL ', COUNT(*)) END AS check_result
  FROM (
    SELECT dict_type, value FROM system_dict_data
     WHERE deleted = b'0' AND dict_type LIKE 'qms_%'
     GROUP BY dict_type, value HAVING COUNT(*) > 1
  ) dup;
