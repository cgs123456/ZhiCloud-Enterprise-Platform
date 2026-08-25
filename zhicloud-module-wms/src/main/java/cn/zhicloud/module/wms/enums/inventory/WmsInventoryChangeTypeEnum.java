package cn.zhicloud.module.wms.enums.inventory;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WMS 库存变更类型枚举
 *
 * 用于区分库存三维字段（available/locked/frozen）的变更场景：
 *  - IN      入库：quantity + qty, available + qty
 *  - OUT     出库：quantity - qty, available - qty
 *  - LOCK    锁定：available - qty, locked + qty（如波次拣货预占）
 *  - UNLOCK  解锁：available + qty, locked - qty（如订单取消释放预占）
 *  - FREEZE  冻结：available - qty, frozen + qty（如质检冻结）
 *  - UNFREEZE 解冻：available + qty, frozen - qty（如质检放行）
 *  - PLAIN   普通增减（兼容旧逻辑）：仅更新 quantity，不动三维字段
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum WmsInventoryChangeTypeEnum {

    IN("IN", "入库"),
    OUT("OUT", "出库"),
    LOCK("LOCK", "锁定"),
    UNLOCK("UNLOCK", "解锁"),
    FREEZE("FREEZE", "冻结"),
    UNFREEZE("UNFREEZE", "解冻"),
    PLAIN("PLAIN", "普通增减");

    /**
     * 变更类型代码
     */
    private final String code;
    /**
     * 描述
     */
    private final String description;

}
