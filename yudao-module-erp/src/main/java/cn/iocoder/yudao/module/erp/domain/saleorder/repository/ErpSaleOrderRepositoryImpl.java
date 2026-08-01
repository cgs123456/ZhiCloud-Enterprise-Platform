package cn.iocoder.yudao.module.erp.domain.saleorder.repository;

import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.domain.saleorder.ErpSaleOrderAggregate;
import cn.iocoder.yudao.module.erp.domain.saleorder.SaleOrderItem;
import cn.iocoder.yudao.module.erp.domain.saleorder.SaleOrderStatus;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 销售订单聚合根仓储实现（DDD 试点 Adapter）
 *
 * <p>基础设施层实现，将聚合根与 DO 互转，内部复用现有的
 * {@link ErpSaleOrderMapper} / {@link ErpSaleOrderItemMapper}。
 *
 * <p>持久化后自动拉取聚合根的领域事件，通过 Spring
 * {@link ApplicationEventPublisher} 发布，监听方通过
 * {@code @TransactionalEventListener} 在事务提交后处理。
 *
 * <p>与现有 {@code ErpSaleOrderServiceImpl} 的持久化逻辑并存，互不影响。
 *
 * @author DDD 试点
 */
@Component
public class ErpSaleOrderRepositoryImpl implements ErpSaleOrderRepository {

    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(ErpSaleOrderAggregate aggregate) {
        ErpSaleOrderDO orderDO = toOrderDO(aggregate);
        boolean isNew = aggregate.getId() == null;
        if (isNew) {
            saleOrderMapper.insert(orderDO);
            aggregate.getId(); // DO 插入后 id 已回填，但聚合根 id 此时仍为 null
        } else {
            saleOrderMapper.updateById(orderDO);
        }
        // 明细：先删后插（聚合根整体持久化，保证一致性）
        Long orderId = orderDO.getId();
        saleOrderItemMapper.deleteByOrderId(orderId);
        List<SaleOrderItem> items = aggregate.getItems();
        if (items != null) {
            for (SaleOrderItem item : items) {
                ErpSaleOrderItemDO itemDO = toItemDO(item, orderId);
                saleOrderItemMapper.insert(itemDO);
            }
        }
        // 发布领域事件（事务提交后由监听器处理）
        for (Object event : aggregate.pullDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
    }

    @Override
    public ErpSaleOrderAggregate findById(Long id) {
        ErpSaleOrderDO orderDO = saleOrderMapper.selectById(id);
        if (orderDO == null) {
            return null;
        }
        List<ErpSaleOrderItemDO> itemDOs = saleOrderItemMapper.selectListByOrderId(id);
        return toAggregate(orderDO, itemDOs);
    }

    @Override
    public List<ErpSaleOrderAggregate> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<ErpSaleOrderDO> orderDOs = saleOrderMapper.selectBatchIds(ids);
        if (orderDOs.isEmpty()) {
            return Collections.emptyList();
        }
        List<ErpSaleOrderItemDO> allItemDOs = saleOrderItemMapper.selectListByOrderIds(ids);
        List<ErpSaleOrderAggregate> result = new ArrayList<>(orderDOs.size());
        for (ErpSaleOrderDO orderDO : orderDOs) {
            List<ErpSaleOrderItemDO> itemDOs = new ArrayList<>();
            for (ErpSaleOrderItemDO itemDO : allItemDOs) {
                if (orderDO.getId().equals(itemDO.getOrderId())) {
                    itemDOs.add(itemDO);
                }
            }
            result.add(toAggregate(orderDO, itemDOs));
        }
        return result;
    }

    // ==================== Aggregate → DO ====================

    /**
     * 聚合根 → 订单头 DO
     */
    private ErpSaleOrderDO toOrderDO(ErpSaleOrderAggregate agg) {
        ErpSaleOrderDO dob = new ErpSaleOrderDO();
        dob.setId(agg.getId());
        dob.setNo(agg.getNo());
        dob.setStatus(agg.getStatus() == null ? null : agg.getStatus().toCode());
        dob.setCustomerId(agg.getCustomerId());
        dob.setAccountId(agg.getAccountId());
        dob.setSaleUserId(agg.getSaleUserId());
        dob.setOrderTime(agg.getOrderTime());
        dob.setTotalCount(agg.getTotalCount());
        dob.setTotalPrice(agg.getTotalPrice());
        dob.setTotalProductPrice(agg.getTotalProductPrice());
        dob.setTotalTaxPrice(agg.getTotalTaxPrice());
        dob.setDiscountPercent(agg.getDiscountPercent());
        dob.setDiscountPrice(agg.getDiscountPrice());
        dob.setDepositPrice(agg.getDepositPrice());
        dob.setCurrencyId(agg.getCurrencyId());
        dob.setExchangeRate(agg.getExchangeRate());
        dob.setBaseCurrencyTotalPrice(agg.getBaseCurrencyTotalPrice());
        dob.setFileUrl(agg.getFileUrl());
        dob.setRemark(agg.getRemark());
        dob.setOutCount(agg.getOutCount());
        dob.setReturnCount(agg.getReturnCount());
        return dob;
    }

    /**
     * 明细值对象 → 明细 DO
     */
    private ErpSaleOrderItemDO toItemDO(SaleOrderItem item, Long orderId) {
        ErpSaleOrderItemDO dob = new ErpSaleOrderItemDO();
        dob.setId(item.getId());
        dob.setOrderId(orderId);
        dob.setProductId(item.getProductId());
        dob.setProductUnitId(item.getProductUnitId());
        dob.setProductPrice(item.getUnitPrice());
        dob.setCount(item.getQuantity());
        dob.setTotalPrice(item.getSubtotal());
        dob.setTaxPercent(item.getTaxRate());
        dob.setTaxPrice(item.getTaxPrice());
        dob.setRemark(item.getRemark());
        dob.setOutCount(item.getOutCount());
        dob.setReturnCount(item.getReturnCount());
        return dob;
    }

    // ==================== DO → Aggregate ====================

    /**
     * DO → 聚合根（通过 reconstitute 重建）
     */
    private ErpSaleOrderAggregate toAggregate(ErpSaleOrderDO orderDO, List<ErpSaleOrderItemDO> itemDOs) {
        List<SaleOrderItem> items = new ArrayList<>();
        if (itemDOs != null) {
            for (ErpSaleOrderItemDO itemDO : itemDOs) {
                items.add(new SaleOrderItem(
                        itemDO.getId(),
                        itemDO.getProductId(),
                        itemDO.getProductUnitId(),
                        itemDO.getCount(),
                        itemDO.getProductPrice(),
                        itemDO.getTaxPercent(),
                        itemDO.getTotalPrice(),
                        itemDO.getTaxPrice(),
                        itemDO.getOutCount(),
                        itemDO.getReturnCount(),
                        itemDO.getRemark()));
            }
        }
        return ErpSaleOrderAggregate.reconstitute(
                orderDO.getId(),
                orderDO.getNo(),
                SaleOrderStatus.of(orderDO.getStatus()),
                orderDO.getCustomerId(),
                orderDO.getAccountId(),
                orderDO.getSaleUserId(),
                orderDO.getOrderTime(),
                items,
                orderDO.getDiscountPercent(),
                orderDO.getDepositPrice(),
                orderDO.getCurrencyId(),
                orderDO.getExchangeRate(),
                orderDO.getOutCount(),
                orderDO.getReturnCount(),
                orderDO.getFileUrl(),
                orderDO.getRemark());
    }
}
