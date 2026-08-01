package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fundplan.ErpFundPlanPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fundplan.ErpFundPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFundPlanDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFundPlanMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFundPlanTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FUND_PLAN_NOT_EXISTS;

/**
 * ERP 资金计划 Service 实现类（P0-3 资金管理）
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpFundPlanServiceImpl implements ErpFundPlanService {

    @Resource
    private ErpFundPlanMapper fundPlanMapper;

    @Override
    public Long createFundPlan(ErpFundPlanSaveReqVO createReqVO) {
        ErpFundPlanDO fundPlan = BeanUtils.toBean(createReqVO, ErpFundPlanDO.class);
        fundPlanMapper.insert(fundPlan);
        return fundPlan.getId();
    }

    @Override
    public void updateFundPlan(ErpFundPlanSaveReqVO updateReqVO) {
        validateFundPlanExists(updateReqVO.getId());
        ErpFundPlanDO updateObj = BeanUtils.toBean(updateReqVO, ErpFundPlanDO.class);
        fundPlanMapper.updateById(updateObj);
    }

    @Override
    public void deleteFundPlan(Long id) {
        validateFundPlanExists(id);
        fundPlanMapper.deleteById(id);
    }

    private void validateFundPlanExists(Long id) {
        if (fundPlanMapper.selectById(id) == null) {
            throw exception(FUND_PLAN_NOT_EXISTS);
        }
    }

    @Override
    public ErpFundPlanDO getFundPlan(Long id) {
        return fundPlanMapper.selectById(id);
    }

    @Override
    public PageResult<ErpFundPlanDO> getFundPlanPage(ErpFundPlanPageReqVO pageReqVO) {
        return fundPlanMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpFundPlanDO> getFundPlanListByPeriod(String planPeriod) {
        return fundPlanMapper.selectListByPlanPeriod(planPeriod);
    }

    @Override
    public BigDecimal sumByPeriod(String planPeriod) {
        List<ErpFundPlanDO> list = fundPlanMapper.selectListByPlanPeriod(planPeriod);
        BigDecimal receipt = BigDecimal.ZERO;
        BigDecimal payment = BigDecimal.ZERO;
        for (ErpFundPlanDO plan : list) {
            BigDecimal amount = plan.getAmount() == null ? BigDecimal.ZERO : plan.getAmount();
            if (ErpFundPlanTypeEnum.RECEIPT.getType().equals(plan.getPlanType())) {
                receipt = receipt.add(amount);
            } else if (ErpFundPlanTypeEnum.PAYMENT.getType().equals(plan.getPlanType())) {
                payment = payment.add(amount);
            }
        }
        return receipt.subtract(payment);
    }

}