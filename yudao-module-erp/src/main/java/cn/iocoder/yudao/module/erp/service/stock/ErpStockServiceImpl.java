package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.inventory.service.InventoryDualWriter;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_COUNT_NEGATIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_COUNT_NEGATIVE2;

/**
 * ERP 产品库存 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpStockServiceImpl implements ErpStockService {

    /**
     * 允许库存为负数
     *
     * TODO 芋艿：后续做成 db 配置
     */
    private static final Boolean NEGATIVE_STOCK_COUNT_ENABLE = false;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;

    @Resource
    private ErpStockMapper stockMapper;

    /**
     * M2 阶段 B：库存双写写入器（enableDualWrite=true 时生效）
     */
    @Autowired(required = false)
    private List<InventoryDualWriter> dualWriters;

    @Override
    public ErpStockDO getStock(Long id) {
        return stockMapper.selectById(id);
    }

    @Override
    public ErpStockDO getStock(Long productId, Long warehouseId) {
        return stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
    }

    @Override
    public java.util.List<ErpStockDO> getStockListByProductId(Long productId) {
        return stockMapper.selectListByProductId(productId);
    }

    @Override
    public BigDecimal getStockCount(Long productId) {
        BigDecimal count = stockMapper.selectSumByProductId(productId);
        return count != null ? count : BigDecimal.ZERO;
    }

    @Override
    public PageResult<ErpStockDO> getStockPage(ErpStockPageReqVO pageReqVO) {
        return stockMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal updateStockCountIncrement(Long productId, Long warehouseId, BigDecimal count) {
        // 1.1 查询当前库存
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        if (stock == null) {
            // 并发初始化：捕获唯一约束冲突（依赖 erp_stock 的 (product_id, warehouse_id) 唯一约束），
            // 冲突后重新查询已插入的记录，避免并发重复插入导致 TooManyResultsException
            stock = new ErpStockDO().setProductId(productId).setWarehouseId(warehouseId).setCount(BigDecimal.ZERO);
            try {
                stockMapper.insert(stock);
            } catch (DuplicateKeyException e) {
                log.info("[updateStockCountIncrement][产品 {} 仓库 {} 库存记录已被并发创建，重新查询]", productId, warehouseId);
                stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
                if (stock == null) {
                    // 理论上不会发生，防御性处理
                    throw exception(STOCK_COUNT_NEGATIVE2, safeProductName(productId), safeWarehouseName(warehouseId));
                }
            }
        }
        // 1.2 校验库存是否充足（基于快照判断，updateCountIncrement 内部有原子兜底）
        BigDecimal currentCount = stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
        if (!NEGATIVE_STOCK_COUNT_ENABLE && currentCount.add(count).compareTo(BigDecimal.ZERO) < 0) {
            throw exception(STOCK_COUNT_NEGATIVE, safeProductName(productId),
                    safeWarehouseName(warehouseId), currentCount, count);
        }

        // 2. 库存变更（原子更新，where 条件兜底防止负库存）
        int updateCount = stockMapper.updateCountIncrement(stock.getId(), count, NEGATIVE_STOCK_COUNT_ENABLE);
        if (updateCount == 0) {
            // 此时不好去查询最新库存，所以直接抛出该提示，不提供具体库存数字
            throw exception(STOCK_COUNT_NEGATIVE2, safeProductName(productId), safeWarehouseName(warehouseId));
        }

        // 3. 返回最新库存（基于快照计算，实际值以 DB 为准）
        triggerDualWrite(productId, warehouseId, count, null);
        return currentCount.add(count);
    }

    /**
     * 安全获取产品名称，避免在异常路径上因产品不存在而 NPE 掩盖原始业务异常
     */
    private String safeProductName(Long productId) {
        try {
            ErpProductDO product = productService.getProduct(productId);
            return product == null ? String.valueOf(productId) : product.getName();
        } catch (Exception e) {
            return String.valueOf(productId);
        }
    }

    /**
     * 安全获取仓库名称，避免在异常路径上因仓库不存在而 NPE 掩盖原始业务异常
     */
    private String safeWarehouseName(Long warehouseId) {
        try {
            ErpWarehouseDO warehouse = warehouseService.getWarehouse(warehouseId);
            return warehouse == null ? String.valueOf(warehouseId) : warehouse.getName();
        } catch (Exception e) {
            return String.valueOf(warehouseId);
        }
    }

    @Override
    public boolean lockStock(Long productId, Long warehouseId, BigDecimal count) {
        if (count == null || count.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        if (stock == null) {
            log.warn("[lockStock][产品 {} 仓库 {} 无库存记录，无法锁定]", productId, warehouseId);
            return false;
        }
        // 校验可用库存是否充足：可用 = count - lockedCount
        BigDecimal available = stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
        BigDecimal locked = stock.getLockedCount() == null ? BigDecimal.ZERO : stock.getLockedCount();
        if (available.subtract(locked).compareTo(count) < 0) {
            log.warn("[lockStock][产品 {} 仓库 {} 可用库存不足，可用={}, 需锁定={}]", productId, warehouseId, available.subtract(locked), count);
            return false;
        }
        int updateCount = stockMapper.updateLockedCountIncrement(stock.getId(), count);
        triggerDualWrite(productId, warehouseId, null, count);
        return updateCount > 0;
    }

    @Override
    public boolean unlockStock(Long productId, Long warehouseId, BigDecimal count) {
        if (count == null || count.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        if (stock == null) {
            log.warn("[unlockStock][产品 {} 仓库 {} 无库存记录，无需释放]", productId, warehouseId);
            return true;
        }
        int updateCount = stockMapper.updateLockedCountIncrement(stock.getId(), count.negate());
        triggerDualWrite(productId, warehouseId, null, count.negate());
        return updateCount > 0;
    }

    private void triggerDualWrite(Long productId, Long warehouseId, BigDecimal quantityDelta,
                                  BigDecimal lockedDelta) {
        if (dualWriters == null || dualWriters.isEmpty()) {
            return;
        }
        for (InventoryDualWriter writer : dualWriters) {
            writer.dualWrite(productId, warehouseId, null, null, null, null, quantityDelta, lockedDelta);
        }
    }

    @Override
    public BigDecimal getAvailableCount(Long productId, Long warehouseId) {
        ErpStockDO stock = stockMapper.selectByProductIdAndWarehouseId(productId, warehouseId);
        if (stock == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal count = stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
        BigDecimal locked = stock.getLockedCount() == null ? BigDecimal.ZERO : stock.getLockedCount();
        return count.subtract(locked);
    }

}