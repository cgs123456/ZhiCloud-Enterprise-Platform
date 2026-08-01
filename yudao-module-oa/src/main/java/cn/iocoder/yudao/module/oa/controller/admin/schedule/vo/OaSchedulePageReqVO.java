package cn.iocoder.yudao.module.oa.controller.admin.schedule.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 日程分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaSchedulePageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "日程类型（10 日程 / 20 任务 / 30 纪念日 / 40 会议）", example = "10")
    private Integer type;

    @Schema(description = "状态（0 未完成 / 1 已完成 / 2 已取消）", example = "0")
    private Integer status;

    @Schema(description = "开始时间范围")
    private LocalDateTime[] startTime;

}
