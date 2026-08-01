package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitability.ErpProfitabilityAnalysisPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpProfitabilityAnalysisDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 获利能力分析 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpProfitabilityAnalysisMapper extends BaseMapperX<ErpProfitabilityAnalysisDO> {

    default ErpProfitabilityAnalysisDO selectByProfitCenterAndPeriod(Long profitCenterId, Long periodId) {
        return selectOne(new LambdaQueryWrapperX<ErpProfitabilityAnalysisDO>()
                .eq(ErpProfitabilityAnalysisDO::getProfitCenterId, profitCenterId)
                .eq(ErpProfitabilityAnalysisDO::getPeriodId, periodId));
    }

    default PageResult<ErpProfitabilityAnalysisDO> selectPage(ErpProfitabilityAnalysisPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpProfitabilityAnalysisDO>()
                .eqIfPresent(ErpProfitabilityAnalysisDO::getProfitCenterId, reqVO.getProfitCenterId())
                .eqIfPresent(ErpProfitabilityAnalysisDO::getPeriodId, reqVO.getPeriodId())
                .orderByDesc(ErpProfitabilityAnalysisDO::getId));
    }

}
