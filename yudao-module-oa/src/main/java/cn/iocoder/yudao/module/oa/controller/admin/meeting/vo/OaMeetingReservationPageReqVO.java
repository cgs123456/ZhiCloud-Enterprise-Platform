package cn.iocoder.yudao.module.oa.controller.admin.meeting.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - OA 会议室预约分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaMeetingReservationPageReqVO extends PageParam {

    @Schema(description = "会议主题", example = "季度复盘会")
    private String title;

    @Schema(description = "会议室 ID", example = "2048")
    private Long roomId;

    @Schema(description = "组织人 ID", example = "2048")
    private Long organizerUserId;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
