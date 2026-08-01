package cn.iocoder.yudao.module.crm.service.servicecloud;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.crm.controller.admin.servicecloud.vo.CrmWorkOrderPageReqVO;
import cn.iocoder.yudao.module.crm.controller.admin.servicecloud.vo.CrmWorkOrderSaveReqVO;
import jakarta.validation.Valid;

/**
 * CRM 售后工单 Service 接口
 *
 * @author dhb52
 */
public interface CrmWorkOrderService {

    /**
     * 创建售后工单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWorkOrder(@Valid CrmWorkOrderSaveReqVO createReqVO);

    /**
     * 更新售后工单
     *
     * @param updateReqVO 更新信息
     */
    void updateWorkOrder(@Valid CrmWorkOrderSaveReqVO updateReqVO);

    /**
     * 删除售后工单
     *
     * @param id 编号
     */
    void deleteWorkOrder(Long id);

    /**
     * 获得售后工单
     *
     * @param id 编号
     * @return 售后工单
     */
    CrmWorkOrderDO getWorkOrder(Long id);

    /**
     * 获得售后工单分页
     *
     * @param pageReqVO 分页查询
     * @return 售后工单分页
     */
    PageResult<CrmWorkOrderDO> getWorkOrderPage(CrmWorkOrderPageReqVO pageReqVO);

    /**
     * 分配工单
     *
     * @param id             编号
     * @param assigneeUserId 处理人
     */
    void assignWorkOrder(Long id, Long assigneeUserId);

    /**
     * 解决工单
     *
     * @param id         编号
     * @param resolution 解决方案
     */
    void resolveWorkOrder(Long id, String resolution);

    /**
     * 关闭工单
     *
     * @param id 编号
     */
    void closeWorkOrder(Long id);

}
