package cn.zhicloud.framework.mdm.enums;

import cn.zhicloud.framework.common.exception.ErrorCode;

/**
 * 主数据跨域映射错误码（V101）
 *
 * <p>使用 1-099-002 段（framework 级 MDM Starter）。1-099-000 段为共享库存 Starter，
 * 1-099-001 段为可靠集成 Starter，业务模块段为 erp 1-020 / wms 1-030 / mes 1-040 / crm 1-050 / qms 1-070。
 *
 * @author 智云集成治理
 */
public interface ErrorCodeConstants {

    // ========== 映射登记 ==========
    ErrorCode MDM_CROSS_REF_ARG_INVALID = new ErrorCode(1_099_002_001,
            "主数据映射参数不完整：sourceSystem / sourceEntity / sourceId / targetSystem / targetEntity / targetId 均不能为空");
    ErrorCode MDM_CROSS_REF_TARGET_ID_INVALID = new ErrorCode(1_099_002_002,
            "主数据映射目标编号不是合法数字：{0}");

    // ========== 映射解析 ==========
    ErrorCode MDM_CROSS_REF_NOT_FOUND = new ErrorCode(1_099_002_003,
            "主数据映射缺失：{0}.{1}={2} -> {3}.{4}，请先维护主数据对照后再重试");
    ErrorCode MDM_CROSS_REF_TENANT_REQUIRED = new ErrorCode(1_099_002_004,
            "主数据映射操作缺少租户上下文");
    ErrorCode MDM_CROSS_REF_TENANT_MISMATCH = new ErrorCode(1_099_002_005,
            "主数据映射租户不一致：当前租户={0}，请求租户={1}");

}