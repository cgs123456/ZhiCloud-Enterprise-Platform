package cn.iocoder.yudao.module.oa.service.meeting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.controller.admin.meeting.vo.OaMeetingRoomPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.meeting.vo.OaMeetingRoomSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.meeting.OaMeetingRoomDO;
import jakarta.validation.Valid;

/**
 * OA 会议室 Service 接口
 *
 * @author yudao
 */
public interface OaMeetingRoomService {

    /**
     * 创建会议室
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMeetingRoom(@Valid OaMeetingRoomSaveReqVO createReqVO);

    /**
     * 更新会议室
     *
     * @param updateReqVO 更新信息
     */
    void updateMeetingRoom(@Valid OaMeetingRoomSaveReqVO updateReqVO);

    /**
     * 删除会议室
     *
     * @param id 编号
     */
    void deleteMeetingRoom(Long id);

    /**
     * 获得会议室
     *
     * @param id 编号
     * @return 会议室
     */
    OaMeetingRoomDO getMeetingRoom(Long id);

    /**
     * 获得会议室分页
     *
     * @param pageReqVO 分页查询
     * @return 会议室分页
     */
    PageResult<OaMeetingRoomDO> getMeetingRoomPage(OaMeetingRoomPageReqVO pageReqVO);

}
