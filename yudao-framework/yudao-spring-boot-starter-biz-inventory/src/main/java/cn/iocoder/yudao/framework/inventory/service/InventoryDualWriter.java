package cn.iocoder.yudao.framework.inventory.service;

import java.math.BigDecimal;

/**
 * 库存双写写入器 SPI（M2 阶段 B）
 *
 * <p>由各业务模块（ERP / MES）实现并注册为 Spring Bean，
 * 供共享 Starter 的 {@link cn.iocoder.yudao.framework.inventory.config.InventoryDualWriteRegistrar}
 * 在事务提交前统一触发，实现「业务表 + 真值源」同 TX 双写。
 *
 * <p>实现类不反向 import 任何共享 Starter 内部类，仅实现本接口并由 Starter 框架调用。
 *
 * @author 智云库存治理
 */
public interface InventoryDualWriter {

    /**
     * 执行一次库存增量双写。
     *
     * @param itemId      物品编号（规范键）
     * @param warehouseId 仓库编号（规范键）
     * @param locationId  库位编号（规范键，ERP 传入 null 由框架按 0L 处理）
     * @param areaId      库区编号（规范键，ERP 传入 null 由框架按 0L 处理）
     * @param batchId     批次编号（规范键，ERP 传入 null 由框架按 0L 处理）
     * @param batchCode   批次编码（冗余字段，MES 保留，ERP 传 null）
     * @param quantityDelta   数量增量（正=入库，负=出库）
     * @param lockedDelta     锁定数量增量（正=预占，负=释放；MES 恒为 0）
     */
    void dualWrite(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId,
                   String batchCode, BigDecimal quantityDelta, BigDecimal lockedDelta);

}
