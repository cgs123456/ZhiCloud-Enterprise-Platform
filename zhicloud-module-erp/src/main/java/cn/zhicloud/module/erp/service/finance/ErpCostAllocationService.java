package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costallocation.ErpCostAllocationPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.costallocation.ErpCostAllocationSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCostAllocationDO;
import jakarta.validation.Valid;

import java.time.LocalDate;

/**
 * ERP 成本分摊 Service 接口
 *
 * @author 智云
 */
public interface ErpCostAllocationService {

    /**
     * 创建成本分摊记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCostAllocation(@Valid ErpCostAllocationSaveReqVO createReqVO);

    /**
     * 更新成本分摊记录
     *
     * @param updateReqVO 更新信息
     */
    void updateCostAllocation(@Valid ErpCostAllocationSaveReqVO updateReqVO);

    /**
     * 删除成本分摊记录
     *
     * @param id 编号
     */
    void deleteCostAllocation(Long id);

    /**
     * 获得成本分摊记录
     *
     * @param id 编号
     * @return 成本分摊记录
     */
    ErpCostAllocationDO getCostAllocation(Long id);

    /**
     * 获得成本分摊分页
     *
     * @param pageReqVO 分页查询
     * @return 成本分摊分页
     */
    PageResult<ErpCostAllocationDO> getCostAllocationPage(ErpCostAllocationPageReqVO pageReqVO);

    /**
     * 执行成本分摊
     *
     * <p>从源成本中心向目标成本中心分摊指定金额，并生成一条分摊记录（手工类型）。
     *
     * @param costCenterId 源成本中心 ID
     * @param targetId     目标成本中心 ID
     * @param amount       分摊金额
     * @param date         分摊日期
     * @return 分摊记录编号
     */
    Long allocateCost(Long costCenterId, Long targetId, java.math.BigDecimal amount, LocalDate date);

}
