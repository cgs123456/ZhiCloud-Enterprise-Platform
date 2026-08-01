package cn.iocoder.yudao.module.oa.controller.admin.meeting.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - OA 会议室新增/修改 Request VO")
@Data
public class OaMeetingRoomSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "会议室名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "一号会议室")
    @NotEmpty(message = "会议室名称不能为空")
    private String name;

    @Schema(description = "位置", example = "A 栋 3 楼")
    private String location;

    @Schema(description = "容纳人数", example = "20")
    private Integer capacity;

    @Schema(description = "设备配置（如投影/白板/视频会议，逗号分隔）", example = "投影,白板")
    private String equipment;

    @Schema(description = "状态（10 可用 20 维修中 30 已停用）", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
