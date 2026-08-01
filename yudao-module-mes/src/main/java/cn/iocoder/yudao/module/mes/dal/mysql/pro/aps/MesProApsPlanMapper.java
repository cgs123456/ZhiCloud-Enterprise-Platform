package cn.iocoder.yudao.module.mes.dal.mysql.pro.aps;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.aps.vo.MesProApsPlanPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MES 排产计划 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesProApsPlanMapper extends BaseMapperX<MesProApsPlanDO> {

    default PageResult<MesProApsPlanDO> selectPage(MesProApsPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProApsPlanDO>()
                .likeIfPresent(MesProApsPlanDO::getPlanNo, reqVO.getPlanNo())
                .eqIfPresent(MesProApsPlanDO::getWorkOrderId, reqVO.getWorkOrderId())
                .eqIfPresent(MesProApsPlanDO::getProductId, reqVO.getProductId())
                .eqIfPresent(MesProApsPlanDO::getWorkstationId, reqVO.getWorkstationId())
                .eqIfPresent(MesProApsPlanDO::getPriority, reqVO.getPriority())
                .eqIfPresent(MesProApsPlanDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProApsPlanDO::getPlannedStartTime, reqVO.getPlannedStartTime())
                .orderByAsc(MesProApsPlanDO::getPlannedStartTime));
    }

    default List<MesProApsPlanDO> selectListByWorkstationAndTimeRange(Long workstationId,
                                                                       LocalDateTime startTime,
                                                                       LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<MesProApsPlanDO>()
                .eqIfPresent(MesProApsPlanDO::getWorkstationId, workstationId)
                .le(MesProApsPlanDO::getPlannedEndTime, endTime)
                .ge(MesProApsPlanDO::getPlannedStartTime, startTime)
                .orderByAsc(MesProApsPlanDO::getPlannedStartTime));
    }

    /**
     * 按工单 ID 列表查询已存在的排产计划（用于重复排产校验）
     *
     * 仅查询 DRAFT/CONFIRMED 状态的计划（已取消/已关闭的计划不阻断重排产）
     */
    default List<MesProApsPlanDO> selectListByWorkOrderIds(List<Long> workOrderIds) {
        return selectList(new LambdaQueryWrapperX<MesProApsPlanDO>()
                .in(MesProApsPlanDO::getWorkOrderId, workOrderIds)
                .in(MesProApsPlanDO::getStatus,
                        cn.iocoder.yudao.module.mes.enums.pro.MesProApsPlanStatusEnum.DRAFT.getStatus(),
                        cn.iocoder.yudao.module.mes.enums.pro.MesProApsPlanStatusEnum.CONFIRMED.getStatus()));
    }

    default MesProApsPlanDO selectByPlanNo(String planNo) {
        return selectOne(MesProApsPlanDO::getPlanNo, planNo);
    }

}
