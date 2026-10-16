-- V202: QMS 状态/处置/评级字典种子（§12.236.10）
-- ==========================================================
-- 说明：QMS 各域的流转状态此前**没有任何 Flyway 字典种子**（库里只有 V5 种的
--       qms_capa_% 三件），前端 15 个表单的「状态」输入框与 34 个列表状态列
--       因此一直显示裸整数。本轮服务端已把状态改为「只能由流转方法写入」
--       （§12.236），表单状态字段转为只读回显，列表改用 dict-tag 渲染，
--       本迁移补齐它们所需的字典。
-- 覆盖：19 个类型 + 79 项数据。除各域流转状态/处置/评级外，另含审核不符合项
--       （qms_nc_status）与文件变更申请（qms_change_request_status）两套——它们此前
--       只存在于未部署的 sql/mysql/qms_document_audit.sql，而前端两个列表页的
--       dict-tag 依赖它们；取值分别对齐 QmsNcStatusEnum / QmsChangeRequestStatusEnum。
-- 取值：**逐条从后端枚举源码解析生成**（脚本 Temp/zc_gen_v202_dict_sql.py），
--       不手抄，避免注释/字典与实现脱节（§12.234.6 同类教训）。
-- 幂等：system_dict_type 按 (type)、system_dict_data 按 (dict_type,value) 预检，
--       只认 deleted = b'0'。qms_capa_stage 已由 V5 种过，预检自动跳过。
-- id 号段：type=1063001+ / data=1063101+（库内现最大 type=1062075、data=1062236）。

