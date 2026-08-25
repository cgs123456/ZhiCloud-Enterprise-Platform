package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fundplan.ErpFundPlanPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fundplan.ErpFundPlanSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFundPlanDO;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

/**
 * ERP 资金计划 Service 接口（P0-3 资金管理）
 *
 * @author 智云
 */
public interface ErpFundPlanService {

    /**
     * 创建资金计划
     */
    Long createFundPlan(@Valid ErpFundPlanSaveReqVO createReqVO);

    /**
     * 更新资金计划
     */
    void updateFundPlan(@Valid ErpFundPlanSaveReqVO updateReqVO);

    /**
     * 删除资金计划
     */
    void deleteFundPlan(Long id);

    /**
     * 获得资金计划
     */
    ErpFundPlanDO getFundPlan(Long id);

    /**
     * 获得资金计划分页
     */
    PageResult<ErpFundPlanDO> getFundPlanPage(ErpFundPlanPageReqVO pageReqVO);

    /**
     * 按期间获取资金计划列表
     */
    List<ErpFundPlanDO> getFundPlanListByPeriod(String planPeriod);

    /**
     * 按期间汇总（收款-付款净额，或返回合计）
     *
     * @param planPeriod 计划期间
     * @return 净额（收款合计 - 付款合计）
     */
    BigDecimal sumByPeriod(String planPeriod);

}