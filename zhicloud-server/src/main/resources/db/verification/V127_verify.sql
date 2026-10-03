-- ============================================================
-- V127 人工复核脚本（只读）
--
-- 门禁：hr_leave_request 必须有可空的 process_instance_id，
--       否则请假审批无法与 BPM 流程实例对上（V126 视图里 HR 那一列恒为 NULL）。
-- ============================================================

SELECT 'V127_HR_LEAVE_PROCESS_INSTANCE_ID' AS check_name,
       CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS result,
       CASE WHEN COUNT(*) = 1 THEN 'present' ELSE 'missing' END AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'hr_leave_request'
   AND column_name = 'process_instance_id'
   AND LOWER(data_type) IN ('varchar', 'char')
   AND is_nullable = 'YES';
