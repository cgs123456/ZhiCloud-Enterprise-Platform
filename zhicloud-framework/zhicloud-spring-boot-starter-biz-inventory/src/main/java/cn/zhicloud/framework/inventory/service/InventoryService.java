package cn.zhicloud.framework.inventory.service;

import java.math.BigDecimal;

/**
 * 共享库存 Service（P1-4 统一读写原语）
 *
 * @author 智云库存治理
 */
public interface InventoryService {

    BigDecimal getAvailableQuantity(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId);

    boolean isSufficient(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal required);

    Long add(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId,
             String batchCode, BigDecimal delta);

    Long deduct(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal delta);

    Long reserve(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal lockDelta);

    Long release(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal lockDelta);

}
