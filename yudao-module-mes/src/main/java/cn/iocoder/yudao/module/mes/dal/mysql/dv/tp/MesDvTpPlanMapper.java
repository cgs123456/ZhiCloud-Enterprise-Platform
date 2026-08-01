package cn.iocoder.yudao.module.mes.dal.mysql.dv.tp;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpPlanDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * MES TPM 计划 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesDvTpPlanMapper extends BaseMapperX<MesDvTpPlanDO> {

    default PageResult<MesDvTpPlanDO> selectPage(MesDvTpPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesDvTpPlanDO>()
                .eqIfPresent(MesDvTpPlanDO::getEquipmentId, reqVO.getEquipmentId())
                .likeIfPresent(MesDvTpPlanDO::getPlanNo, reqVO.getPlanNo())
                .eqIfPresent(MesDvTpPlanDO::getPlanType, reqVO.getPlanType())
                .eqIfPresent(MesDvTpPlanDO::getCycleType, reqVO.getCycleType())
                .eqIfPresent(MesDvTpPlanDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesDvTpPlanDO::getNextExecuteDate, reqVO.getNextExecuteDate())
                .betweenIfPresent(MesDvTpPlanDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesDvTpPlanDO::getId));
    }

    default MesDvTpPlanDO selectByPlanNo(String planNo) {
        return selectOne(MesDvTpPlanDO::getPlanNo, planNo);
    }

    default List<MesDvTpPlanDO> selectOverdueList(LocalDate today) {
        return selectList(new LambdaQueryWrapperX<MesDvTpPlanDO>()
                .lt(MesDvTpPlanDO::getNextExecuteDate, today)
                .eq(MesDvTpPlanDO::getStatus, 10));
    }

}