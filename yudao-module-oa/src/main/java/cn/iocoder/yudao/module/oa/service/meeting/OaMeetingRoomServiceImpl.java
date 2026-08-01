package cn.iocoder.yudao.module.oa.service.meeting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.oa.controller.admin.meeting.vo.OaMeetingRoomPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.meeting.vo.OaMeetingRoomSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.meeting.OaMeetingRoomDO;
import cn.iocoder.yudao.module.oa.dal.mysql.meeting.OaMeetingRoomMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.oa.enums.ErrorCodeConstants.OA_MEETING_ROOM_NOT_EXISTS;

/**
 * OA 会议室 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class OaMeetingRoomServiceImpl implements OaMeetingRoomService {

    /**
     * 可用状态
     */
    private static final int STATUS_AVAILABLE = 10;

    @Resource
    private OaMeetingRoomMapper meetingRoomMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMeetingRoom(OaMeetingRoomSaveReqVO createReqVO) {
        OaMeetingRoomDO room = BeanUtils.toBean(createReqVO, OaMeetingRoomDO.class);
        if (room.getStatus() == null) {
            room.setStatus(STATUS_AVAILABLE);
        }
        meetingRoomMapper.insert(room);
        return room.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMeetingRoom(OaMeetingRoomSaveReqVO updateReqVO) {
        validateMeetingRoomExists(updateReqVO.getId());
        OaMeetingRoomDO updateObj = BeanUtils.toBean(updateReqVO, OaMeetingRoomDO.class);
        meetingRoomMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMeetingRoom(Long id) {
        validateMeetingRoomExists(id);
        meetingRoomMapper.deleteById(id);
    }

    @Override
    public OaMeetingRoomDO getMeetingRoom(Long id) {
        return meetingRoomMapper.selectById(id);
    }

    @Override
    public PageResult<OaMeetingRoomDO> getMeetingRoomPage(OaMeetingRoomPageReqVO pageReqVO) {
        return meetingRoomMapper.selectPage(pageReqVO);
    }

    private OaMeetingRoomDO validateMeetingRoomExists(Long id) {
        OaMeetingRoomDO room = meetingRoomMapper.selectById(id);
        if (room == null) {
            throw exception(OA_MEETING_ROOM_NOT_EXISTS);
        }
        return room;
    }

}
