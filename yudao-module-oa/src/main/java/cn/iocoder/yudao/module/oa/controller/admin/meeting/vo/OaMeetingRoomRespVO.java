package cn.iocoder.yudao.module.oa.controller.admin.meeting.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 会议室 Response VO")
@Data
@ExcelIgnoreUnannotated
public class OaMeetingRoomRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "会议室名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "一号会议室")
    @ExcelProperty("会议室名称")
    private String name;

    @Schema(description = "位置", example = "A 栋 3 楼")
    @ExcelProperty("位置")
    private String location;

    @Schema(description = "容纳人数", example = "20")
    @ExcelProperty("容纳人数")
    private Integer capacity;

    @Schema(description = "设备配置（如投影/白板/视频会议，逗号分隔）", example = "投影,白板")
    @ExcelProperty("设备配置")
    private String equipment;

    @Schema(description = "状态（10 可用 20 维修中 30 已停用）", example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
