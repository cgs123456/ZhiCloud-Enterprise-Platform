package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.profitability.ErpProfitabilityAnalysisPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpProfitabilityAnalysisDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 获利能力分析 Mapper
 *
 * @author 智云
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
