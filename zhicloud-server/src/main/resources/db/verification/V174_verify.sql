-- V174 验证：HRM 绩效域 8 张表落库
SELECT CASE WHEN COUNT(*) = 8 THEN 'V174_PERF_TABLES PASS'
            ELSE CONCAT('V174_PERF_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`tables`
 WHERE `table_schema` = DATABASE()
   AND `table_name` IN ('hr_performance_assessment_template','hr_performance_result_template',
                        'hr_performance_plan','hr_performance_assessment',
                        'hr_performance_assessment_dimension','hr_performance_assessment_quota',
                        'hr_performance_assessment_stage','hr_performance_process_record');

-- 计划配置 json 列（考核/结果配置快照 + 三类阶段节点，共 7）
SELECT CASE WHEN COUNT(*) = 7 THEN 'V174_PLAN_JSON_COLS PASS'
            ELSE CONCAT('V174_PLAN_JSON_COLS FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`columns`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_performance_plan'
   AND `column_name` IN ('scopes','assessment_config','result_config','target_confirmation_stage',
                         'review_stages','result_audit_stages','appeal_stages');

-- 考核指标表 assessment_id / dimension_id 索引
SELECT CASE WHEN COUNT(*) = 2 THEN 'V174_QUOTA_IDX PASS'
            ELSE CONCAT('V174_QUOTA_IDX FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`statistics`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_performance_assessment_quota'
   AND `index_name` IN ('idx_hr_perf_quota_assessment','idx_hr_perf_quota_dimension')
   AND `seq_in_index` = 1;

-- 流程记录 (biz_type,biz_id) 复合索引
SELECT CASE WHEN COUNT(*) = 2 THEN 'V174_RECORD_IDX PASS'
            ELSE CONCAT('V174_RECORD_IDX FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`statistics`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_performance_process_record'
   AND `index_name` = 'idx_hr_perf_record_biz';
