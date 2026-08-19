package cn.iocoder.yudao.framework.inventory.api;

import java.math.BigDecimal;

/**
 * 共享库存对外 API（P1-4 / 提升自 WMS InventoryApi）
 *
 * <p>跨模块库存读写统一契约。对应方案文档 {@code docs/P1-inventory-architecture-migration-plan.md}
 * 中「新增 Starter 对外契约亦置于 api 包」的边界纪律。
 *
 * @author 智云库存治理
 */
public interface InventoryApi {

    /**
     * 查询可用数量 = quantity - lockedCount
     */
    BigDecimal getAvailableQuantity(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId);

    /**
     * 判断可用数量是否满足需求
     */
    boolean isSufficient(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal required);

    /**
     * 增加库存（delta > 0 入库，delta < 0 出库；出库不足时抛 INVENTORY_QUANTITY_NOT_ENOUGH）
     *
     * @return 库存条目 id
     */
    Long add(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId,
             String batchCode, BigDecimal delta);

    /**
     * 扣减库存（等价于 add 传入负 delta）
     *
     * @return 库存条目 id
     */
    Long deduct(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal delta);

    /**
     * 锁定库存（预占，lockDelta > 0 预占，< 0 释放；不足时抛 INVENTORY_LOCKED_NOT_ENOUGH）
     *
     * @return 库存条目 id
     */
    Long reserve(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal lockDelta);

    /**
     * 释放锁定库存（等价于 reserve 传入负 lockDelta）
     *
     * @return 库存条目 id
     */
    Long release(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal lockDelta);

}
