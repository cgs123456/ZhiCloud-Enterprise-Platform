package cn.iocoder.yudao.module.crm.service.salesorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.crm.controller.admin.salesorder.vo.CrmSaleOrderPageReqVO;
import cn.iocoder.yudao.module.crm.controller.admin.salesorder.vo.CrmSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.salesorder.CrmSaleOrderDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.salesorder.CrmSaleOrderItemDO;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * CRM 销售订单 Service 接口
 *
 * @author dhb52
 */
public interface CrmSaleOrderService {

    /**
     * 创建销售订单
     *
     * @param createReqVO 创建信息
     * @param userId      用户编号
     * @return 编号
     */
    Long createSaleOrder(@Valid CrmSaleOrderSaveReqVO createReqVO, Long userId);

    /**
     * 更新销售订单
     *
     * @param updateReqVO 更新信息
     */
    void updateSaleOrder(@Valid CrmSaleOrderSaveReqVO updateReqVO);

    /**
     * 删除销售订单
     *
     * @param id 编号
     */
    void deleteSaleOrder(Long id);

    /**
     * 获得销售订单
     *
     * @param id 编号
     * @return 销售订单
     */
    CrmSaleOrderDO getSaleOrder(Long id);

    /**
     * 校验销售订单是否合法
     *
     * @param id 编号
     * @return 销售订单
     */
    CrmSaleOrderDO validateSaleOrder(Long id);

    /**
     * 获得销售订单分页
     *
     * @param pageReqVO 分页查询
     * @return 销售订单分页
     */
    PageResult<CrmSaleOrderDO> getSaleOrderPage(CrmSaleOrderPageReqVO pageReqVO);

    /**
     * 确认销售订单
     *
     * @param id 编号
     */
    void confirmSaleOrder(Long id);

    /**
     * 提交销售订单审批
     *
     * @param id     编号
     * @param userId 用户编号
     */
    void submitSaleOrder(Long id, Long userId);

    /**
     * 获得销售订单分页，基于指定合同
     *
     * @param pageReqVO 分页查询
     * @return 销售订单分页
     */
    PageResult<CrmSaleOrderDO> getSaleOrderPageByContractId(CrmSaleOrderPageReqVO pageReqVO);

    /**
     * 获得销售订单分页，基于指定客户
     *
     * @param pageReqVO 分页查询
     * @return 销售订单分页
     */
    PageResult<CrmSaleOrderDO> getSaleOrderPageByCustomerId(CrmSaleOrderPageReqVO pageReqVO);

    /**
     * 根据订单编号，获得订单明细列表
     *
     * @param orderId 订单编号
     * @return 订单明细列表
     */
    List<CrmSaleOrderItemDO> getSaleOrderItemListByOrderId(Long orderId);

    /**
     * 根据合同编号，获得已下单金额汇总 Map
     *
     * @param contractIds 合同编号集合
     * @return Map<合同编号, 已下单金额>
     */
    Map<Long, BigDecimal> getSaleOrderPriceMapByContractId(Collection<Long> contractIds);

}
