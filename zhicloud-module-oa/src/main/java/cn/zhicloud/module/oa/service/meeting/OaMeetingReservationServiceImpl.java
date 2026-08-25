package cn.zhicloud.module.oa.service.meeting;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.oa.controller.admin.meeting.vo.OaMeetingReservationPageReqVO;
import cn.zhicloud.module.oa.controller.admin.meeting.vo.OaMeetingReservationSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.meeting.OaMeetingReservationDO;
import cn.zhicloud.module.oa.dal.dataobject.meeting.OaMeetingRoomDO;
import cn.zhicloud.module.oa.dal.mysql.meeting.OaMeetingReservationMapper;
import cn.zhicloud.module.oa.dal.mysql.meeting.OaMeetingRoomMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.oa.enums.ErrorCodeConstants.OA_MEETING_RESERVATION_NOT_EXISTS;
import static cn.zhicloud.module.oa.enums.ErrorCodeConstants.OA_MEETING_RESERVATION_TIME_CONFLICT;
import static cn.zhicloud.module.oa.enums.ErrorCodeConstants.OA_MEETING_ROOM_NOT_EXISTS;
import static cn.zhicloud.module.oa.enums.ErrorCodeConstants.OA_MEETING_ROOM_STATUS_INVALID;

/**
 * OA 会议室预约 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class OaMeetingReservationServiceImpl implements OaMeetingReservationService {

    /**
     * 待确认状态
     */
    private static final int STATUS_PENDING = 10;
    /**
     * 会议室可用状态
     */
    private static final int ROOM_STATUS_AVAILABLE = 10;

    @Resource
    private OaMeetingReservationMapper meetingReservationMapper;
    @Resource
    private OaMeetingRoomService meetingRoomService;
    @Resource
    private OaMeetingRoomMapper meetingRoomMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReservation(OaMeetingReservationSaveReqVO createReqVO) {
        // 悲观锁锁住会议室行，确保同一会议室的并发预订请求串行化，避免 check-then-insert 竞态
        OaMeetingRoomDO room = meetingRoomMapper.selectByIdForUpdate(createReqVO.getRoomId());
        if (room == null) {
            throw exception(OA_MEETING_ROOM_NOT_EXISTS);
        }
        if (!Integer.valueOf(ROOM_STATUS_AVAILABLE).equals(room.getStatus())) {
            throw exception(OA_MEETING_ROOM_STATUS_INVALID);
        }
        // 校验时段冲突（持锁状态下查询，避免幻读）
        if (checkTimeConflict(createReqVO.getRoomId(), createReqVO.getStartTime(),
                createReqVO.getEndTime(), null)) {
            throw exception(OA_MEETING_RESERVATION_TIME_CONFLICT);
        }
        // 插入预约（默认待确认）
        OaMeetingReservationDO reservation = BeanUtils.toBean(createReqVO, OaMeetingReservationDO.class);
        if (reservation.getStatus() == null) {
            reservation.setStatus(STATUS_PENDING);
        }
        meetingReservationMapper.insert(reservation);
        return reservation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReservation(OaMeetingReservationSaveReqVO updateReqVO) {
        // 校验存在
        validateReservationExists(updateReqVO.getId());
        // 悲观锁锁住会议室行，确保并发更新串行化
        OaMeetingRoomDO room = meetingRoomMapper.selectByIdForUpdate(updateReqVO.getRoomId());
        if (room == null) {
            throw exception(OA_MEETING_ROOM_NOT_EXISTS);
        }
        if (!Integer.valueOf(ROOM_STATUS_AVAILABLE).equals(room.getStatus())) {
            throw exception(OA_MEETING_ROOM_STATUS_INVALID);
        }
        // 校验时段冲突（排除自身）
        if (checkTimeConflict(updateReqVO.getRoomId(), updateReqVO.getStartTime(),
                updateReqVO.getEndTime(), updateReqVO.getId())) {
            throw exception(OA_MEETING_RESERVATION_TIME_CONFLICT);
        }
        // 更新
        OaMeetingReservationDO updateObj = BeanUtils.toBean(updateReqVO, OaMeetingReservationDO.class);
        meetingReservationMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReservation(Long id) {
        validateReservationExists(id);
        meetingReservationMapper.deleteById(id);
    }

    @Override
    public OaMeetingReservationDO getReservation(Long id) {
        return meetingReservationMapper.selectById(id);
    }

    @Override
    public PageResult<OaMeetingReservationDO> getReservationPage(OaMeetingReservationPageReqVO pageReqVO) {
        return meetingReservationMapper.selectPage(pageReqVO);
    }

    @Override
    public boolean checkTimeConflict(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeReservationId) {
        return !meetingReservationMapper.selectConflictList(roomId, startTime, endTime, excludeReservationId).isEmpty();
    }

    private OaMeetingReservationDO validateReservationExists(Long id) {
        OaMeetingReservationDO reservation = meetingReservationMapper.selectById(id);
        if (reservation == null) {
            throw exception(OA_MEETING_RESERVATION_NOT_EXISTS);
        }
        return reservation;
    }

    private OaMeetingRoomDO validateRoomExists(Long roomId) {
        OaMeetingRoomDO room = meetingRoomService.getMeetingRoom(roomId);
        if (room == null) {
            throw exception(OA_MEETING_ROOM_NOT_EXISTS);
        }
        return room;
    }

}
