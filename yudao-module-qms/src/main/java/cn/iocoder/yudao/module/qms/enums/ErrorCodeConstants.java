package cn.iocoder.yudao.module.qms.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * QMS 错误码枚举类
 * <p>
 * qms 系统，使用 1-040-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== QMS 检验项目（1-040-100-000） ==========
    ErrorCode INSPECTION_ITEM_NOT_EXISTS = new ErrorCode(1_044_000_009, "检验项目不存在");
    ErrorCode INSPECTION_ITEM_CODE_DUPLICATE = new ErrorCode(1_044_000_010, "检验项目编码已存在");

    // ========== QMS 检验单（1-040-101-000） ==========
    ErrorCode INSPECTION_ORDER_NOT_EXISTS = new ErrorCode(1_044_000_011, "检验单不存在");
    ErrorCode INSPECTION_ORDER_NOT_SUBMIT = new ErrorCode(1_044_000_012, "检验单不是待检验或检验中状态，无法提交检验结果");
    ErrorCode INSPECTION_ORDER_NO_EXISTS = new ErrorCode(1_044_000_013, "生成检验单号失败，请重新提交");

    // ========== QMS 检验记录（1-040-102-000） ==========
    ErrorCode INSPECTION_RECORD_NOT_EXISTS = new ErrorCode(1_044_000_014, "检验记录不存在");

    // ========== QMS CAPA 文档（1-040-103-000） ==========
    ErrorCode CAPA_DOCUMENT_NOT_EXISTS = new ErrorCode(1_044_000_015, "CAPA 文档不存在");
    ErrorCode CAPA_DOCUMENT_NOT_CLOSE = new ErrorCode(1_044_000_016, "CAPA 文档不是待处理或处理中状态，无法关闭");
    // P0-4 CAPA 全流程状态机错误码
    ErrorCode CAPA_DOCUMENT_STAGE_TRANSITION_INVALID = new ErrorCode(1_040_103_010, "CAPA 阶段流转非法：当前阶段={}，目标阶段={}，仅允许前进或后退 1 步");
    ErrorCode CAPA_DOCUMENT_ROOT_CAUSE_REQUIRED = new ErrorCode(1_040_103_011, "进入纠正措施阶段前，必须填写根本原因分析");
    ErrorCode CAPA_DOCUMENT_CORRECTIVE_ACTION_REQUIRED = new ErrorCode(1_040_103_012, "进入预防措施阶段前，必须填写纠正措施");
    ErrorCode CAPA_DOCUMENT_PREVENTIVE_ACTION_REQUIRED = new ErrorCode(1_040_103_013, "进入有效性验证阶段前，必须填写预防措施");
    ErrorCode CAPA_DOCUMENT_VERIFY_RESULT_REQUIRED = new ErrorCode(1_040_103_014, "关闭 CAPA 前，有效性验证结果必须为「通过」");
    ErrorCode CAPA_DOCUMENT_NOT_VERIFICATION_STAGE = new ErrorCode(1_040_103_015, "CAPA 当前阶段不是「有效性验证」，无法填写验证结果");
    ErrorCode CAPA_DOCUMENT_VERIFY_COMMENT_REQUIRED = new ErrorCode(1_040_103_016, "提交验证结果时必须填写验证意见");

    // ========== QMS SPC 统计过程控制（1-040-104-000） ==========
    ErrorCode SPC_LOT_SIZE_INVALID = new ErrorCode(1_044_000_017, "批量大小必须大于 0");
    ErrorCode SPC_INSPECTION_ITEM_NO_SPEC_LIMIT = new ErrorCode(1_044_000_018, "检验项目未配置规格上下限，无法计算 Cp/Cpk");

    // ========== QMS 不合格品处理 NCR（1-040-105-000） ==========
    ErrorCode NCR_DOCUMENT_NOT_EXISTS = new ErrorCode(1_044_000_019, "NCR 不合格品报告不存在");
    ErrorCode NCR_DOCUMENT_NOT_SUBMIT_MRB = new ErrorCode(1_044_000_020, "NCR 当前状态不是待处理，无法提交 MRB 评审");
    ErrorCode NCR_DOCUMENT_NOT_MRB_REVIEW = new ErrorCode(1_044_000_021, "NCR 当前状态不是 MRB 评审中，无法记录处置决议");
    ErrorCode NCR_DOCUMENT_NOT_DISPOSITIONED = new ErrorCode(1_044_000_022, "NCR 当前状态不是已处置，无法关闭");
    ErrorCode NCR_MRB_RECORD_NOT_EXISTS = new ErrorCode(1_040_105_004, "MRB 评审记录不存在");

    // ========== QMS FMEA 失效模式分析（1-040-106-000） ==========
    ErrorCode FMEA_DOCUMENT_NOT_EXISTS = new ErrorCode(1_044_000_023, "FMEA 文档不存在");
    ErrorCode FMEA_ITEM_NOT_EXISTS = new ErrorCode(1_044_000_024, "FMEA 条目不存在");
    ErrorCode FMEA_ITEM_SOD_INVALID = new ErrorCode(1_044_000_025, "严重度/频度/探测度必须在 1-10 之间");

    // ========== QMS MSA 测量系统分析（1-040-107-000） ==========
    ErrorCode MSA_STUDY_NOT_EXISTS = new ErrorCode(1_044_000_026, "MSA 研究记录不存在");
    ErrorCode MSA_MEASUREMENT_NOT_EXISTS = new ErrorCode(1_044_000_027, "MSA 测量数据不存在");
    ErrorCode MSA_STUDY_NOT_COMPLETED = new ErrorCode(1_044_000_028, "MSA 研究未完成，无法计算 GR&R");
    ErrorCode MSA_STUDY_DATA_NOT_ENOUGH = new ErrorCode(1_044_000_029, "MSA 测量数据不足，无法计算 GR&R");

    // ========== QMS 质量追溯（1-040-108-000） ==========
    ErrorCode QUALITY_TRACEABILITY_NO_DATA = new ErrorCode(1_044_000_030, "追溯查询无数据");

    // ========== QMS 电子签名（1-040-109-000） ==========
    ErrorCode ELECTRONIC_SIGNATURE_USERNAME_REQUIRED = new ErrorCode(1_044_000_031, "电子签名请求头缺少用户名");
    ErrorCode ELECTRONIC_SIGNATURE_PASSWORD_REQUIRED = new ErrorCode(1_044_000_032, "电子签名请求头缺少密码");
    ErrorCode ELECTRONIC_SIGNATURE_PASSWORD_ERROR = new ErrorCode(1_040_109_002, "电子签名密码验证失败");
    ErrorCode ELECTRONIC_SIGNATURE_REASON_REQUIRED = new ErrorCode(1_040_109_003, "当前操作需要填写电子签名理由");
    // 认证服务不可用时必须 fail-closed（21 CFR Part 11：签名不可绕过），禁止降级放行
    ErrorCode ELECTRONIC_SIGNATURE_AUTH_SERVICE_UNAVAILABLE = new ErrorCode(1_044_000_050, "电子签名认证服务不可用，操作已拒绝");

    // ========== QMS 计量器具台账（1-040-600-000） ==========
    ErrorCode INSTRUMENT_NOT_EXISTS = new ErrorCode(1_044_000_033, "计量器具不存在");
    ErrorCode INSTRUMENT_CODE_DUPLICATE = new ErrorCode(1_044_000_002, "计量器具编号已存在");
    ErrorCode INSTRUMENT_HAS_CALIBRATIONS = new ErrorCode(1_044_000_003, "计量器具存在校准记录，无法删除");
    ErrorCode INSTRUMENT_STATUS_INVALID = new ErrorCode(1_044_000_004, "计量器具状态非法");
    ErrorCode INSTRUMENT_NOT_CALIBRATABLE = new ErrorCode(1_044_000_005, "计量器具为报废或封存状态，无法记录校准");

    // ========== QMS 计量器具校准记录（1-040-601-000） ==========
    ErrorCode CALIBRATION_NOT_EXISTS = new ErrorCode(1_044_000_034, "校准记录不存在");
    ErrorCode CALIBRATION_NO_DUPLICATE = new ErrorCode(1_044_000_035, "校准证书编号已存在");

    // ========== QMS 受控文档（1-040-700-000） ==========
    ErrorCode DOCUMENT_NOT_EXISTS = new ErrorCode(1_044_000_006, "受控文档不存在");
    ErrorCode DOCUMENT_NO_DUPLICATE = new ErrorCode(1_044_000_007, "文件编号已存在");
    ErrorCode DOCUMENT_STATUS_INVALID = new ErrorCode(1_044_000_036, "受控文档状态非法，不允许当前操作");

    // ========== QMS 文档分发记录（1-040-701-000） ==========
    ErrorCode DOCUMENT_DISTRIBUTE_NOT_EXISTS = new ErrorCode(1_044_000_037, "文档分发记录不存在");

    // ========== QMS 文件变更申请（1-040-702-000） ==========
    ErrorCode DOCUMENT_CHANGE_REQUEST_NOT_EXISTS = new ErrorCode(1_044_000_038, "文件变更申请不存在");
    ErrorCode DOCUMENT_CHANGE_REQUEST_STATUS_INVALID = new ErrorCode(1_044_000_039, "文件变更申请状态非法，不允许当前操作");

    // ========== QMS 审核计划（1-040-710-000） ==========
    ErrorCode AUDIT_PLAN_NOT_EXISTS = new ErrorCode(1_044_000_040, "审核计划不存在");
    ErrorCode AUDIT_PLAN_NO_DUPLICATE = new ErrorCode(1_044_000_041, "审核计划编号已存在");
    ErrorCode AUDIT_PLAN_STATUS_INVALID = new ErrorCode(1_044_000_042, "审核计划状态非法，不允许当前操作");

    // ========== QMS 审核报告（1-040-711-000） ==========
    ErrorCode AUDIT_REPORT_NOT_EXISTS = new ErrorCode(1_044_000_043, "审核报告不存在");

    // ========== QMS 审核不符合项（1-040-712-000） ==========
    ErrorCode AUDIT_NONCONFORMITY_NOT_EXISTS = new ErrorCode(1_044_000_044, "审核不符合项不存在");
    ErrorCode AUDIT_NC_STATUS_INVALID = new ErrorCode(1_044_000_045, "审核不符合项状态非法，不允许当前操作");

    // ========== QMS 8D 报告（1-040-800-000） ==========
    ErrorCode EIGHT_D_REPORT_NOT_EXISTS = new ErrorCode(1_044_000_000, "8D 报告不存在");
    ErrorCode EIGHT_D_REPORT_STAGE_INVALID = new ErrorCode(1_044_000_001, "8D 报告已关闭，无法推进阶段");

    // ========== QMS 培训计划（1-040-810-000） ==========
    ErrorCode TRAINING_PLAN_NOT_EXISTS = new ErrorCode(1_044_000_008, "培训计划不存在");
    // ========== QMS 培训记录（1-040-811-000） ==========
    ErrorCode TRAINING_RECORD_NOT_EXISTS = new ErrorCode(1_040_811_000, "培训记录不存在");
    // ========== QMS 资格管理（1-040-812-000） ==========
    ErrorCode QUALIFICATION_NOT_EXISTS = new ErrorCode(1_040_812_000, "岗位资格不存在");

    // ========== QMS 客户投诉（1-040-820-000） ==========
    ErrorCode CUSTOMER_COMPLAINT_NOT_EXISTS = new ErrorCode(1_040_820_000, "客户投诉不存在");
    ErrorCode CUSTOMER_COMPLAINT_STATUS_INVALID = new ErrorCode(1_040_820_001, "客户投诉状态非法，不允许当前操作");

    // ========== QMS 供应商评级（1-040-830-000） ==========
    ErrorCode SUPPLIER_RATING_NOT_EXISTS = new ErrorCode(1_040_830_000, "供应商评级不存在");
    // ========== QMS 供应商纠正措施 SCAR（1-040-831-000） ==========
    ErrorCode SCAR_NOT_EXISTS = new ErrorCode(1_040_831_000, "SCAR 供应商纠正措施请求不存在");
    ErrorCode SCAR_STATUS_INVALID = new ErrorCode(1_040_831_001, "SCAR 状态非法，不允许当前操作");
    // ========== QMS 供应商审核（1-040-832-000） ==========
    ErrorCode SUPPLIER_AUDIT_NOT_EXISTS = new ErrorCode(1_040_832_000, "供应商审核不存在");

    // ========== QMS 质量成本 PAIF（1-040-840-000） ==========
    ErrorCode QUALITY_COST_NOT_EXISTS = new ErrorCode(1_040_840_000, "质量成本记录不存在");
    ErrorCode QUALITY_COST_PERIOD_INVALID = new ErrorCode(1_040_840_001, "质量成本期间非法：月份必须在 1-12 之间");
    ErrorCode QUALITY_COST_PERIOD_RANGE_INVALID = new ErrorCode(1_040_840_002, "质量成本期间区间非法：起始月份不能大于结束月份");

}