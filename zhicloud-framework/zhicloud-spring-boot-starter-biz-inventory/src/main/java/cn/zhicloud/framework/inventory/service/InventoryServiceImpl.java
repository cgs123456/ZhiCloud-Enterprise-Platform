package cn.zhicloud.framework.inventory.service;

import cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil;
import cn.zhicloud.framework.inventory.dal.dataobject.InventoryItemDO;
import cn.zhicloud.framework.inventory.dal.mysql.InventoryItemMapper;
import cn.zhicloud.framework.inventory.enums.ErrorCodeConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * 共享库存 Service 实现（P1-4 三件套并发保护）
 *
 * <p>由 {@code InventoryAutoConfiguration} 以 {@code @Bean} 注册，故此处不标注 {@code @Service}。
 *
 * <p>并发正确性由三层兜底：
 * <ol>
 *     <li>{@link InventoryItemMapper#selectByCompositeKey} + 插入的 {@link DuplicateKeyException} 兜底
 *         （并发 {@code getOrCreate} 返回同一行，避免 TooManyResultsException / 重复行）；</li>
 *     <li>{@link InventoryItemMapper#updateCountIncrement} / {@link InventoryItemMapper#updateLockedCountIncrement}
 *         的 DB 层 CAS（{@code WHERE ... AND (qty + ?) >= 0}，超扣/超释放返回 0 行并抛异常）；</li>
 *     <li>{@code InventoryItemDO.@Version} 乐观锁（守护任何 {@code updateById} 全量更新路径）。</li>
 * </ol>
 *
 * @author 智云库存治理
 */
@Validated
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    @Resource
    private InventoryItemMapper inventoryItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId,
                    String batchCode, BigDecimal delta) {
        InventoryItemDO stock = getOrCreate(itemId, warehouseId, locationId, areaId, batchId, batchCode);
        int rows = inventoryItemMapper.updateCountIncrement(stock.getId(), delta);
        if (rows == 0) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INVENTORY_QUANTITY_NOT_ENOUGH);
        }
        return stock.getId();
    }

    @Override
    public Long deduct(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal delta) {
        return add(itemId, warehouseId, locationId, areaId, batchId, null, delta.negate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reserve(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal lockDelta) {
        InventoryItemDO stock = getOrCreate(itemId, warehouseId, locationId, areaId, batchId, null);
        int rows = inventoryItemMapper.updateLockedCountIncrement(stock.getId(), lockDelta);
        if (rows == 0) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INVENTORY_LOCKED_NOT_ENOUGH);
        }
        return stock.getId();
    }

    @Override
    public Long release(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal lockDelta) {
        return reserve(itemId, warehouseId, locationId, areaId, batchId, lockDelta.negate());
    }

    @Override
    public BigDecimal getAvailableQuantity(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId) {
        InventoryItemDO stock = inventoryItemMapper.selectByCompositeKey(itemId, warehouseId, locationId, areaId, batchId);
        if (stock == null) {
            return BigDecimal.ZERO;
        }
        return stock.getQuantity().subtract(stock.getLockedCount());
    }

    @Override
    public boolean isSufficient(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId, BigDecimal required) {
        return getAvailableQuantity(itemId, warehouseId, locationId, areaId, batchId).compareTo(required) >= 0;
    }

    /**
     * 规范复合键 get-or-create（并发安全）
     */
    private InventoryItemDO getOrCreate(Long itemId, Long warehouseId, Long locationId, Long areaId,
                                        Long batchId, String batchCode) {
        InventoryItemDO existing = inventoryItemMapper.selectByCompositeKey(itemId, warehouseId, locationId, areaId, batchId);
        if (existing != null) {
            return existing;
        }
        InventoryItemDO n = InventoryItemDO.builder()
                .itemId(itemId).warehouseId(warehouseId).locationId(locationId)
                .areaId(areaId).batchId(batchId).batchCode(batchCode)
                .quantity(BigDecimal.ZERO).lockedCount(BigDecimal.ZERO).tenantId(0L)
                .build();
        try {
            inventoryItemMapper.insert(n);
            return n;
        } catch (DuplicateKeyException e) {
            // 并发插入冲突：回查已有记录，避免 TooManyResultsException 与重复行
            InventoryItemDO again = inventoryItemMapper.selectByCompositeKey(itemId, warehouseId, locationId, areaId, batchId);
            if (again != null) {
                log.warn("[getOrCreate][并发插入冲突 返回已有id={}]", again.getId());
                return again;
            }
            throw e;
        }
    }

}