-- 一、字典类型
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT t.* FROM (
  SELECT 1063001 AS `id`, 'QMS 受控文档状态' AS `name`, 'qms_doc_status' AS `type`, 0 AS `status`, 'QmsDocStatusEnum' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, '1970-01-01 00:00:00' AS `deleted_time`
  UNION ALL SELECT 1063002, 'QMS 不合格品报告状态', 'qms_ncr_status', 0, 'NcrStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063003, 'QMS 不合格品处置方式', 'qms_ncr_disposition', 0, 'NcrDispositionEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063004, 'QMS CAPA 状态', 'qms_capa_status', 0, 'CAPAStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063005, 'QMS 客户投诉状态', 'qms_complaint_status', 0, 'ComplaintStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063006, 'QMS 8D 报告状态', 'qms_eight_d_status', 0, 'EightDStatusEnum（注意草稿=0）', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063007, 'QMS FMEA 状态', 'qms_fmea_status', 0, 'FmeaStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063008, 'QMS 检验单状态', 'qms_inspection_order_status', 0, 'InspectionOrderStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063009, 'QMS 计量器具状态', 'qms_instrument_status', 0, 'QmsInstrumentStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063010, 'QMS MSA 分析状态', 'qms_msa_status', 0, 'MsaStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063011, 'QMS SCAR 状态', 'qms_scar_status', 0, 'ScarStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063012, 'QMS 供应商审核状态', 'qms_supplier_audit_status', 0, 'SupplierAuditStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063013, 'QMS 审核计划状态', 'qms_audit_plan_status', 0, 'QmsAuditPlanStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063014, 'QMS 培训计划状态', 'qms_training_plan_status', 0, 'TrainingPlanStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063015, 'QMS 资质状态', 'qms_qualification_status', 0, 'QualificationStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063016, 'QMS 培训记录状态', 'qms_training_record_status', 0, '该域无枚举，按 TrainingRecordDO#status 注释：10 已登记 20 已完成', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063017, 'QMS 供应商评级', 'qms_supplier_grade', 0, 'SupplierGradeEnum（value 为字符串 A-D，由 calculateGrade 按指标算出）', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063018, 'QMS 不符合项状态', 'qms_nc_status', 0, 'QmsNcStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1063019, 'QMS 文件变更申请状态', 'qms_change_request_status', 0, 'QmsChangeRequestStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
) t
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` d WHERE d.`deleted` = b'0' AND d.`type` = t.`type`);

-- 二、字典数据
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  SELECT 1063101 AS `id`, 1 AS `sort`, '草稿' AS `label`, '10' AS `value`, 'qms_doc_status' AS `dict_type`, 0 AS `status`, 'info' AS `color_type`, '' AS `css_class`, '' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 1063102, 2, '待审', '20', 'qms_doc_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063103, 3, '已发布', '30', 'qms_doc_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063104, 4, '已作废', '40', 'qms_doc_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063105, 5, '已回收', '50', 'qms_doc_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063106, 1, '待处理', '10', 'qms_ncr_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063107, 2, 'MRB 评审中', '20', 'qms_ncr_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063108, 3, '已处置', '30', 'qms_ncr_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063109, 4, '已关闭', '40', 'qms_ncr_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063110, 1, '返工', '10', 'qms_ncr_disposition', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063111, 2, '返修', '20', 'qms_ncr_disposition', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063112, 3, '降级', '30', 'qms_ncr_disposition', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063113, 4, '报废', '40', 'qms_ncr_disposition', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063114, 5, '让步接收', '50', 'qms_ncr_disposition', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063115, 6, '退货', '60', 'qms_ncr_disposition', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063116, 1, '待处理', '10', 'qms_capa_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063117, 2, '处理中', '20', 'qms_capa_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063118, 3, '已关闭', '30', 'qms_capa_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063119, 1, '已登记', '10', 'qms_complaint_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063120, 2, '调查中', '20', 'qms_complaint_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063121, 3, '处理中', '30', 'qms_complaint_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063122, 4, '已关闭', '40', 'qms_complaint_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063123, 1, '草稿', '0', 'qms_eight_d_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063124, 2, 'D1 成立团队', '10', 'qms_eight_d_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063125, 3, 'D2 描述问题', '20', 'qms_eight_d_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063126, 4, 'D3 临时遏制措施', '30', 'qms_eight_d_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063127, 5, 'D4 根本原因分析', '40', 'qms_eight_d_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063128, 6, 'D5 制定永久纠正措施', '50', 'qms_eight_d_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063129, 7, 'D6 实施并验证', '60', 'qms_eight_d_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063130, 8, 'D7 预防再发生', '70', 'qms_eight_d_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063131, 9, 'D8 团队表彰/关闭', '80', 'qms_eight_d_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063132, 1, '草稿', '10', 'qms_fmea_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063133, 2, '已评审', '20', 'qms_fmea_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063134, 3, '已批准', '30', 'qms_fmea_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063135, 1, '待检验', '10', 'qms_inspection_order_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063136, 2, '检验中', '20', 'qms_inspection_order_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063137, 3, '检验通过', '30', 'qms_inspection_order_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063138, 4, '检验不通过', '40', 'qms_inspection_order_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063139, 1, '在用', '10', 'qms_instrument_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063140, 2, '停用', '20', 'qms_instrument_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063141, 3, '报废', '30', 'qms_instrument_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063142, 4, '封存', '40', 'qms_instrument_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063143, 1, '草稿', '10', 'qms_msa_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063144, 2, '进行中', '20', 'qms_msa_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063145, 3, '已完成', '30', 'qms_msa_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063146, 1, '已发起', '10', 'qms_scar_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063147, 2, '处理中', '20', 'qms_scar_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063148, 3, '已关闭', '30', 'qms_scar_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063149, 1, '已计划', '10', 'qms_supplier_audit_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063150, 2, '审核中', '20', 'qms_supplier_audit_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063151, 3, '已完成', '30', 'qms_supplier_audit_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063152, 4, '已取消', '40', 'qms_supplier_audit_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063153, 1, '已计划', '10', 'qms_audit_plan_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063154, 2, '已执行', '20', 'qms_audit_plan_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063155, 3, '已完成', '30', 'qms_audit_plan_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063156, 4, '已取消', '40', 'qms_audit_plan_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063157, 1, '草稿', '10', 'qms_training_plan_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063158, 2, '已安排', '20', 'qms_training_plan_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063159, 3, '进行中', '30', 'qms_training_plan_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063160, 4, '已完成', '40', 'qms_training_plan_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063161, 5, '已取消', '50', 'qms_training_plan_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063162, 1, '有效', '10', 'qms_qualification_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063163, 2, '即将到期', '20', 'qms_qualification_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063164, 3, '已到期', '30', 'qms_qualification_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063165, 4, '已撤销', '40', 'qms_qualification_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063166, 1, '已登记', '10', 'qms_training_record_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063167, 2, '已完成', '20', 'qms_training_record_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063168, 1, '优秀', 'A', 'qms_supplier_grade', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063169, 2, '合格', 'B', 'qms_supplier_grade', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063170, 3, '待改进', 'C', 'qms_supplier_grade', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063171, 4, '不合格', 'D', 'qms_supplier_grade', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063172, 1, '待整改', '10', 'qms_nc_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063173, 2, '整改中', '20', 'qms_nc_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063174, 3, '已整改', '30', 'qms_nc_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063175, 4, '已验证', '40', 'qms_nc_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063176, 5, '已关闭', '50', 'qms_nc_status', 0, '', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063177, 1, '待审', '10', 'qms_change_request_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063178, 2, '已审', '20', 'qms_change_request_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1063179, 3, '已驳回', '30', 'qms_change_request_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` d WHERE d.`deleted` = b'0' AND d.`dict_type` = t.`dict_type` AND d.`value` = t.`value`);
