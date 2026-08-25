package cn.zhicloud.module.erp.service.finance.cost;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.actualcost.ErpActualCostPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.actualcost.ErpActualCostSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cost.ErpActualCostDO;
import cn.zhicloud.module.erp.dal.mysql.finance.cost.ErpActualCostMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.ACTUAL_COST_NOT_EXISTS;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.ACTUAL_COST_PERIOD_DUPLICATE;

/**
 * ERP 实际成本 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpActualCostServiceImpl implements ErpActualCostService {

    @Resource
    private ErpActualCostMapper actualCostMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createActualCost(ErpActualCostSaveReqVO createReqVO) {
        // 校验同期间同项目不存在
        validatePeriodUnique(null, createReqVO.getProductId(), createReqVO.getCostPeriod(), createReqVO.getCostItemId());
        // 计算单位成本
        ErpActualCostDO actualCost = BeanUtils.toBean(createReqVO, ErpActualCostDO.class);
        calculateUnitCost(actualCost);
        actualCostMapper.insert(actualCost);
        return actualCost.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActualCost(ErpActualCostSaveReqVO updateReqVO) {
        // 校验存在
        validateActualCostExists(updateReqVO.getId());
        // 校验同期间同项目不存在
        validatePeriodUnique(updateReqVO.getId(), updateReqVO.getProductId(), updateReqVO.getCostPeriod(), updateReqVO.getCostItemId());
        // 更新
        ErpActualCostDO updateObj = BeanUtils.toBean(updateReqVO, ErpActualCostDO.class);
        calculateUnitCost(updateObj);
        actualCostMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActualCost(Long id) {
        // 校验存在
        validateActualCostExists(id);
        // 删除
        actualCostMapper.deleteById(id);
    }

    @Override
    public ErpActualCostDO getActualCost(Long id) {
        return actualCostMapper.selectById(id);
    }

    @Override
    public PageResult<ErpActualCostDO> getActualCostPage(ErpActualCostPageReqVO pageReqVO) {
        return actualCostMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpActualCostDO> getActualCostListByProductAndPeriod(Long productId, String costPeriod) {
        return actualCostMapper.selectListByProductAndPeriod(productId, costPeriod);
    }

    private void validateActualCostExists(Long id) {
        if (actualCostMapper.selectById(id) == null) {
            throw exception(ACTUAL_COST_NOT_EXISTS);
        }
    }

    private void validatePeriodUnique(Long id, Long productId, String costPeriod, Long costItemId) {
        ErpActualCostDO existing = actualCostMapper.selectByProductAndPeriod(productId, costPeriod, costItemId);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(ACTUAL_COST_PERIOD_DUPLICATE, productId, costPeriod);
        }
    }

    /**
     * 计算单位成本：实际成本总额 / 实际产量
     */
    private void calculateUnitCost(ErpActualCostDO actualCost) {
        if (actualCost.getActualCost() != null && actualCost.getActualQuantity() != null
                && actualCost.getActualQuantity().compareTo(BigDecimal.ZERO) != 0) {
            actualCost.setUnitCost(actualCost.getActualCost()
                    .divide(actualCost.getActualQuantity(), 4, RoundingMode.HALF_UP));
        }
    }

}
