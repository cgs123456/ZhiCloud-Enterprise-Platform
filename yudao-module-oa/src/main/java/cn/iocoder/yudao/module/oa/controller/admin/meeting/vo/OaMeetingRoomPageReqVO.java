package cn.iocoder.yudao.module.oa.controller.admin.meeting.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - OA 会议室分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaMeetingRoomPageReqVO extends PageParam {

    @Schema(description = "会议室名称", example = "一号会议室")
    private String name;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
