package cn.iocoder.yudao.module.erp.service.finance.cost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.workordercost.ErpWorkOrderCostPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.workordercost.ErpWorkOrderCostSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpWorkOrderCostDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 工单成本归集 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpWorkOrderCostService {

    /**
     * 创建工单成本归集
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWorkOrderCost(@Valid ErpWorkOrderCostSaveReqVO createReqVO);

    /**
     * 更新工单成本归集
     *
     * @param updateReqVO 更新信息
     */
    void updateWorkOrderCost(@Valid ErpWorkOrderCostSaveReqVO updateReqVO);

    /**
     * 删除工单成本归集
     *
     * @param id 编号
     */
    void deleteWorkOrderCost(Long id);

    /**
     * 获得工单成本归集
     *
     * @param id 编号
     * @return 工单成本归集
     */
    ErpWorkOrderCostDO getWorkOrderCost(Long id);

    /**
     * 根据工单 ID 获得工单成本归集
     *
     * @param workOrderId 工单 ID
     * @return 工单成本归集
     */
    ErpWorkOrderCostDO getWorkOrderCostByWorkOrderId(Long workOrderId);

    /**
     * 获得工单成本归集分页
     *
     * @param pageReqVO 分页查询
     * @return 工单成本归集分页
     */
    PageResult<ErpWorkOrderCostDO> getWorkOrderCostPage(ErpWorkOrderCostPageReqVO pageReqVO);

    /**
     * 获得产品在某期间的工单成本归集列表
     *
     * @param productId 产品 ID
     * @param costPeriod 成本期间
     * @return 工单成本归集列表
     */
    List<ErpWorkOrderCostDO> getWorkOrderCostListByProductAndPeriod(Long productId, String costPeriod);

}
