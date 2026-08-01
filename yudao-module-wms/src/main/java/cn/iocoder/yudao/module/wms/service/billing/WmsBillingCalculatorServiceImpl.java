package cn.iocoder.yudao.module.wms.service.billing;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingBillDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingBillLineDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingContractDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingContractItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.crossdock.WmsCrossDockOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.movement.WmsMovementOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.receipt.WmsReceiptOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.iocoder.yudao.module.wms.dal.mysql.billing.WmsBillingContractItemMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.billing.WmsBillingContractMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.crossdock.WmsCrossDockOrderMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.movement.WmsMovementOrderMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.receipt.WmsReceiptOrderMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.BILLING_CONTRACT_NOT_EFFECTIVE;

/**
 * WMS 3PL 计费引擎 Service 实现类
 *
 * 内置三个计费策略：
 *  1. {@link WmsStorageFeeCalculator}    仓储费（按天）
 *  2. {@link WmsHandlingFeeCalculator}   操作费/装卸费（按次/按件）
 *  3. {@link WmsCrossDockFeeCalculator}  越库费（按次/按件）
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class WmsBillingCalculatorServiceImpl implements WmsBillingCalculatorService {

    /**
     * 费用类型：10 仓储费
     */
    public static final int FEE_TYPE_STORAGE = 10;
    /**
     * 费用类型：20 操作费
     */
    public static final int FEE_TYPE_HANDLING = 20;
    /**
     * 费用类型：30 装卸费
     */
    public static final int FEE_TYPE_LOADING = 30;
    /**
     * 费用类型：40 越库费
     */
    public static final int FEE_TYPE_CROSS_DOCK = 40;
    /**
     * 费用类型：50 其他
     */
    public static final int FEE_TYPE_OTHER = 50;

    /**
     * 计费方式：10 按天
     */
    public static final int FEE_MODE_BY_DAY = 10;
    /**
     * 计费方式：20 按次
     */
    public static final int FEE_MODE_BY_TIME = 20;
    /**
     * 计费方式：30 按件
     */
    public static final int FEE_MODE_BY_PIECE = 30;

    /**
     * 账单状态：10 草稿
     */
    public static final int BILL_STATUS_DRAFT = 10;
    /**
     * 合同状态：10 生效
     */
    public static final int CONTRACT_STATUS_EFFECTIVE = 10;

    @Resource
    private WmsBillingContractMapper billingContractMapper;
    @Resource
    private WmsBillingContractItemMapper billingContractItemMapper;
    @Resource
    private WmsReceiptOrderMapper receiptOrderMapper;
    @Resource
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Resource
    private WmsMovementOrderMapper movementOrderMapper;
    @Resource
    private WmsCrossDockOrderMapper crossDockOrderMapper;
    @Resource
    private WmsInventoryMapper inventoryMapper;

    @Override
    public BillingCalculationResult calculateBilling(Long ownerId, LocalDateTime start, LocalDateTime end) {
        // 1. 查找生效合同（状态=生效，且周期被合同期限覆盖）
        WmsBillingContractDO contract = billingContractMapper.selectOne(new LambdaQueryWrapperX<WmsBillingContractDO>()
                .eq(WmsBillingContractDO::getOwnerId, ownerId)
                .eq(WmsBillingContractDO::getStatus, CONTRACT_STATUS_EFFECTIVE)
                .le(WmsBillingContractDO::getStartDate, start.toLocalDate())
                .ge(WmsBillingContractDO::getEndDate, end.toLocalDate())
                .last("LIMIT 1"));
        if (contract == null) {
            throw exception(BILLING_CONTRACT_NOT_EFFECTIVE);
        }

        // 2. 获取合同条款
        List<WmsBillingContractItemDO> items = billingContractItemMapper.selectListByContractId(contract.getId());
        // 3. 按条款类型选择策略并计算明细
        List<WmsBillingBillLineDO> lines = new ArrayList<>(items.size());
        for (WmsBillingContractItemDO item : items) {
            BigDecimal quantity = calculateQuantity(item, ownerId, start, end);
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal amount = quantity.multiply(item.getUnitPrice());
            // 应用最低收费
            if (item.getMinCharge() != null && amount.compareTo(item.getMinCharge()) < 0) {
                amount = item.getMinCharge();
            }
            lines.add(new WmsBillingBillLineDO()
                    .setContractItemId(item.getId())
                    .setFeeType(item.getFeeType())
                    .setFeeMode(item.getFeeMode())
                    .setQuantity(quantity)
                    .setUnitPrice(item.getUnitPrice())
                    .setAmount(amount));
        }

        // 4. 汇总账单
        BigDecimal totalAmount = lines.stream()
                .map(WmsBillingBillLineDO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        WmsBillingBillDO bill = new WmsBillingBillDO()
                .setOwnerId(ownerId)
                .setBillingPeriodStart(start)
                .setBillingPeriodEnd(end)
                .setTotalAmount(totalAmount)
                .setStatus(BILL_STATUS_DRAFT);
        return new BillingCalculationResult(bill, lines);
    }

    /**
     * 根据条款类型选择计费策略，计算数量
     */
    private BigDecimal calculateQuantity(WmsBillingContractItemDO item, Long ownerId,
                                         LocalDateTime start, LocalDateTime end) {
        switch (item.getFeeType()) {
            case FEE_TYPE_STORAGE:
                return WmsStorageFeeCalculator.calculate(item, ownerId, start, end, inventoryMapper);
            case FEE_TYPE_HANDLING:
            case FEE_TYPE_LOADING:
                return WmsHandlingFeeCalculator.calculate(item, ownerId, start, end,
                        receiptOrderMapper, shipmentOrderMapper, movementOrderMapper);
            case FEE_TYPE_CROSS_DOCK:
                return WmsCrossDockFeeCalculator.calculate(item, ownerId, start, end, crossDockOrderMapper);
            case FEE_TYPE_OTHER:
            default:
                // 其他费用：固定 1 次
                return BigDecimal.ONE;
        }
    }

    // ==================== 计费策略 ====================

    /**
     * 仓储费策略：按天计费。
     *
     * 数量 = 计费周期天数；仅当货主在周期内有库存时计费（quantity > 0）。
     */
    static class WmsStorageFeeCalculator {

        static BigDecimal calculate(WmsBillingContractItemDO item, Long ownerId,
                                    LocalDateTime start, LocalDateTime end,
                                    WmsInventoryMapper inventoryMapper) {
            // 货主在周期内是否持有库存
            Long inventoryCount = inventoryMapper.selectCount(new LambdaQueryWrapperX<WmsInventoryDO>()
                    .eq(WmsInventoryDO::getOwnerId, ownerId)
                    .gt(WmsInventoryDO::getQuantity, BigDecimal.ZERO));
            if (inventoryCount == null || inventoryCount == 0) {
                return BigDecimal.ZERO;
            }
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            return BigDecimal.valueOf(days);
        }

    }

    /**
     * 操作费/装卸费策略：按次或按件计费。
     *
     * 数量 = 周期内入库/出库/移库单据数量（按次）或单据总数量之和（按件）。
     */
    static class WmsHandlingFeeCalculator {

        static BigDecimal calculate(WmsBillingContractItemDO item, Long ownerId,
                                    LocalDateTime start, LocalDateTime end,
                                    WmsReceiptOrderMapper receiptOrderMapper,
                                    WmsShipmentOrderMapper shipmentOrderMapper,
                                    WmsMovementOrderMapper movementOrderMapper) {
            // 周期内的入库/出库/移库单据
            List<WmsReceiptOrderDO> receiptOrders = receiptOrderMapper.selectList(
                    new LambdaQueryWrapperX<WmsReceiptOrderDO>()
                            .ge(WmsReceiptOrderDO::getCreateTime, start)
                            .le(WmsReceiptOrderDO::getCreateTime, end));
            List<WmsShipmentOrderDO> shipmentOrders = shipmentOrderMapper.selectList(
                    new LambdaQueryWrapperX<WmsShipmentOrderDO>()
                            .ge(WmsShipmentOrderDO::getCreateTime, start)
                            .le(WmsShipmentOrderDO::getCreateTime, end));
            List<WmsMovementOrderDO> movementOrders = movementOrderMapper.selectList(
                    new LambdaQueryWrapperX<WmsMovementOrderDO>()
                            .ge(WmsMovementOrderDO::getCreateTime, start)
                            .le(WmsMovementOrderDO::getCreateTime, end));

            if (item.getFeeMode() != null && item.getFeeMode() == FEE_MODE_BY_PIECE) {
                // 按件：累加单据总数量
                BigDecimal totalQuantity = BigDecimal.ZERO;
                totalQuantity = addQuantities(totalQuantity, receiptOrders, WmsReceiptOrderDO::getTotalQuantity);
                totalQuantity = addQuantities(totalQuantity, shipmentOrders, WmsShipmentOrderDO::getTotalQuantity);
                totalQuantity = addQuantities(totalQuantity, movementOrders, WmsMovementOrderDO::getTotalQuantity);
                return totalQuantity;
            }
            // 按次（默认）：单据数量
            long count = receiptOrders.size() + shipmentOrders.size() + movementOrders.size();
            return BigDecimal.valueOf(count);
        }

    }

    /**
     * 越库费策略：按次或按件计费。
     *
     * 数量 = 周期内越库单据数量（按次）或越库总数量之和（按件）；仅统计该货主参与的越库单。
     */
    static class WmsCrossDockFeeCalculator {

        static BigDecimal calculate(WmsBillingContractItemDO item, Long ownerId,
                                    LocalDateTime start, LocalDateTime end,
                                    WmsCrossDockOrderMapper crossDockOrderMapper) {
            // 周期内该货主参与的越库单（作为供应商或客户）
            List<WmsCrossDockOrderDO> crossDockOrders = crossDockOrderMapper.selectList(
                    new LambdaQueryWrapperX<WmsCrossDockOrderDO>()
                            .ge(WmsCrossDockOrderDO::getCreateTime, start)
                            .le(WmsCrossDockOrderDO::getCreateTime, end)
                            .and(w -> w.eq(WmsCrossDockOrderDO::getSourceSupplierId, ownerId)
                                    .or().eq(WmsCrossDockOrderDO::getTargetCustomerId, ownerId)));
            if (CollUtil.isEmpty(crossDockOrders)) {
                return BigDecimal.ZERO;
            }
            if (item.getFeeMode() != null && item.getFeeMode() == FEE_MODE_BY_PIECE) {
                // 按件：累加越库单总数量
                return crossDockOrders.stream()
                        .map(WmsCrossDockOrderDO::getTotalQuantity)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            // 按次（默认）：越库单数量
            return BigDecimal.valueOf(crossDockOrders.size());
        }

    }

    /**
     * 累加单据列表中的总数量字段
     */
    private static <T> BigDecimal addQuantities(BigDecimal total, List<T> orders,
                                                java.util.function.Function<T, BigDecimal> getter) {
        if (CollUtil.isEmpty(orders)) {
            return total;
        }
        for (T order : orders) {
            BigDecimal qty = getter.apply(order);
            if (qty != null) {
                total = total.add(qty);
            }
        }
        return total;
    }

}
