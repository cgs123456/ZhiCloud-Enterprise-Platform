package cn.iocoder.yudao.framework.inventory.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * 共享库存错误码（P1-4）
 *
 * <p>使用 1-099 段（framework 级库存 Starter），与业务模块（erp 1-020 / wms 1-030 / mes 1-040 / crm 1-050 / qms 1-070）隔离。
 *
 * @author 智云库存治理
 */
public interface ErrorCodeConstants {

    // ========== 共享库存（1-099-000-000） ==========
    ErrorCode INVENTORY_QUANTITY_NOT_ENOUGH = new ErrorCode(1_099_000_001, "库存可用数量不足");
    ErrorCode INVENTORY_LOCKED_NOT_ENOUGH = new ErrorCode(1_099_000_002, "库存锁定数量不足");

}
