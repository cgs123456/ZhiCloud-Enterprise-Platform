package cn.iocoder.yudao.module.erp.service.finance.cost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.standardcost.ErpStandardCostPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.standardcost.ErpStandardCostSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpStandardCostDO;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 标准成本 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpStandardCostService {

    /**
     * 创建标准成本
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStandardCost(@Valid ErpStandardCostSaveReqVO createReqVO);

    /**
     * 更新标准成本
     *
     * @param updateReqVO 更新信息
     */
    void updateStandardCost(@Valid ErpStandardCostSaveReqVO updateReqVO);

    /**
     * 删除标准成本
     *
     * @param id 编号
     */
    void deleteStandardCost(Long id);

    /**
     * 获得标准成本
     *
     * @param id 编号
     * @return 标准成本
     */
    ErpStandardCostDO getStandardCost(Long id);

    /**
     * 获得标准成本分页
     *
     * @param pageReqVO 分页查询
     * @return 标准成本分页
     */
    PageResult<ErpStandardCostDO> getStandardCostPage(ErpStandardCostPageReqVO pageReqVO);

    /**
     * 获得产品在某天生效的标准成本列表
     *
     * @param productId 产品 ID
     * @param date 日期
     * @return 标准成本列表
     */
    List<ErpStandardCostDO> getEffectiveStandardCostList(Long productId, LocalDate date);

}
