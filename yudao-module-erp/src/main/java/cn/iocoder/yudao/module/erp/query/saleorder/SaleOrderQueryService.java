package cn.iocoder.yudao.module.erp.query.saleorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;

/**
 * 销售订单查询服务（CQRS 试点读侧）
 *
 * <p>读模型查询服务，与写模型（{@code ErpSaleOrderService}）分离，
 * 专注只读查询。读模型投影为独立的 {@link SaleOrderView}，
 * 包含客户名称、销售员名称等冗余字段，专为查询优化。
 *
 * <p>本接口与 {@code query.sale.SaleOrderQueryService} 并存，
 * 互不影响。原有读方法保留，本接口为新增的读侧实现。
 *
 * @author DDD 试点
 */
public interface SaleOrderQueryService {

    /**
     * 获取销售订单详情视图（含明细）
     *
     * @param id 订单编号
     * @return 订单视图，不存在时返回 null
     */
    SaleOrderView getSaleOrderView(Long id);

    /**
     * 分页查询销售订单视图
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<SaleOrderView> pageQuery(SaleOrderPageQuery query);
}
