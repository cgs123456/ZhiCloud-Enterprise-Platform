package cn.zhicloud.module.erp.service.finance.cost;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.workordercost.ErpWorkOrderCostPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.workordercost.ErpWorkOrderCostSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpWorkOrderCostDO;
import cn.zhicloud.module.erp.dal.mysql.finance.cost.ErpWorkOrderCostMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.WORK_ORDER_COST_NOT_EXISTS;

/**
 * ERP 工单成本归集 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpWorkOrderCostServiceImpl implements ErpWorkOrderCostService {

    @Resource
    private ErpWorkOrderCostMapper workOrderCostMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrderCost(ErpWorkOrderCostSaveReqVO createReqVO) {
        ErpWorkOrderCostDO workOrderCost = BeanUtils.toBean(createReqVO, ErpWorkOrderCostDO.class);
        calculateTotals(workOrderCost);
        workOrderCostMapper.insert(workOrderCost);
        return workOrderCost.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrderCost(ErpWorkOrderCostSaveReqVO updateReqVO) {
        // 校验存在
        validateWorkOrderCostExists(updateReqVO.getId());
        // 更新
        ErpWorkOrderCostDO updateObj = BeanUtils.toBean(updateReqVO, ErpWorkOrderCostDO.class);
        calculateTotals(updateObj);
        workOrderCostMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrderCost(Long id) {
        // 校验存在
        validateWorkOrderCostExists(id);
        // 删除
        workOrderCostMapper.deleteById(id);
    }

    @Override
    public ErpWorkOrderCostDO getWorkOrderCost(Long id) {
        return workOrderCostMapper.selectById(id);
    }

    @Override
    public ErpWorkOrderCostDO getWorkOrderCostByWorkOrderId(Long workOrderId) {
        return workOrderCostMapper.selectByWorkOrderId(workOrderId);
    }

    @Override
    public PageResult<ErpWorkOrderCostDO> getWorkOrderCostPage(ErpWorkOrderCostPageReqVO pageReqVO) {
        return workOrderCostMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpWorkOrderCostDO> getWorkOrderCostListByProductAndPeriod(Long productId, String costPeriod) {
        return workOrderCostMapper.selectListByProductAndPeriod(productId, costPeriod);
    }

    private void validateWorkOrderCostExists(Long id) {
        if (workOrderCostMapper.selectById(id) == null) {
            throw exception(WORK_ORDER_COST_NOT_EXISTS);
        }
    }

    /**
     * 计算总成本和单位成本
     *
     * <p>总成本 = 材料成本 + 人工成本 + 制造费用 + 外协成本
     * <p>单位成本 = 总成本 / 工单产量
     */
    private void calculateTotals(ErpWorkOrderCostDO workOrderCost) {
        BigDecimal material = workOrderCost.getMaterialCost() == null ? BigDecimal.ZERO : workOrderCost.getMaterialCost();
        BigDecimal labor = workOrderCost.getLaborCost() == null ? BigDecimal.ZERO : workOrderCost.getLaborCost();
        BigDecimal overhead = workOrderCost.getOverheadCost() == null ? BigDecimal.ZERO : workOrderCost.getOverheadCost();
        BigDecimal outsourcing = workOrderCost.getOutsourcingCost() == null ? BigDecimal.ZERO : workOrderCost.getOutsourcingCost();
        BigDecimal total = material.add(labor).add(overhead).add(outsourcing);
        workOrderCost.setTotalCost(total);
        if (workOrderCost.getQuantity() != null && workOrderCost.getQuantity().compareTo(BigDecimal.ZERO) != 0) {
            workOrderCost.setUnitCost(total.divide(workOrderCost.getQuantity(), 4, RoundingMode.HALF_UP));
        }
    }

}
