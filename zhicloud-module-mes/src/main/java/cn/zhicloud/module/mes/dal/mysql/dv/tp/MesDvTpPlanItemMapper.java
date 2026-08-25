package cn.zhicloud.module.mes.dal.mysql.dv.tp;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.dal.dataobject.dv.tp.MesDvTpPlanItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES TPM 计划项目 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesDvTpPlanItemMapper extends BaseMapperX<MesDvTpPlanItemDO> {

    default List<MesDvTpPlanItemDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<MesDvTpPlanItemDO>()
                .eq(MesDvTpPlanItemDO::getPlanId, planId)
                .orderByAsc(MesDvTpPlanItemDO::getSort));
    }

    default int deleteByPlanId(Long planId) {
        return delete(new LambdaQueryWrapperX<MesDvTpPlanItemDO>()
                .eq(MesDvTpPlanItemDO::getPlanId, planId));
    }

}