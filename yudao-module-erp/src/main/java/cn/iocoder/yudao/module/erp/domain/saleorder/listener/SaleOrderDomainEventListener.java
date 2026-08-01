package cn.iocoder.yudao.module.erp.domain.saleorder.listener;

import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.domain.saleorder.event.SaleOrderAuditedEvent;
import cn.iocoder.yudao.module.erp.domain.saleorder.event.SaleOrderCreatedEvent;
import cn.iocoder.yudao.module.erp.domain.saleorder.event.SaleOrderOutCountUpdatedEvent;
import cn.iocoder.yudao.module.erp.domain.saleorder.event.SaleOrderReturnCountUpdatedEvent;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_ORDER_STOCK_LOCK_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_ORDER_STOCK_OUT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_ORDER_STOCK_RETURN_FAIL;

/**
 * 销售订单领域事件监听器（DDD 试点）
 *
 * <p>监听 {@code domain.saleorder.event} 包下的领域事件，演示跨模块解耦：
 * 销售模块通过聚合根产生事件，下游模块（库存 / 财务 / BPM）按需订阅，
 * 无需销售模块直接依赖这些模块的 Service。
 *
 * <p>事件处理策略：
 * <ul>
 *     <li>订单创建事件：事务提交后处理（仅日志，无副作用）</li>
 *     <li>库存操作事件：事务提交前处理（{@link TransactionPhase#BEFORE_COMMIT}），
 *     确保库存操作与主业务在同一事务中，失败时整体回滚，避免孤儿订单</li>
 * </ul>
 *
 * <p>库存操作说明：
 * <ul>
 *     <li>订单审批 → 锁定可用库存（lockedCount += delta），预留出库</li>
 *     <li>出库数量更新 → 扣减实际库存（count -= delta）并释放对应锁定</li>
 *     <li>退货数量更新 → 恢复库存（count += delta），退货入库</li>
 * </ul>
 *
 * @author DDD 试点
 */
@Slf4j
@Component
public class SaleOrderDomainEventListener {

    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpSaleOrderService saleOrderService;

