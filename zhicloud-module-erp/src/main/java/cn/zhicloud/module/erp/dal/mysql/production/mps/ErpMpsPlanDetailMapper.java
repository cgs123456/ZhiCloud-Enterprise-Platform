package cn.zhicloud.module.erp.dal.mysql.production.mps;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.production.mps.ErpMpsPlanDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 主生产计划明细 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpMpsPlanDetailMapper extends BaseMapperX<ErpMpsPlanDetailDO> {

    default List<ErpMpsPlanDetailDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<ErpMpsPlanDetailDO>()
                .eq(ErpMpsPlanDetailDO::getPlanId, planId)
                .orderByAsc(ErpMpsPlanDetailDO::getSort));
    }

    default int deleteByPlanId(Long planId) {
        return delete(new LambdaQueryWrapperX<ErpMpsPlanDetailDO>()
                .eq(ErpMpsPlanDetailDO::getPlanId, planId));
    }

}