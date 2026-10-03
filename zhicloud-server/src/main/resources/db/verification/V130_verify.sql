-- ============================================================
-- V130 验证脚本：v_bpm_approval_inflight 的「在途」口径
-- 每条检查输出一行 PASS / FAIL，便于人工与脚本共同读数。
-- 与 V130 的自检不同：这里做行为断言（视图返回的每一行都必须满足在途条件），
-- 而不是只看定义文本，防止有人日后用等价但语义漂移的定义糊过去。
-- ============================================================

SELECT CASE WHEN COUNT(*) = 9 THEN 'V130_COLUMNS_9 PASS'
            ELSE CONCAT('V130_COLUMNS_9 FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'v_bpm_approval_inflight';

SELECT CASE WHEN COUNT(*) = 0 THEN 'V130_OA_ONLY_APPROVING PASS'
            ELSE CONCAT('V130_OA_ONLY_APPROVING FAIL ', COUNT(*)) END AS check_result
  FROM v_bpm_approval_inflight v
  JOIN oa_reimburse r ON r.id = v.biz_id
 WHERE v.biz_type = 'oa_reimburse'
   AND (r.deleted <> 0 OR r.status <> 20);

SELECT CASE WHEN COUNT(*) = 0 THEN 'V130_CRM_ONLY_PROCESSING PASS'
            ELSE CONCAT('V130_CRM_ONLY_PROCESSING FAIL ', COUNT(*)) END AS check_result
  FROM v_bpm_approval_inflight v
  JOIN crm_contract c ON c.id = v.biz_id
 WHERE v.biz_type = 'crm_contract'
   AND (c.deleted <> 0 OR c.audit_status <> 10);

SELECT CASE WHEN COUNT(*) = 0 THEN 'V130_HR_ONLY_PENDING PASS'
            ELSE CONCAT('V130_HR_ONLY_PENDING FAIL ', COUNT(*)) END AS check_result
  FROM v_bpm_approval_inflight v
  JOIN hr_leave_request h ON h.id = v.biz_id
 WHERE v.biz_type = 'hr_leave_request'
   AND (h.deleted <> 0 OR h.status <> 0);

-- hr 分支的流程实例编号必须透传，否则审批看板无法跳转 BPM 流程图
SELECT CASE WHEN COUNT(*) = 0 THEN 'V130_HR_PROCESS_INSTANCE_PASSED_THROUGH PASS'
            ELSE CONCAT('V130_HR_PROCESS_INSTANCE_PASSED_THROUGH FAIL ', COUNT(*)) END AS check_result
  FROM hr_leave_request h
  JOIN v_bpm_approval_inflight v ON v.biz_type = 'hr_leave_request' AND v.biz_id = h.id
 WHERE h.deleted = 0
   AND h.status = 0
   AND NOT (v.process_instance_id <=> h.process_instance_id);

-- 已完成/已驳回的审批不得出现在在途视图里（真机跑过一次审批后才写得出的断言）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V130_CLOSED_APPROVALS_EXCLUDED PASS'
            ELSE CONCAT('V130_CLOSED_APPROVALS_EXCLUDED FAIL ', COUNT(*)) END AS check_result
  FROM v_bpm_approval_inflight v
 WHERE v.biz_type = 'oa_reimburse' AND v.biz_status <> 20
    OR v.biz_type = 'crm_contract' AND v.biz_status <> 10
    OR v.biz_type = 'hr_leave_request' AND v.biz_status <> 0;
