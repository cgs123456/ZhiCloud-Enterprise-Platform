package cn.iocoder.yudao.module.erp.service.sale.ddd;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.domain.sale.aggregate.OrderItem;
import cn.iocoder.yudao.module.erp.domain.sale.aggregate.SaleOrderAggregate;
import cn.iocoder.yudao.module.erp.domain.sale.event.SaleOrderEventPublisher;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_ORDER_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_ORDER_NOT_EXISTS;

/**
 * 销售订单 DDD 应用服务实现（试点）
 *
 * <p>编排领域模型与持久化、事件发布。业务规则全部委托给 {@link SaleOrderAggregate}，
 * 本类不直接操作 DO 的业务字段，仅做加载 / 重建 / 持久化 / 发事件。
 *
 * <p>与现有 {@code ErpSaleOrderServiceImpl} 并存，互不影响。
 *
 * @author DDD 试点
 */
@Service
@Validated
public class SaleOrderApplicationServiceImpl implements SaleOrderApplicationService {

    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Resource
    private ErpProductService productService;
    @Resource
    private SaleOrderEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long orderId) {
        // 1. 加载订单与明细
        ErpSaleOrderDO order = loadOrder(orderId);
        List<ErpSaleOrderItemDO> itemDOs = saleOrderItemMapper.selectListByOrderId(orderId);
        // 2. 重建聚合根（业务规则内聚于聚合根，应用服务不感知状态流转细节）
        SaleOrderAggregate aggregate = reconstitute(order, itemDOs);
        // 3. 调用聚合根方法完成状态变更（不直接 set DO 字段）
        aggregate.confirm();
        // 4. 持久化（乐观锁：仅当原状态为草稿时才更新为已确认）
        int updateCount = saleOrderMapper.updateByIdAndStatus(orderId, SaleOrderAggregate.STATUS_DRAFT,
                new ErpSaleOrderDO().setStatus(SaleOrderAggregate.STATUS_CONFIRMED));
        if (updateCount == 0) {
            throw exception(SALE_ORDER_APPROVE_FAIL);
        }
        // 5. 发布领域事件（事务提交后由监听器处理，解耦库存等模块）
        eventPublisher.publishConfirmed(aggregate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, Map<Long, BigDecimal> outCountMap) {
        // 1. 加载订单与明细
        ErpSaleOrderDO order = loadOrder(orderId);
        List<ErpSaleOrderItemDO> itemDOs = saleOrderItemMapper.selectListByOrderId(orderId);
        // 2. 重建聚合根
        SaleOrderAggregate aggregate = reconstitute(order, itemDOs);
        // 3. 构造出库数量数组（与 items 顺序一致；缺失项保持当前出库数量）
        BigDecimal[] quantities = new BigDecimal[itemDOs.size()];
        for (int i = 0; i < itemDOs.size(); i++) {
            ErpSaleOrderItemDO item = itemDOs.get(i);
            quantities[i] = outCountMap.getOrDefault(item.getId(), item.getOutCount());
        }
        // 4. 调用聚合根方法（校验数量、更新出库状态，规则内聚于聚合根）
        aggregate.ship(quantities);
        // 5. 持久化：按聚合根变更后的明细出库数量回写
        for (OrderItem item : aggregate.getItems()) {
            saleOrderItemMapper.updateById(
                    new ErpSaleOrderItemDO().setId(item.itemId()).setOutCount(item.outCount()));
        }
        saleOrderMapper.updateById(new ErpSaleOrderDO()
                .setId(orderId).setOutCount(aggregate.getTotalOutCount()));
        // 6. 发布出库事件
        eventPublisher.publishShipped(aggregate, outCountMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, String reason) {
        // 1. 加载订单与明细
        ErpSaleOrderDO order = loadOrder(orderId);
        List<ErpSaleOrderItemDO> itemDOs = saleOrderItemMapper.selectListByOrderId(orderId);
        // 2. 重建聚合根
        SaleOrderAggregate aggregate = reconstitute(order, itemDOs);
        // 3. 调用聚合根方法（校验是否可取消，已出库则拒绝）
        aggregate.cancel();
        // 4. 持久化（注：STATUS_CANCELLED 为领域层扩展状态，演示 DDD 对业务概念的演进）
        saleOrderMapper.updateById(new ErpSaleOrderDO()
                .setId(orderId).setStatus(SaleOrderAggregate.STATUS_CANCELLED));
        // 5. 发布取消事件
        eventPublisher.publishCancelled(aggregate, reason);
    }

    // ==================== 内部工具 ====================

    /**
     * 加载订单，不存在则抛业务异常
     */
    private ErpSaleOrderDO loadOrder(Long orderId) {
        ErpSaleOrderDO order = saleOrderMapper.selectById(orderId);
        if (order == null) {
            throw exception(SALE_ORDER_NOT_EXISTS);
        }
        return order;
    }

    /**
     * 从持久化 DO 重建聚合根
     *
     * <p>应用服务负责装配聚合根：组装产品名称、租户编号等基础设施关注点，
     * 交给聚合根后由其内聚业务规则。
     */
    private SaleOrderAggregate reconstitute(ErpSaleOrderDO order, List<ErpSaleOrderItemDO> itemDOs) {
        Set<Long> productIds = convertSet(itemDOs, ErpSaleOrderItemDO::getProductId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(productIds);
        List<OrderItem> items = convertList(itemDOs, item -> {
            ErpProductRespVO product = productMap.get(item.getProductId());
            return new OrderItem(item.getId(), item.getProductId(),
                    product == null ? null : product.getName(),
                    item.getCount(), item.getProductPrice(), item.getTotalPrice(),
                    item.getOutCount(), item.getReturnCount());
        });
        return SaleOrderAggregate.reconstitute(
                order.getId(), order.getNo(), order.getCustomerId(),
                order.getOrderTime(), order.getTotalPrice(), order.getStatus(),
                TenantContextHolder.getTenantId(), items);
    }

}
