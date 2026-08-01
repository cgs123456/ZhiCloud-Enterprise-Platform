package cn.iocoder.yudao.module.mes.dal.mysql.pro.mrp;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mrp.MesProMrpPlanDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES MRP 计划 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesProMrpPlanMapper extends BaseMapperX<MesProMrpPlanDO> {

    default PageResult<MesProMrpPlanDO> selectPage(MesProMrpPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProMrpPlanDO>()
                .likeIfPresent(MesProMrpPlanDO::getPlanNo, reqVO.getPlanNo())
                .eqIfPresent(MesProMrpPlanDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProMrpPlanDO::getPlanDate, reqVO.getPlanDate())
                .orderByDesc(MesProMrpPlanDO::getId));
    }

    default MesProMrpPlanDO selectByPlanNo(String planNo) {
        return selectOne(MesProMrpPlanDO::getPlanNo, planNo);
    }

}
