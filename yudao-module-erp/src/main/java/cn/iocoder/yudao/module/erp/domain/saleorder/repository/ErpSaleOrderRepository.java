package cn.iocoder.yudao.module.erp.domain.saleorder.repository;

import cn.iocoder.yudao.module.erp.domain.saleorder.ErpSaleOrderAggregate;

import java.util.List;

/**
 * 销售订单聚合根仓储接口（DDD 试点 Port）
 *
 * <p>领域层定义的仓储端口，屏蔽持久化细节。领域层只依赖此接口，
 * 具体实现（{@code ErpSaleOrderRepositoryImpl}）在基础设施层，
 * 内部复用现有的 {@code ErpSaleOrderMapper} / {@code ErpSaleOrderItemMapper}。
 *
 * <p>仓储负责聚合根的完整持久化：订单头 + 明细列表，保证聚合内数据一致性。
 *
 * @author DDD 试点
 */
public interface ErpSaleOrderRepository {

    /**
     * 保存聚合根（新增或更新）
     *
     * <p>若聚合根 id 为空则新增，否则更新。同时保存订单头和明细列表。
     * 持久化后拉取并发布聚合根的领域事件。
     *
     * @param aggregate 聚合根
     */
    void save(ErpSaleOrderAggregate aggregate);

    /**
     * 根据编号查找聚合根
     *
     * @param id 订单编号
     * @return 聚合根实例，不存在时返回 null
     */
    ErpSaleOrderAggregate findById(Long id);

    /**
     * 根据编号批量查找聚合根
     *
     * @param ids 订单编号列表
     * @return 聚合根列表
     */
    List<ErpSaleOrderAggregate> findByIds(List<Long> ids);
}
