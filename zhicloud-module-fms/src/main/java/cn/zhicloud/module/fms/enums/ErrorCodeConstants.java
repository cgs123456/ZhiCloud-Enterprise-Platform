package cn.zhicloud.module.fms.enums;

import cn.zhicloud.framework.common.exception.ErrorCode;

/**
 * FMS 错误码枚举类
 * <p>
 * FMS 系统，使用 1-070-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== FMS 账套（1-070-000-000） ==========
    ErrorCode ACCOUNT_SET_NOT_EXISTS = new ErrorCode(1_070_000_000, "账套不存在");
    ErrorCode ACCOUNT_SET_INITIALIZED = new ErrorCode(1_070_000_001, "账套已初始化，不允许重复初始化");
    ErrorCode ACCOUNT_SET_DEFAULT_EXISTS = new ErrorCode(1_070_000_002, "已存在默认账套");
    ErrorCode ACCOUNT_SET_COMPANY_CODE_DUPLICATE = new ErrorCode(1_070_000_003, "公司编码已存在");

    // ========== FMS 币别（1-070-001-000） ==========
    ErrorCode CURRENCY_NOT_EXISTS = new ErrorCode(1_070_001_000, "币别不存在");
    ErrorCode CURRENCY_CODE_DUPLICATE = new ErrorCode(1_070_001_001, "币别编码已存在");
    ErrorCode CURRENCY_STANDARD_EXISTS = new ErrorCode(1_070_001_002, "账套已存在本位币");

    // ========== FMS 凭证字（1-070-002-000） ==========
    ErrorCode VOUCHER_WORD_NOT_EXISTS = new ErrorCode(1_070_002_000, "凭证字不存在");
    ErrorCode VOUCHER_WORD_NAME_DUPLICATE = new ErrorCode(1_070_002_001, "凭证字已存在");

    // ========== FMS 科目（1-070-003-000） ==========
    ErrorCode SUBJECT_NOT_EXISTS = new ErrorCode(1_070_003_000, "科目不存在");
    ErrorCode SUBJECT_CODE_DUPLICATE = new ErrorCode(1_070_003_001, "科目编码已存在");
    ErrorCode SUBJECT_PARENT_NOT_EXISTS = new ErrorCode(1_070_003_002, "上级科目不存在");
    ErrorCode SUBJECT_USED = new ErrorCode(1_070_003_003, "科目已被业务数据引用，不允许删除");
    ErrorCode SUBJECT_LEVEL_LIMIT = new ErrorCode(1_070_003_004, "科目层级超出账套参数限制");

    // ========== FMS 常用摘要（1-070-004-000） ==========
    ErrorCode DIGEST_NOT_EXISTS = new ErrorCode(1_070_004_000, "常用摘要不存在");

    // ========== FMS 辅助核算（1-070-005-000） ==========
    ErrorCode AUXILIARY_TYPE_NOT_EXISTS = new ErrorCode(1_070_005_000, "辅助核算类别不存在");
    ErrorCode AUXILIARY_TYPE_SYSTEM_PRESET_DELETE = new ErrorCode(1_070_005_001, "系统预置辅助核算类别不允许删除");
    ErrorCode AUXILIARY_ITEM_NOT_EXISTS = new ErrorCode(1_070_005_100, "辅助核算项目不存在");
    ErrorCode AUXILIARY_ITEM_CODE_DUPLICATE = new ErrorCode(1_070_005_101, "辅助核算项目编码已存在");

    // ========== FMS 凭证模板（1-070-006-000） ==========
    ErrorCode VOUCHER_TEMPLATE_NOT_EXISTS = new ErrorCode(1_070_006_000, "凭证模板不存在");
    ErrorCode VOUCHER_TEMPLATE_CATEGORY_NOT_EXISTS = new ErrorCode(1_070_006_100, "凭证模板分类不存在");

    // ========== FMS 财务指标（1-070-007-000） ==========
    ErrorCode FINANCE_INDICATOR_NOT_EXISTS = new ErrorCode(1_070_007_000, "财务指标不存在");

    // ========== FMS 账套成员（1-070-008-000） ==========
    ErrorCode ACCOUNT_USER_NOT_MEMBER = new ErrorCode(1_070_008_000, "当前用户不是该账套成员");

    // ========== FMS 账套参数（1-070-009-000） ==========
    ErrorCode FINANCE_PARAMETER_NOT_EXISTS = new ErrorCode(1_070_009_000, "账套参数不存在");

    // ========== FMS 初始余额（1-070-010-000） ==========
    ErrorCode INITIAL_BALANCE_SUBJECT_INVALID = new ErrorCode(1_070_010_000, "科目不存在或不是末级科目，无法录入初始余额");

}
