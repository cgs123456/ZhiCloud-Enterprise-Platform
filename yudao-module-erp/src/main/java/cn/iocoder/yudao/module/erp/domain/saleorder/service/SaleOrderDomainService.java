package cn.iocoder.yudao.module.erp.domain.saleorder.service;

import cn.iocoder.yudao.module.erp.domain.saleorder.ErpSaleOrderAggregate;
import cn.iocoder.yudao.module.erp.domain.saleorder.repository.ErpSaleOrderRepository;
import cn.iocoder.yudao.module.erp.domain.saleorder.vo.UpdateSaleOrderOutCountCommand;
import cn.iocoder.yudao.module.erp.domain.saleorder.vo.UpdateSaleOrderReturnCountCommand;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_ORDER_NOT_EXISTS;

/**
 * 销售订单领域服务（DDD 试点）
 *
 * <p>封装跨聚合的领域逻辑。纯粹的订单内逻辑放在 {@link ErpSaleOrderAggregate} 中，
 * 涉及聚合根加载 → 调用聚合根方法 → 持久化 → 事件发布的编排逻辑放在本服务中。
 *
 * <p>与现有 {@code ErpSaleOrderService} 并存：
 * <ul>
 *     <li>现有 Service 直接操作 DO / Mapper，业务规则散落</li>
 *     <li>本领域服务通过聚合根操作，业务规则内聚于聚合根</li>
 * </ul>
 *
 * <p>新功能优先使用本服务，老代码保留不动。
 *
 * @author DDD 试点
 */
@Service
public class SaleOrderDomainService {

    @Resource
    private ErpSaleOrderRepository repository;

    /**
     * 审批销售订单
     *
     * <p>流程：加载聚合根 → {@code aggregate.audit()} → 持久化（事件自动发布）
     *
     * @param orderId 订单编号
     * @throws IllegalStateException 状态不满足审批条件
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditOrder(Long orderId) {
        ErpSaleOrderAggregate aggregate = loadAggregate(orderId);
        aggregate.audit();
        repository.save(aggregate);
    }

    /**
     * 更新出库数量（由出库单完成时触发）
     *
     * <p>流程：加载聚合根 → {@code aggregate.updateOutCount(delta)} → 持久化
     *
     * @param cmd 出库数量更新命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOutCount(UpdateSaleOrderOutCountCommand cmd) {
        ErpSaleOrderAggregate aggregate = loadAggregate(cmd.getOrderId());
        aggregate.updateOutCount(cmd.getDelta());
        repository.save(aggregate);
    }

    /**
     * 更新退货数量（由退货单完成时触发）
     *
     * <p>流程：加载聚合根 → {@code aggregate.updateReturnCount(delta)} → 持久化
     *
     * @param cmd 退货数量更新命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateReturnCount(UpdateSaleOrderReturnCountCommand cmd) {
        ErpSaleOrderAggregate aggregate = loadAggregate(cmd.getOrderId());
        aggregate.updateReturnCount(cmd.getDelta());
        repository.save(aggregate);
    }

    /**
     * 取消订单
     *
     * @param orderId 订单编号
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        ErpSaleOrderAggregate aggregate = loadAggregate(orderId);
        aggregate.cancel();
        repository.save(aggregate);
    }

    // ==================== 内部工具 ====================

    /**
     * 加载聚合根，不存在则抛业务异常
     */
    private ErpSaleOrderAggregate loadAggregate(Long orderId) {
        ErpSaleOrderAggregate aggregate = repository.findById(orderId);
        if (aggregate == null) {
            throw exception(SALE_ORDER_NOT_EXISTS);
        }
        return aggregate;
    }
}
