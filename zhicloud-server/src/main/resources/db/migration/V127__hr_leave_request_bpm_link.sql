-- ======================== HR 请假单接入 BPM ========================
-- 作者：zhicloud
-- 说明：请假单此前只有 approver_id / approve_time，审批结果只能由 HR 自己的接口手写，
--       流程引擎里查不到这张单子对应的流程实例（V126 的 v_bpm_approval_inflight 里 HR 那一列就是 NULL）。
--       补 process_instance_id 后，请假审批才能和报销/合同一样「提交—审批—回写」闭环。
--       允许为 NULL：历史单据与不走 BPM 的手工审批都保持原样。

ALTER TABLE hr_leave_request
    ADD COLUMN process_instance_id VARCHAR(64) NULL DEFAULT NULL COMMENT 'BPM 流程实例编号（NULL 表示未经流程引擎审批）';
