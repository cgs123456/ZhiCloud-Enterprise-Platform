package cn.zhicloud.framework.inventory.api;

import cn.zhicloud.framework.inventory.service.InventoryService;
import jakarta.annotation.Resource;

import java.math.BigDecimal;

/**
 * 共享库存 API 实现（P1-4）
 *
 * <p>由 {@code InventoryAutoConfiguration} 以 {@code @Bean} 注册，故此处不标注 {@code @Service}，
 * 以避免与组件扫描产生重复 bean 定义。
 *
 * @author 智云库存治理
 */
public class InventoryApiImpl implements InventoryApi {

    @Resource
    private InventoryService inventoryService;

    @Override
    public BigDecimal getAvailableQuantity(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId) {
        return inventoryService.getAvailableQuantity(itemId, warehouseId, locationId, areaId, batchId);
    }

    @Override
    public boolean isSufficient(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal required) {
        return inventoryService.isSufficient(itemId, warehouseId, locationId, areaId, batchId, required);
    }

    @Override
    public Long add(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId,
                   String batchCode, BigDecimal delta) {
        return inventoryService.add(itemId, warehouseId, locationId, areaId, batchId, batchCode, delta);
    }

    @Override
    public Long deduct(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal delta) {
        return inventoryService.deduct(itemId, warehouseId, locationId, areaId, batchId, delta);
    }

    @Override
    public Long reserve(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal lockDelta) {
        return inventoryService.reserve(itemId, warehouseId, locationId, areaId, batchId, lockDelta);
    }

    @Override
    public Long release(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal lockDelta) {
        return inventoryService.release(itemId, warehouseId, locationId, areaId, batchId, lockDelta);
    }

}
