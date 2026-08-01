package cn.iocoder.yudao.module.mes.dal.mysql.pro.mrp;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mrp.MesProMrpResultDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES MRP 计算结果 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesProMrpResultMapper extends BaseMapperX<MesProMrpResultDO> {

    default List<MesProMrpResultDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<MesProMrpResultDO>()
                .eqIfPresent(MesProMrpResultDO::getPlanId, planId)
                .orderByAsc(MesProMrpResultDO::getId));
    }

    default int deleteByPlanId(Long planId) {
        return delete(new LambdaQueryWrapperX<MesProMrpResultDO>()
                .eqIfPresent(MesProMrpResultDO::getPlanId, planId));
    }

}
