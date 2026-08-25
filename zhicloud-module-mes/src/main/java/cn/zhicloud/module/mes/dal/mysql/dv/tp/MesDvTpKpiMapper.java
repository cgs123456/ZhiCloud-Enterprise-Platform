package cn.zhicloud.module.mes.dal.mysql.dv.tp;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.dal.dataobject.dv.tp.MesDvTpKpiDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES TPM KPI 指标 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesDvTpKpiMapper extends BaseMapperX<MesDvTpKpiDO> {

    default MesDvTpKpiDO selectByEquipmentAndPeriod(Long equipmentId, String period) {
        return selectOne(new LambdaQueryWrapperX<MesDvTpKpiDO>()
                .eq(MesDvTpKpiDO::getEquipmentId, equipmentId)
                .eq(MesDvTpKpiDO::getPeriod, period));
    }

    default List<MesDvTpKpiDO> selectListByEquipmentAndPeriods(Long equipmentId, List<String> periods) {
        return selectList(new LambdaQueryWrapperX<MesDvTpKpiDO>()
                .eq(MesDvTpKpiDO::getEquipmentId, equipmentId)
                .in(MesDvTpKpiDO::getPeriod, periods)
                .orderByAsc(MesDvTpKpiDO::getPeriod));
    }

}