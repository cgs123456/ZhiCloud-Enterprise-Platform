package cn.iocoder.yudao.module.erp.service.finance.cost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costvariance.ErpCostVariancePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costvariance.ErpCostVarianceSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpCostVarianceDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.cost.ErpCostVarianceMapper;
import cn.iocoder.yudao.module.erp.enums.finance.cost.ErpVarianceTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.COST_VARIANCE_NOT_EXISTS;

/**
 * ERP 成本差异 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpCostVarianceServiceImpl implements ErpCostVarianceService {

    @Resource
    private ErpCostVarianceMapper costVarianceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCostVariance(ErpCostVarianceSaveReqVO createReqVO) {
        ErpCostVarianceDO costVariance = BeanUtils.toBean(createReqVO, ErpCostVarianceDO.class);
        calculateVariance(costVariance);
        costVarianceMapper.insert(costVariance);
        return costVariance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCostVariance(ErpCostVarianceSaveReqVO updateReqVO) {
        // 校验存在
        validateCostVarianceExists(updateReqVO.getId());
        // 更新
        ErpCostVarianceDO updateObj = BeanUtils.toBean(updateReqVO, ErpCostVarianceDO.class);
        calculateVariance(updateObj);
        costVarianceMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCostVariance(Long id) {
        // 校验存在
        validateCostVarianceExists(id);
        // 删除
        costVarianceMapper.deleteById(id);
    }

    @Override
    public ErpCostVarianceDO getCostVariance(Long id) {
        return costVarianceMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCostVarianceDO> getCostVariancePage(ErpCostVariancePageReqVO pageReqVO) {
        return costVarianceMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCostVarianceDO> getCostVarianceListByProductAndPeriod(Long productId, String costPeriod) {
        return costVarianceMapper.selectListByProductAndPeriod(productId, costPeriod);
    }

    private void validateCostVarianceExists(Long id) {
        if (costVarianceMapper.selectById(id) == null) {
            throw exception(COST_VARIANCE_NOT_EXISTS);
        }
    }

    /**
     * 计算差异金额、差异率与差异类型
     *
     * <p>差异金额 = 实际成本 - 标准成本
     * <p>差异率(%) = 差异金额 / 标准成本 * 100
     * <p>差异金额 &lt; 0 → 有利差异（FAVORABLE）；&gt; 0 → 不利差异（UNFAVORABLE）
     */
    private void calculateVariance(ErpCostVarianceDO variance) {
        BigDecimal standard = variance.getStandardCost() == null ? BigDecimal.ZERO : variance.getStandardCost();
        BigDecimal actual = variance.getActualCost() == null ? BigDecimal.ZERO : variance.getActualCost();
        BigDecimal amount = actual.subtract(standard);
        variance.setVarianceAmount(amount);
        if (standard.compareTo(BigDecimal.ZERO) != 0) {
            variance.setVarianceRate(amount
                    .divide(standard, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            variance.setVarianceType(ErpVarianceTypeEnum.FAVORABLE.getType());
        } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
            variance.setVarianceType(ErpVarianceTypeEnum.UNFAVORABLE.getType());
        }
    }

}
