package cn.iocoder.yudao.module.mes.dal.mysql.dv.tp;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES TPM 执行记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesDvTpRecordMapper extends BaseMapperX<MesDvTpRecordDO> {

    default PageResult<MesDvTpRecordDO> selectPage(MesDvTpRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesDvTpRecordDO>()
                .eqIfPresent(MesDvTpRecordDO::getPlanId, reqVO.getPlanId())
                .eqIfPresent(MesDvTpRecordDO::getEquipmentId, reqVO.getEquipmentId())
                .eqIfPresent(MesDvTpRecordDO::getExecutorId, reqVO.getExecutorId())
                .eqIfPresent(MesDvTpRecordDO::getResult, reqVO.getResult())
                .betweenIfPresent(MesDvTpRecordDO::getExecuteDate, reqVO.getExecuteDate())
                .betweenIfPresent(MesDvTpRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesDvTpRecordDO::getId));
    }

    default List<MesDvTpRecordDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<MesDvTpRecordDO>()
                .eq(MesDvTpRecordDO::getPlanId, planId)
                .orderByDesc(MesDvTpRecordDO::getExecuteDate));
    }

    default List<MesDvTpRecordDO> selectListByEquipmentIdAndPeriod(Long equipmentId, String periodStart, String periodEnd) {
        return selectList(new LambdaQueryWrapperX<MesDvTpRecordDO>()
                .eq(MesDvTpRecordDO::getEquipmentId, equipmentId)
                .between(MesDvTpRecordDO::getExecuteDate, periodStart, periodEnd)
                .orderByDesc(MesDvTpRecordDO::getExecuteDate));
    }

}