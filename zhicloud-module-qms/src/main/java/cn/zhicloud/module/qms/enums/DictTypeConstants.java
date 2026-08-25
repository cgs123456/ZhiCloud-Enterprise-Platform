package cn.zhicloud.module.qms.enums;

/**
 * QMS 字典类型的枚举类
 *
 * @author 智云
 */
public interface DictTypeConstants {

    String INSPECTION_TYPE = "qms_inspection_type"; // 检验类型
    String INSPECTION_METHOD = "qms_inspection_method"; // 检验方法
    String INSPECTION_RESULT = "qms_inspection_result"; // 检验结果
    String INSPECTION_ORDER_STATUS = "qms_inspection_order_status"; // 检验单状态
    String CAPA_SOURCE = "qms_capa_source"; // CAPA 来源
    String CAPA_STATUS = "qms_capa_status"; // CAPA 状态
    String CAPA_PRIORITY = "qms_capa_priority"; // CAPA 优先级
    String CAPA_STAGE = "qms_capa_stage"; // CAPA 阶段
    String CAPA_VERIFICATION_RESULT = "qms_capa_verification_result"; // CAPA 验证结果

    // NCR 不合格品处理
    String NCR_SOURCE = "qms_ncr_source"; // NCR 来源
    String NCR_DEFECT_LEVEL = "qms_ncr_defect_level"; // NCR 缺陷等级
    String NCR_DISPOSITION = "qms_ncr_disposition"; // NCR 处置方式
    String NCR_STATUS = "qms_ncr_status"; // NCR 状态
    String NCR_MRB_DECISION = "qms_ncr_mrb_decision"; // MRB 决议

    // FMEA 失效模式分析
    String FMEA_TYPE = "qms_fmea_type"; // FMEA 类型
    String FMEA_STATUS = "qms_fmea_status"; // FMEA 状态
    String FMEA_RISK_LEVEL = "qms_fmea_risk_level"; // FMEA 风险等级

    // MSA 测量系统分析
    String MSA_STUDY_TYPE = "qms_msa_study_type"; // MSA 研究类型
    String MSA_STATUS = "qms_msa_status"; // MSA 状态

    // 计量器具管理
    String INSTRUMENT_CATEGORY = "qms_instrument_category"; // 计量器具类别
    String INSTRUMENT_STATUS = "qms_instrument_status"; // 计量器具状态
    String CALIBRATION_RESULT = "qms_calibration_result"; // 校准结果

    // 受控文档管理
    String DOC_TYPE = "qms_doc_type"; // 受控文档类型
    String DOC_STATUS = "qms_doc_status"; // 受控文档状态
    String DOC_CHANGE_TYPE = "qms_doc_change_type"; // 文件变更类型
    String CHANGE_REQUEST_STATUS = "qms_change_request_status"; // 变更申请状态

    // 审核管理
    String AUDIT_TYPE = "qms_audit_type"; // 审核类型
    String AUDIT_PLAN_STATUS = "qms_audit_plan_status"; // 审核计划状态
    String AUDITOR_ROLE = "qms_auditor_role"; // 审核员角色
    String AUDIT_CONCLUSION = "qms_audit_conclusion"; // 审核结论
    String NC_SEVERITY = "qms_nc_severity"; // 不符合项严重程度
    String NC_STATUS = "qms_nc_status"; // 不符合项状态

    // 8D 报告
    String EIGHT_D_STATUS = "qms_eight_d_status"; // 8D 状态

    // 培训管理
    String TRAINING_PLAN_STATUS = "qms_training_plan_status"; // 培训计划状态
    String TRAINING_RECORD_STATUS = "qms_training_record_status"; // 培训记录状态
    String QUALIFICATION_STATUS = "qms_qualification_status"; // 资格状态

    // 客户投诉
    String COMPLAINT_STATUS = "qms_complaint_status"; // 投诉状态
    String COMPLAINT_HANDLE_TYPE = "qms_complaint_handle_type"; // 投诉处理方式

    // 供应商质量管理
    String SUPPLIER_GRADE = "qms_supplier_grade"; // 供应商等级
    String SCAR_STATUS = "qms_scar_status"; // SCAR 状态
    String SUPPLIER_AUDIT_STATUS = "qms_supplier_audit_status"; // 供应商审核状态

    // 质量成本 PAIF
    String QUALITY_COST_TYPE = "qms_quality_cost_type"; // 质量成本类型
    String QUALITY_COST_RELATED_TYPE = "qms_quality_cost_related_type"; // 质量成本关联业务类型
}