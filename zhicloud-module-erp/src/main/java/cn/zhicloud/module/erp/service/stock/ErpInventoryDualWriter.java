package cn.zhicloud.module.erp.service.stock;

import cn.zhicloud.framework.inventory.service.InventoryDualWriter;
import cn.zhicloud.framework.inventory.service.InventoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * ERP 库存双写写入器（M2 阶段 B）
 *
 * <p>将 {@code erp_stock} 的库存变更同步到共享真值源 {@code inventory_item}。
 * ERP 无库位/库区/批次维度，对应参数置 null（对账时按 0L 处理）。
 *
 * @author 智云库存治理
 */
@Slf4j
@Component
public class ErpInventoryDualWriter implements InventoryDualWriter {

    @Resource
    private InventoryService inventoryService;

    @Override
    public void dualWrite(Long itemId, Long warehouseId, Long locationId, Long areaId,
                          Long batchId, String batchCode, BigDecimal quantityDelta, BigDecimal lockedDelta) {
        // 两者均为零或 null 时才跳过（括号确保 || 先于 && 求值）
        boolean qtySkippable = quantityDelta == null || quantityDelta.compareTo(BigDecimal.ZERO) == 0;
        boolean lockSkippable = lockedDelta == null || lockedDelta.compareTo(BigDecimal.ZERO) == 0;
        if (qtySkippable && lockSkippable) {
            return;
        }
        // quantity delta
        if (quantityDelta != null && quantityDelta.compareTo(BigDecimal.ZERO) != 0) {
            try {
                inventoryService.add(itemId, warehouseId, null, null, null, null, quantityDelta);
            } catch (Exception e) {
                log.error("[ErpInventoryDualWriter] quantity 双写失败 productId={} wh={} delta={}",
                        itemId, warehouseId, quantityDelta, e);
                // @bare-throw-ignore 双写失败须触发父级事务回滚，不能用 ServiceException（会吞掉异常导致双写不一致静默）
                throw new RuntimeException("[ErpInventoryDualWriter] 双写 inventory_item quantity 失败，触发事务回滚", e);
            }
        }
        // locked delta
        if (lockedDelta != null && lockedDelta.compareTo(BigDecimal.ZERO) != 0) {
            try {
                if (lockedDelta.compareTo(BigDecimal.ZERO) > 0) {
                    inventoryService.reserve(itemId, warehouseId, null, null, null, lockedDelta);
                } else {
                    inventoryService.release(itemId, warehouseId, null, null, null, lockedDelta.abs());
                }
            } catch (Exception e) {
                log.error("[ErpInventoryDualWriter] lockedCount 双写失败 productId={} wh={} delta={}",
                        itemId, warehouseId, lockedDelta, e);
                // @bare-throw-ignore 双写失败须触发父级事务回滚，不能用 ServiceException（会吞掉异常导致双写不一致静默）
                throw new RuntimeException("[ErpInventoryDualWriter] 双写 inventory_item lockedCount 失败，触发事务回滚", e);
            }
        }
    }

}
