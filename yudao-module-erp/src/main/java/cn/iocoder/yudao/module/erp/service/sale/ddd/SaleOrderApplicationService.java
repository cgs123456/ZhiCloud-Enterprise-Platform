package cn.iocoder.yudao.module.erp.service.sale.ddd;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 销售订单 DDD 应用服务（试点）
 *
 * <p>DDD 风格的应用服务，与现有 {@code ErpSaleOrderService} 并存。
 * 通过聚合根 + 领域事件的方式操作销售订单，展示 DDD 的核心价值：
 * <ul>
 *     <li>业务规则内聚到聚合根（{@link cn.iocoder.yudao.module.erp.domain.sale.aggregate.SaleOrderAggregate}），
 *         应用服务只负责编排：加载 → 重建聚合根 → 调用聚合根方法 → 持久化 → 发布事件</li>
 *     <li>跨模块通过领域事件解耦（销售 → 库存），而非直接调用</li>
 * </ul>
 *
 * <p>注：本接口为试点，不替代现有 {@code ErpSaleOrderService}，仅用于验证 DDD 模式在本项目的可行性。
 *
 * @author DDD 试点
 */
public interface SaleOrderApplicationService {

    /**
     * DDD 风格审核确认订单
     *
     * <p>流程：加载订单 → 重建聚合根 → {@code aggregate.confirm()} → 持久化 → 发布确认事件
     *
     * @param orderId 订单编号
     */
    void confirmOrder(Long orderId);

    /**
     * DDD 风格出库
     *
     * <p>流程：加载订单 → 重建聚合根 → {@code aggregate.ship(quantities)} → 持久化 → 发布出库事件
     *
     * @param orderId     订单编号
     * @param outCountMap 出库明细：key = 订单明细编号（itemId），value = 该明细累计出库数量
     */
    void shipOrder(Long orderId, Map<Long, BigDecimal> outCountMap);

    /**
     * DDD 风格取消订单
     *
     * <p>流程：加载订单 → 重建聚合根 → {@code aggregate.cancel()} → 持久化 → 发布取消事件
     *
     * @param orderId 订单编号
     * @param reason  取消原因
     */
    void cancelOrder(Long orderId, String reason);

}
