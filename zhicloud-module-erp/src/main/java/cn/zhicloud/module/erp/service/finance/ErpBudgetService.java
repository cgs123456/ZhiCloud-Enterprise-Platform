package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.budget.ErpBudgetPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.budget.ErpBudgetSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpBudgetDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpBudgetDetailDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 预算 Service 接口（P0-14）
 *
 * <p>提供预算 CRUD + 审批 + 明细查询。
 *
 * @author 智云
 */
public interface ErpBudgetService {

    /**
     * 创建预算
     *
     * <p>校验期间唯一性、明细金额合计 = 总额，初始化状态为草稿。
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createBudget(@Valid ErpBudgetSaveReqVO createReqVO);

    /**
     * 更新预算
     *
     * <p>仅草稿状态可修改。
     *
     * @param updateReqVO 更新信息
     */
    void updateBudget(@Valid ErpBudgetSaveReqVO updateReqVO);

    /**
     * 删除预算
     *
     * <p>仅草稿可删，级联删除明细。
     *
     * @param id 编号
     */
    void deleteBudget(Long id);

    /**
     * 获取预算
     *
     * @param id 编号
     * @return 预算
     */
    ErpBudgetDO getBudget(Long id);

    /**
     * 分页查询预算
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<ErpBudgetDO> getBudgetPage(ErpBudgetPageReqVO pageReqVO);

    /**
     * 审批预算
     *
     * <p>草稿 → 已审批。
     *
     * @param id 编号
     */
    void approveBudget(Long id);

    /**
     * 获取预算明细列表
     *
     * @param budgetId 预算编号
     * @return 明细列表
     */
    List<ErpBudgetDetailDO> getBudgetDetailListByBudgetId(Long budgetId);

}
