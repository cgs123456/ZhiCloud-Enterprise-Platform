package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.profitability.ErpProfitabilityAnalysisPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.profitability.ErpProfitabilityAnalysisSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpProfitCenterDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpProfitabilityAnalysisDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpProfitabilityAnalysisMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 获利能力分析 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpProfitabilityAnalysisServiceImpl implements ErpProfitabilityAnalysisService {

    @Resource
    private ErpProfitabilityAnalysisMapper profitabilityAnalysisMapper;
    @Resource
    private ErpProfitCenterService profitCenterService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProfitabilityAnalysis(ErpProfitabilityAnalysisSaveReqVO createReqVO) {
        // 校验利润中心存在
        validateProfitCenter(createReqVO.getProfitCenterId());
        // 计算利润与利润率
        ErpProfitabilityAnalysisDO analysis = BeanUtils.toBean(createReqVO, ErpProfitabilityAnalysisDO.class);
        fillCalculatedFields(analysis);
        profitabilityAnalysisMapper.insert(analysis);
        return analysis.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfitabilityAnalysis(ErpProfitabilityAnalysisSaveReqVO updateReqVO) {
        // 校验存在
        validateProfitabilityAnalysisExists(updateReqVO.getId());
        // 校验利润中心存在
        validateProfitCenter(updateReqVO.getProfitCenterId());
        // 计算利润与利润率
        ErpProfitabilityAnalysisDO updateObj = BeanUtils.toBean(updateReqVO, ErpProfitabilityAnalysisDO.class);
        fillCalculatedFields(updateObj);
        profitabilityAnalysisMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProfitabilityAnalysis(Long id) {
        // 校验存在
        validateProfitabilityAnalysisExists(id);
        // 删除
        profitabilityAnalysisMapper.deleteById(id);
    }

    @Override
    public ErpProfitabilityAnalysisDO getProfitabilityAnalysis(Long id) {
        return profitabilityAnalysisMapper.selectById(id);
    }

    @Override
    public PageResult<ErpProfitabilityAnalysisDO> getProfitabilityAnalysisPage(ErpProfitabilityAnalysisPageReqVO pageReqVO) {
        return profitabilityAnalysisMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpProfitabilityAnalysisDO calculateProfitability(Long profitCenterId, Long periodId) {
        // 校验利润中心存在
        validateProfitCenter(profitCenterId);
        // 查询该利润中心 + 期间的记录
        ErpProfitabilityAnalysisDO analysis = profitabilityAnalysisMapper
                .selectByProfitCenterAndPeriod(profitCenterId, periodId);
        if (analysis == null) {
            // 不存在则新建一条空记录（revenue/cost 由后续维护）
            analysis = ErpProfitabilityAnalysisDO.builder()
                    .profitCenterId(profitCenterId)
                    .periodId(periodId)
                    .revenue(BigDecimal.ZERO)
                    .cost(BigDecimal.ZERO)
                    .build();
            fillCalculatedFields(analysis);
            profitabilityAnalysisMapper.insert(analysis);
            return analysis;
        }
        // 重新计算并回写
        fillCalculatedFields(analysis);
        profitabilityAnalysisMapper.updateById(analysis);
        return analysis;
    }

    private void validateProfitabilityAnalysisExists(Long id) {
        if (profitabilityAnalysisMapper.selectById(id) == null) {
            throw exception(PROFITABILITY_ANALYSIS_NOT_EXISTS);
        }
    }

    private void validateProfitCenter(Long id) {
        ErpProfitCenterDO center = profitCenterService.getProfitCenter(id);
        if (center == null) {
            throw exception(PROFIT_CENTER_NOT_EXISTS);
        }
    }

    /**
     * 计算利润 = 收入 - 成本，利润率 = 利润 / 收入（保留 4 位小数）
     */
    private void fillCalculatedFields(ErpProfitabilityAnalysisDO analysis) {
        BigDecimal revenue = analysis.getRevenue() == null ? BigDecimal.ZERO : analysis.getRevenue();
        BigDecimal cost = analysis.getCost() == null ? BigDecimal.ZERO : analysis.getCost();
        BigDecimal profit = revenue.subtract(cost);
        BigDecimal profitMargin = BigDecimal.ZERO;
        if (revenue.compareTo(BigDecimal.ZERO) != 0) {
            profitMargin = profit.divide(revenue, 4, RoundingMode.HALF_UP);
        }
        analysis.setRevenue(revenue);
        analysis.setCost(cost);
        analysis.setProfit(profit);
        analysis.setProfitMargin(profitMargin);
    }

}
