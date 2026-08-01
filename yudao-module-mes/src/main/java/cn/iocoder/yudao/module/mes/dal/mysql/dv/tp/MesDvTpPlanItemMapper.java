package cn.iocoder.yudao.module.mes.dal.mysql.dv.tp;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpPlanItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES TPM 计划项目 Mapper
 *
 * @author 芋道源码
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