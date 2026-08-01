package cn.iocoder.yudao.module.erp.service.finance.cost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.actualcost.ErpActualCostPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.actualcost.ErpActualCostSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpActualCostDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 实际成本 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpActualCostService {

    /**
     * 创建实际成本
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createActualCost(@Valid ErpActualCostSaveReqVO createReqVO);

    /**
     * 更新实际成本
     *
     * @param updateReqVO 更新信息
     */
    void updateActualCost(@Valid ErpActualCostSaveReqVO updateReqVO);

    /**
     * 删除实际成本
     *
     * @param id 编号
     */
    void deleteActualCost(Long id);

    /**
     * 获得实际成本
     *
     * @param id 编号
     * @return 实际成本
     */
    ErpActualCostDO getActualCost(Long id);

    /**
     * 获得实际成本分页
     *
     * @param pageReqVO 分页查询
     * @return 实际成本分页
     */
    PageResult<ErpActualCostDO> getActualCostPage(ErpActualCostPageReqVO pageReqVO);

    /**
     * 获得产品在某期间的实际成本列表
     *
     * @param productId 产品 ID
     * @param costPeriod 成本期间
     * @return 实际成本列表
     */
    List<ErpActualCostDO> getActualCostListByProductAndPeriod(Long productId, String costPeriod);

}
