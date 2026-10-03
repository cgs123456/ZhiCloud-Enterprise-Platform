-- ============================================================
-- V130：修正 v_bpm_approval_inflight 的「在途」口径
-- ============================================================
-- V126 建视图时三个分支都只写了 deleted = 0，导致「已通过 / 已驳回」的历史审批也出现在
-- 「在途审批」视图里（真机验证：审批完成的报销单 status=30 仍然被视图返回）。
-- 同时 hr_leave_request 分支把 process_instance_id 写成常量 NULL，前端拿不到流程实例，
-- 无法从审批看板跳到 BPM 流程图。
--
-- 本脚本用 CREATE OR REPLACE VIEW 重建，口径改为业务侧「审批中」状态：
--   oa_reimburse.status = 20（审批中）
--   crm_contract.audit_status = 10（CrmAuditStatusEnum.PROCESS 审批中）
--   hr_leave_request.status = 0（HrLeaveStatusEnum.PENDING 待审批）
-- 列顺序与类型与 V126 保持一致，依赖方无需改查询。
-- ============================================================

CREATE OR REPLACE VIEW v_bpm_approval_inflight AS
SELECT CAST('oa_reimburse' AS CHAR) COLLATE utf8mb4_unicode_ci     AS biz_type,
       r.id                                                         AS biz_id,
       CAST(r.no AS CHAR) COLLATE utf8mb4_unicode_ci                AS biz_no,
       r.status                                                     AS biz_status,
       CAST(r.process_instance_id AS CHAR) COLLATE utf8mb4_unicode_ci AS process_instance_id,
       r.applicant_user_id                                          AS owner_user_id,
       r.dept_id                                                    AS dept_id,
       r.total_amount                                               AS amount,
       r.update_time                                                AS update_time
  FROM oa_reimburse r
 WHERE r.deleted = 0
   AND r.status = 20
UNION ALL
SELECT CAST('crm_contract' AS CHAR) COLLATE utf8mb4_unicode_ci,
       c.id,
       CAST(c.no AS CHAR) COLLATE utf8mb4_unicode_ci,
       c.audit_status,
       CAST(c.process_instance_id AS CHAR) COLLATE utf8mb4_unicode_ci,
       c.owner_user_id,
       NULL,
       c.total_price,
       c.update_time
  FROM crm_contract c
 WHERE c.deleted = 0
   AND c.audit_status = 10
UNION ALL
SELECT CAST('hr_leave_request' AS CHAR) COLLATE utf8mb4_unicode_ci,
       h.id,
       CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci,
       h.status,
       CAST(h.process_instance_id AS CHAR) COLLATE utf8mb4_unicode_ci,
       NULL,
       NULL,
       NULL,
       h.update_time
  FROM hr_leave_request h
 WHERE h.deleted = 0
   AND h.status = 0;

-- 自检：视图定义必须带上三个「审批中」谓词，且 hr 分支不能再回常量 NULL 的流程实例
-- MySQL 存 information_schema.views.view_definition 时会补全反引号限定名，
-- 所以匹配串要按 `alias`.`column` = value 的形态写，不能按手写 SQL 的裸列名写。
SELECT CASE WHEN (SELECT COUNT(*) FROM information_schema.views
                   WHERE table_schema = DATABASE()
                     AND table_name = 'v_bpm_approval_inflight'
                     AND view_definition LIKE '%`status` = 20%'
                     AND view_definition LIKE '%`audit_status` = 10%'
                     AND view_definition LIKE '%`status` = 0%'
                     AND view_definition LIKE '%`h`.`process_instance_id`%') = 1
            THEN 'V130 self-check passed'
            ELSE 'V130 self-check FAILED' END AS v130_view_predicate_check;
