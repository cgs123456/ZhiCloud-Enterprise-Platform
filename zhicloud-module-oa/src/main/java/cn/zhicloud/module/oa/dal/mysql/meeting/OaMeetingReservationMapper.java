package cn.zhicloud.module.oa.dal.mysql.meeting;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.controller.admin.meeting.vo.OaMeetingReservationPageReqVO;
import cn.zhicloud.module.oa.dal.dataobject.meeting.OaMeetingReservationDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OA 会议室预约 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface OaMeetingReservationMapper extends BaseMapperX<OaMeetingReservationDO> {

    default PageResult<OaMeetingReservationDO> selectPage(OaMeetingReservationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaMeetingReservationDO>()
                .likeIfPresent(OaMeetingReservationDO::getTitle, reqVO.getTitle())
                .eqIfPresent(OaMeetingReservationDO::getRoomId, reqVO.getRoomId())
                .eqIfPresent(OaMeetingReservationDO::getOrganizerUserId, reqVO.getOrganizerUserId())
                .eqIfPresent(OaMeetingReservationDO::getStatus, reqVO.getStatus())
                .orderByDesc(OaMeetingReservationDO::getStartTime));
    }

    /**
     * 查询时段冲突的预约（排除指定预约与已取消预约）：时间段存在重叠
     * <p>
     * 重叠条件：existing.start < endTime AND existing.end > startTime
     */
    default List<OaMeetingReservationDO> selectConflictList(Long roomId, LocalDateTime startTime,
                                                            LocalDateTime endTime, Long excludeReservationId) {
        return selectList(new LambdaQueryWrapperX<OaMeetingReservationDO>()
                .eq(OaMeetingReservationDO::getRoomId, roomId)
                .ne(OaMeetingReservationDO::getStatus, 30)
                .ne(excludeReservationId != null, OaMeetingReservationDO::getId, excludeReservationId)
                .lt(OaMeetingReservationDO::getStartTime, endTime)
                .gt(OaMeetingReservationDO::getEndTime, startTime));
    }

}
