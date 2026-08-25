package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fundplan.ErpFundPlanPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFundPlanDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 资金计划 Mapper（P0-3）
 *
 * @author 智云
 */
@Mapper
public interface ErpFundPlanMapper extends BaseMapperX<ErpFundPlanDO> {

    default PageResult<ErpFundPlanDO> selectPage(ErpFundPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpFundPlanDO>()
                .eqIfPresent(ErpFundPlanDO::getPlanPeriod, reqVO.getPlanPeriod())
                .eqIfPresent(ErpFundPlanDO::getPlanType, reqVO.getPlanType())
                .eqIfPresent(ErpFundPlanDO::getBankAccountId, reqVO.getBankAccountId())
                .orderByDesc(ErpFundPlanDO::getPlanPeriod)
                .orderByDesc(ErpFundPlanDO::getId));
    }

    default List<ErpFundPlanDO> selectListByPlanPeriod(String planPeriod) {
        return selectList(ErpFundPlanDO::getPlanPeriod, planPeriod);
    }

}