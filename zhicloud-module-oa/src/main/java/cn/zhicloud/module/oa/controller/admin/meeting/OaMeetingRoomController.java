package cn.zhicloud.module.oa.controller.admin.meeting;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.oa.controller.admin.meeting.vo.OaMeetingRoomPageReqVO;
import cn.zhicloud.module.oa.controller.admin.meeting.vo.OaMeetingRoomRespVO;
import cn.zhicloud.module.oa.controller.admin.meeting.vo.OaMeetingRoomSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.meeting.OaMeetingRoomDO;
import cn.zhicloud.module.oa.service.meeting.OaMeetingRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - OA 会议室管理")
@RestController
@RequestMapping("/oa/meeting-room")
@Validated
public class OaMeetingRoomController {

    @Resource
    private OaMeetingRoomService meetingRoomService;

    @PostMapping("/create")
    @Operation(summary = "创建会议室")
    @PreAuthorize("@ss.hasPermission('oa:meeting-room:create')")
    public CommonResult<Long> createMeetingRoom(@Valid @RequestBody OaMeetingRoomSaveReqVO createReqVO) {
        return success(meetingRoomService.createMeetingRoom(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会议室")
    @PreAuthorize("@ss.hasPermission('oa:meeting-room:update')")
    public CommonResult<Boolean> updateMeetingRoom(@Valid @RequestBody OaMeetingRoomSaveReqVO updateReqVO) {
        meetingRoomService.updateMeetingRoom(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会议室")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:meeting-room:delete')")
    public CommonResult<Boolean> deleteMeetingRoom(@RequestParam("id") Long id) {
        meetingRoomService.deleteMeetingRoom(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会议室")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:meeting-room:query')")
    public CommonResult<OaMeetingRoomRespVO> getMeetingRoom(@RequestParam("id") Long id) {
        OaMeetingRoomDO room = meetingRoomService.getMeetingRoom(id);
        return success(BeanUtils.toBean(room, OaMeetingRoomRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会议室分页")
    @PreAuthorize("@ss.hasPermission('oa:meeting-room:query')")
    public CommonResult<PageResult<OaMeetingRoomRespVO>> getMeetingRoomPage(@Valid OaMeetingRoomPageReqVO pageReqVO) {
        PageResult<OaMeetingRoomDO> pageResult = meetingRoomService.getMeetingRoomPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaMeetingRoomRespVO.class));
    }

}
