package cn.zhicloud.module.erp.service.finance.cost;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costvariance.ErpCostVariancePageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costvariance.ErpCostVarianceSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpCostVarianceDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 成本差异 Service 接口
 *
 * @author 智云
 */
public interface ErpCostVarianceService {

    /**
     * 创建成本差异
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCostVariance(@Valid ErpCostVarianceSaveReqVO createReqVO);

    /**
     * 更新成本差异
     *
     * @param updateReqVO 更新信息
     */
    void updateCostVariance(@Valid ErpCostVarianceSaveReqVO updateReqVO);

    /**
     * 删除成本差异
     *
     * @param id 编号
     */
    void deleteCostVariance(Long id);

    /**
     * 获得成本差异
     *
     * @param id 编号
     * @return 成本差异
     */
    ErpCostVarianceDO getCostVariance(Long id);

    /**
     * 获得成本差异分页
     *
     * @param pageReqVO 分页查询
     * @return 成本差异分页
     */
    PageResult<ErpCostVarianceDO> getCostVariancePage(ErpCostVariancePageReqVO pageReqVO);

    /**
     * 获得产品在某期间的成本差异列表
     *
     * @param productId 产品 ID
     * @param costPeriod 成本期间
     * @return 成本差异列表
     */
    List<ErpCostVarianceDO> getCostVarianceListByProductAndPeriod(Long productId, String costPeriod);

}
