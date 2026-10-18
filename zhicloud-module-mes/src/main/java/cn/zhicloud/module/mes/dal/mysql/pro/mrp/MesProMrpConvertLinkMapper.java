package cn.zhicloud.module.mes.dal.mysql.pro.mrp;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.mes.dal.dataobject.pro.mrp.MesProMrpConvertLinkDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES MRP 结果行→工单回链 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesProMrpConvertLinkMapper extends BaseMapperX<MesProMrpConvertLinkDO> {

    default List<MesProMrpConvertLinkDO> selectListByPlanId(Long planId) {
        return selectList(MesProMrpConvertLinkDO::getPlanId, planId);
    }

}
