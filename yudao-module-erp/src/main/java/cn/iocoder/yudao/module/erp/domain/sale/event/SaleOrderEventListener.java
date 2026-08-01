package cn.iocoder.yudao.module.erp.domain.sale.event;

import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 销售订单领域事件监听器（DDD 试点）
 *
 * <p>监听销售订单的领域事件，演示跨模块解耦：销售模块发布事件，库存模块按需订阅。
 * 事件在事务提交前（{@link TransactionPhase#BEFORE_COMMIT}）处理，确保库存操作与主业务在同一事务中，
 * 失败时整体回滚，避免"已确认但未锁定库存"等数据不一致。
 *
 * <p>库存操作说明：
 * <ul>
 *     <li>订单确认 → 锁定可用库存（lockedCount += delta），预留出库</li>
 *     <li>订单出库 → 实际扣减库存（count -= delta），并释放对应锁定（lockedCount -= delta）</li>
 *     <li>订单取消 → 释放预留库存（lockedCount -= delta）</li>
 * </ul>
 * 从而实现销售 → 库存的单向依赖解耦，销售模块无需直接依赖库存 Service。
 *
 * @author DDD 试点
 */
@Slf4j
@Component
public class SaleOrderEventListener {

    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpSaleOrderService saleOrderService;

    /**
     * 监听订单审核确认事件：预留库存（锁定）
     *
     * <p>使用 BEFORE_COMMIT 阶段：库存锁定失败时回滚主事务，避免"已确认但未锁定库存"的孤儿状态。
     *
     * @param event 确认事件
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onConfirmed(SaleOrderConfirmedEvent event) {
        Long orderId = event.getOrderId();
        log.info("[onConfirmed][订单 {} 审核确认，开始预留库存，客户={}，金额={}]",
                orderId, event.getCustomerId(), event.getTotalAmount());
        try {
            // 1. 查询订单明细
            List<ErpSaleOrderItemDO> items = saleOrderService.getSaleOrderItemListByOrderId(orderId);
            if (items == null || items.isEmpty()) {
                log.warn("[onConfirmed][订单 {} 无明细，跳过库存预留]", orderId);
                return;
            }
            // 2. 逐明细锁定库存
            int successCount = 0;
            for (ErpSaleOrderItemDO item : items) {
                BigDecimal needCount = item.getCount() == null ? BigDecimal.ZERO : item.getCount();
                if (needCount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                // 查询该产品在各仓库的库存记录，按可用库存从大到小锁定
                List<ErpStockDO> stocks = stockService.getStockListByProductId(item.getProductId());
                BigDecimal remaining = needCount;
                for (ErpStockDO stock : stocks) {
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    BigDecimal available = stockService.getAvailableCount(item.getProductId(), stock.getWarehouseId());
                    if (available.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    BigDecimal lockAmount = available.compareTo(remaining) >= 0 ? remaining : available;
                    boolean locked = stockService.lockStock(item.getProductId(), stock.getWarehouseId(), lockAmount);
                    if (locked) {
                        remaining = remaining.subtract(lockAmount);
                        log.info("[onConfirmed][订单 {} 产品 {} 仓库 {} 锁定库存 {}]",
                                orderId, item.getProductId(), stock.getWarehouseId(), lockAmount);
                    }
                }
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    log.warn("[onConfirmed][订单 {} 产品 {} 库存不足，仍需 {} 未锁定]",
                            orderId, item.getProductId(), remaining);
                } else {
                    successCount++;
                }
            }
            log.info("[onConfirmed][订单 {} 库存预留完成，成功 {} 项 / 共 {} 项]", orderId, successCount, items.size());
        } catch (Exception e) {
            log.error("[onConfirmed][订单 {} 库存预留异常，事务将回滚]", orderId, e);
            throw e;
        }
    }

    /**
     * 监听订单出库事件：扣减实际库存 + 释放对应锁定
     *
     * <p>使用 BEFORE_COMMIT 阶段：库存扣减失败时回滚主事务，避免"已出库但库存未扣减"的数据不一致。
     *
     * @param event 出库事件
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onShipped(SaleOrderShippedEvent event) {
        Long orderId = event.getOrderId();
        Map<Long, BigDecimal> outCountMap = event.getOutCountMap();
        log.info("[onShipped][订单 {} 出库，开始扣减库存，明细数={}]", orderId, outCountMap == null ? 0 : outCountMap.size());
        try {
            if (outCountMap == null || outCountMap.isEmpty()) {
                log.warn("[onShipped][订单 {} 出库明细为空，跳过]", orderId);
                return;
            }
            // 1. 查询订单明细，构建 itemId -> productId 映射
            List<ErpSaleOrderItemDO> items = saleOrderService.getSaleOrderItemListByOrderId(orderId);
            if (items == null || items.isEmpty()) {
                log.warn("[onShipped][订单 {} 无明细，跳过库存扣减]", orderId);
                return;
            }
            Map<Long, Long> productMap = new java.util.HashMap<>();
            for (ErpSaleOrderItemDO item : items) {
                productMap.put(item.getId(), item.getProductId());
            }
            // 2. 逐明细扣减库存 + 释放锁定
            for (Map.Entry<Long, BigDecimal> entry : outCountMap.entrySet()) {
                Long itemId = entry.getKey();
                BigDecimal outCount = entry.getValue();
                if (outCount == null || outCount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                Long productId = productMap.get(itemId);
                if (productId == null) {
                    log.warn("[onShipped][订单 {} 明细 {} 未找到对应产品，跳过]", orderId, itemId);
                    continue;
                }
                // 2.1 释放此前锁定的数量（lockedCount -= outCount）
                List<ErpStockDO> stocks = stockService.getStockListByProductId(productId);
                if (stocks == null || stocks.isEmpty()) {
                    log.warn("[onShipped][订单 {} 产品 {} 无库存记录，跳过扣减]", orderId, productId);
                    continue;
                }
                BigDecimal releaseRemaining = outCount;
                for (ErpStockDO stock : stocks) {
                    if (releaseRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    BigDecimal locked = stock.getLockedCount() == null ? BigDecimal.ZERO : stock.getLockedCount();
                    BigDecimal releaseAmount = locked.compareTo(releaseRemaining) >= 0 ? releaseRemaining : locked;
                    if (releaseAmount.compareTo(BigDecimal.ZERO) > 0) {
                        stockService.unlockStock(productId, stock.getWarehouseId(), releaseAmount);
                        releaseRemaining = releaseRemaining.subtract(releaseAmount);
                    }
                }
                // 2.2 实际扣减库存（count -= outCount）
                BigDecimal deductRemaining = outCount;
                for (ErpStockDO stock : stocks) {
                    if (deductRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    BigDecimal stockCount = stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
                    BigDecimal deductAmount = stockCount.compareTo(deductRemaining) >= 0 ? deductRemaining : stockCount;
                    if (deductAmount.compareTo(BigDecimal.ZERO) > 0) {
                        stockService.updateStockCountIncrement(productId, stock.getWarehouseId(), deductAmount.negate());
                        deductRemaining = deductRemaining.subtract(deductAmount);
                        log.info("[onShipped][订单 {} 产品 {} 仓库 {} 扣减库存 {}]",
                                orderId, productId, stock.getWarehouseId(), deductAmount);
                    }
                }
                if (deductRemaining.compareTo(BigDecimal.ZERO) > 0) {
                    log.warn("[onShipped][订单 {} 产品 {} 库存不足，仍需 {} 未扣减]",
                            orderId, productId, deductRemaining);
                }
            }
            log.info("[onShipped][订单 {} 库存扣减完成]", orderId);
        } catch (Exception e) {
            log.error("[onShipped][订单 {} 库存扣减异常，事务将回滚]", orderId, e);
            throw e;
        }
    }

    /**
     * 监听订单取消事件：释放预留库存
     *
     * <p>使用 BEFORE_COMMIT 阶段：库存释放失败时回滚主事务，确保取消操作与库存释放的原子性。
     *
     * @param event 取消事件
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onCancelled(SaleOrderCancelledEvent event) {
        Long orderId = event.getOrderId();
        log.info("[onCancelled][订单 {} 已取消，开始释放预留库存，原因={}]", orderId, event.getReason());
        try {
            // 1. 查询订单明细
            List<ErpSaleOrderItemDO> items = saleOrderService.getSaleOrderItemListByOrderId(orderId);
            if (items == null || items.isEmpty()) {
                log.warn("[onCancelled][订单 {} 无明细，跳过库存释放]", orderId);
                return;
            }
            // 2. 逐明细释放锁定
            for (ErpSaleOrderItemDO item : items) {
                BigDecimal releaseCount = item.getCount() == null ? BigDecimal.ZERO : item.getCount();
                if (releaseCount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                // 查询该产品在各仓库的库存记录，逐仓库释放锁定
                List<ErpStockDO> stocks = stockService.getStockListByProductId(item.getProductId());
                BigDecimal remaining = releaseCount;
                for (ErpStockDO stock : stocks) {
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    BigDecimal locked = stock.getLockedCount() == null ? BigDecimal.ZERO : stock.getLockedCount();
                    BigDecimal releaseAmount = locked.compareTo(remaining) >= 0 ? remaining : locked;
                    if (releaseAmount.compareTo(BigDecimal.ZERO) > 0) {
                        stockService.unlockStock(item.getProductId(), stock.getWarehouseId(), releaseAmount);
                        remaining = remaining.subtract(releaseAmount);
                        log.info("[onCancelled][订单 {} 产品 {} 仓库 {} 释放锁定 {}]",
                                orderId, item.getProductId(), stock.getWarehouseId(), releaseAmount);
                    }
                }
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    log.warn("[onCancelled][订单 {} 产品 {} 无足够锁定记录可释放，剩余 {}]",
                            orderId, item.getProductId(), remaining);
                }
            }
            log.info("[onCancelled][订单 {} 库存释放完成]", orderId);
        } catch (Exception e) {
            log.error("[onCancelled][订单 {} 库存释放异常，事务将回滚]", orderId, e);
            throw e;
        }
    }
}
