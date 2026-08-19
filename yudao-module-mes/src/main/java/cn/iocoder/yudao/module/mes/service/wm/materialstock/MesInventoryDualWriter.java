package cn.iocoder.yudao.module.mes.service.wm.materialstock;

import cn.iocoder.yudao.framework.inventory.service.InventoryDualWriter;
import cn.iocoder.yudao.framework.inventory.service.InventoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * MES 库存双写写入器（M2 阶段 B）
 *
 * <p>将 {@code mes_wm_material_stock} 的库存变更同步到共享真值源 {@code inventory_item}。
 * MES 无锁定原语，{@code lockedDelta} 恒为 null。
 *
 * @author 智云库存治理
 */
@Slf4j
@Component
public class MesInventoryDualWriter implements InventoryDualWriter {

    @Resource
    private InventoryService inventoryService;

    @Override
    public void dualWrite(Long itemId, Long warehouseId, Long locationId, Long areaId,
                          Long batchId, String batchCode, BigDecimal quantityDelta, BigDecimal lockedDelta) {
        if (quantityDelta == null || quantityDelta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        try {
            inventoryService.add(itemId, warehouseId, locationId, areaId, batchId, batchCode, quantityDelta);
            log.debug("[MesInventoryDualWriter] 双写成功 itemId={} wh={} qtyDelta={}",
                    itemId, warehouseId, quantityDelta);
        } catch (Exception e) {
            log.error("[MesInventoryDualWriter] 双写失败 itemId={} wh={} qtyDelta={}",
                    itemId, warehouseId, quantityDelta, e);
            // @bare-throw-ignore 双写失败须触发父级事务回滚，不能用 ServiceException（会吞掉异常导致双写不一致静默）
            throw new RuntimeException("[MesInventoryDualWriter] 双写 inventory_item 失败，触发事务回滚", e);
        }
    }

}