    /**
     * 监听订单创建事件
     *
     * <p>创建时无副作用，仅记录日志。后续可对接：初始化应收记录、推送创建通知。
     *
     * @param event 创建事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(SaleOrderCreatedEvent event) {
        log.info("[onCreated][销售订单已创建，订单编号={}，订单号={}]",
                event.getOrderId(), event.getOrderNo());
    }

    /**
     * 监听订单审批通过事件：锁定可用库存（预留）
     *
     * <p>查询订单明细，对每个产品按"逐仓库扣减"策略锁定可用库存：
     * 遍历该产品在各仓库的库存记录，按可用库存（count - lockedCount）从大到小依次锁定，直到满足订单需求。
     *
     * <p>使用 BEFORE_COMMIT 阶段：库存锁定失败时回滚主事务（订单审批失败），避免"已审批但未锁定库存"的孤儿状态。
     *
     * @param event 审批事件
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onAudited(SaleOrderAuditedEvent event) {
        Long orderId = event.getOrderId();
        log.info("[onAudited][销售订单已审批，开始锁定库存，订单编号={}，订单号={}]",
                orderId, event.getOrderNo());
        try {
            // 1. 查询订单明细
            List<ErpSaleOrderItemDO> items = saleOrderService.getSaleOrderItemListByOrderId(orderId);
            if (items == null || items.isEmpty()) {
                log.warn("[onAudited][订单 {} 无明细，跳过库存锁定]", orderId);
                return;
            }
            // 2. 逐明细锁定库存
            for (ErpSaleOrderItemDO item : items) {
                BigDecimal needCount = item.getCount() == null ? BigDecimal.ZERO : item.getCount();
                if (needCount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                // 查询该产品在各仓库的库存记录，按可用库存依次锁定
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
                        log.info("[onAudited][订单 {} 产品 {} 仓库 {} 锁定库存 {}]",
                                orderId, item.getProductId(), stock.getWarehouseId(), lockAmount);
                    }
                }
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    log.warn("[onAudited][订单 {} 产品 {} 库存不足，仍需 {} 未锁定]",
                            orderId, item.getProductId(), remaining);
                }
            }
            log.info("[onAudited][订单 {} 库存锁定完成]", orderId);
        } catch (Exception e) {
            log.error("[onAudited][订单 {} 库存锁定异常，事务将回滚]", orderId, e);
            throw exception(SALE_ORDER_STOCK_LOCK_FAIL, orderId);
        }
    }

    /**
     * 监听出库数量更新事件：扣减实际库存 + 释放对应锁定
     *
     * <p>使用 BEFORE_COMMIT 阶段：库存扣减失败时回滚主事务，避免"已出库但库存未扣减"的数据不一致。
     *
     * @param event 出库事件
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOutCountUpdated(SaleOrderOutCountUpdatedEvent event) {
        Long orderId = event.getOrderId();
        BigDecimal outCount = event.getNewOutCount() == null ? BigDecimal.ZERO : event.getNewOutCount();
        log.info("[onOutCountUpdated][销售订单出库数量更新，订单编号={}，订单号={}，累计出库={}]",
                orderId, event.getOrderNo(), outCount);
        try {
            // 1. 查询订单明细
            List<ErpSaleOrderItemDO> items = saleOrderService.getSaleOrderItemListByOrderId(orderId);
            if (items == null || items.isEmpty()) {
                log.warn("[onOutCountUpdated][订单 {} 无明细，跳过库存扣减]", orderId);
                return;
            }
            // 2. 计算订单总数量，用于按比例分配累计出库数量到各明细
            BigDecimal totalCount = BigDecimal.ZERO;
            for (ErpSaleOrderItemDO item : items) {
                BigDecimal c = item.getCount() == null ? BigDecimal.ZERO : item.getCount();
                totalCount = totalCount.add(c);
            }
            if (totalCount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("[onOutCountUpdated][订单 {} 总数量为 0，跳过]", orderId);
                return;
            }
            // 3. 按明细数量比例分配出库数量，逐产品扣减库存 + 释放锁定
            for (ErpSaleOrderItemDO item : items) {
                BigDecimal itemQty = item.getCount() == null ? BigDecimal.ZERO : item.getCount();
                if (itemQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal share = outCount.multiply(itemQty)
                        .divide(totalCount, 4, RoundingMode.HALF_UP);
                if (share.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                List<ErpStockDO> stocks = stockService.getStockListByProductId(item.getProductId());
                // 3.1 释放此前锁定的数量（lockedCount -= share）
                BigDecimal releaseRemaining = share;
                for (ErpStockDO stock : stocks) {
                    if (releaseRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    BigDecimal locked = stock.getLockedCount() == null ? BigDecimal.ZERO : stock.getLockedCount();
                    BigDecimal releaseAmount = locked.compareTo(releaseRemaining) >= 0 ? releaseRemaining : locked;
                    if (releaseAmount.compareTo(BigDecimal.ZERO) > 0) {
                        stockService.unlockStock(item.getProductId(), stock.getWarehouseId(), releaseAmount);
                        releaseRemaining = releaseRemaining.subtract(releaseAmount);
                    }
                }
                // 3.2 实际扣减库存（count -= share）
                BigDecimal deductRemaining = share;
                for (ErpStockDO stock : stocks) {
                    if (deductRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    BigDecimal stockCount = stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
                    BigDecimal deductAmount = stockCount.compareTo(deductRemaining) >= 0 ? deductRemaining : stockCount;
                    if (deductAmount.compareTo(BigDecimal.ZERO) > 0) {
                        stockService.updateStockCountIncrement(item.getProductId(), stock.getWarehouseId(), deductAmount.negate());
                        deductRemaining = deductRemaining.subtract(deductAmount);
                        log.info("[onOutCountUpdated][订单 {} 产品 {} 仓库 {} 扣减库存 {}]",
                                orderId, item.getProductId(), stock.getWarehouseId(), deductAmount);
                    }
                }
                if (deductRemaining.compareTo(BigDecimal.ZERO) > 0) {
                    log.warn("[onOutCountUpdated][订单 {} 产品 {} 库存不足，仍需 {} 未扣减]",
                            orderId, item.getProductId(), deductRemaining);
                }
            }
            log.info("[onOutCountUpdated][订单 {} 库存扣减完成]", orderId);
        } catch (Exception e) {
            log.error("[onOutCountUpdated][订单 {} 库存扣减异常，事务将回滚]", orderId, e);
            throw exception(SALE_ORDER_STOCK_OUT_FAIL, orderId);
        }
    }

    /**
     * 监听退货数量更新事件：恢复库存（退货入库）
     *
     * <p>使用 BEFORE_COMMIT 阶段：库存恢复失败时回滚主事务，避免"已退货但库存未恢复"的数据不一致。
     *
     * @param event 退货事件
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onReturnCountUpdated(SaleOrderReturnCountUpdatedEvent event) {
        Long orderId = event.getOrderId();
        BigDecimal returnCount = event.getNewReturnCount() == null ? BigDecimal.ZERO : event.getNewReturnCount();
        log.info("[onReturnCountUpdated][销售订单退货数量更新，订单编号={}，订单号={}，累计退货={}]",
                orderId, event.getOrderNo(), returnCount);
        try {
            // 1. 查询订单明细
            List<ErpSaleOrderItemDO> items = saleOrderService.getSaleOrderItemListByOrderId(orderId);
            if (items == null || items.isEmpty()) {
                log.warn("[onReturnCountUpdated][订单 {} 无明细，跳过库存恢复]", orderId);
                return;
            }
            // 2. 计算订单总数量，用于按比例分配累计退货数量到各明细
            BigDecimal totalCount = BigDecimal.ZERO;
            for (ErpSaleOrderItemDO item : items) {
                BigDecimal c = item.getCount() == null ? BigDecimal.ZERO : item.getCount();
                totalCount = totalCount.add(c);
            }
            if (totalCount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("[onReturnCountUpdated][订单 {} 总数量为 0，跳过]", orderId);
                return;
            }
            // 3. 按明细数量比例分配退货数量，逐产品恢复库存（入库到第一个仓库）
            for (ErpSaleOrderItemDO item : items) {
                BigDecimal itemQty = item.getCount() == null ? BigDecimal.ZERO : item.getCount();
                if (itemQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal share = returnCount.multiply(itemQty)
                        .divide(totalCount, 4, RoundingMode.HALF_UP);
                if (share.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                List<ErpStockDO> stocks = stockService.getStockListByProductId(item.getProductId());
                if (stocks == null || stocks.isEmpty()) {
                    log.warn("[onReturnCountUpdated][订单 {} 产品 {} 无库存记录，跳过退货入库]",
                            orderId, item.getProductId());
                    continue;
                }
                // 退货入库：增加到第一个仓库（简化策略，后续可优化为原出库仓库）
                ErpStockDO firstStock = stocks.get(0);
                stockService.updateStockCountIncrement(item.getProductId(), firstStock.getWarehouseId(), share);
                log.info("[onReturnCountUpdated][订单 {} 产品 {} 仓库 {} 退货入库 {}]",
                        orderId, item.getProductId(), firstStock.getWarehouseId(), share);
            }
            log.info("[onReturnCountUpdated][订单 {} 库存恢复完成]", orderId);
        } catch (Exception e) {
            log.error("[onReturnCountUpdated][订单 {} 库存恢复异常，事务将回滚]", orderId, e);
            throw exception(SALE_ORDER_STOCK_RETURN_FAIL, orderId);
        }
    }
}