package cn.iocoder.yudao.module.mes.dal.mysql.dv.tp;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpKpiDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES TPM KPI 指标 Mapper
 *
 * @author 芋道源码
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