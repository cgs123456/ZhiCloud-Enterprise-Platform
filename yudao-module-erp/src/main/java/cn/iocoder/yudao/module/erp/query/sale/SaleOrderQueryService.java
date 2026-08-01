package cn.iocoder.yudao.module.erp.query.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;

import java.time.LocalDate;

/**
 * 销售订单查询服务（CQRS 读模型）
 *
 * <p>读模型查询服务，与写模型（{@code ErpSaleOrderService}）分离，专注只读查询与统计聚合。
 * 直接复用现有 Mapper，不新建读模型表，仅是查询角度的分离（CQRS 的轻量化落地）。
 *
 * <p>读模型不复用写模型的 DO 直接返回，而是投影为独立的查询 VO，避免读侧变更影响写侧契约。
 *
 * @author DDD 试点
 */
public interface SaleOrderQueryService {

    /**
     * 分页查询销售订单摘要
     *
     * @param pageReqVO 分页查询条件
     * @return 摘要分页结果
     */
    PageResult<SaleOrderSummaryVO> querySummary(ErpSaleOrderPageReqVO pageReqVO);

    /**
     * 查询销售订单详情（含明细）
     *
     * @param orderId 订单编号
     * @return 详情 VO，不存在时返回 null
     */
    SaleOrderDetailVO queryDetail(Long orderId);

    /**
     * 统计查询（按状态、客户维度聚合）
     *
     * @param start 起始日期（含），可为空
     * @param end   截止日期（含），可为空
     * @return 统计 VO
     */
    SaleOrderStatisticsVO queryStatistics(LocalDate start, LocalDate end);

}
