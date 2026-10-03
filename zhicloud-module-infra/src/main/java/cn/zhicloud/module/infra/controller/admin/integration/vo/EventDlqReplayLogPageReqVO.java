package cn.zhicloud.module.infra.controller.admin.integration.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 死信重放审计分页 Request VO")
@Data
public class EventDlqReplayLogPageReqVO extends PageParam {

    @Schema(description = "事件编号", example = "evt-20260926-0001")
    @Size(max = 64, message = "事件编号长度不能超过 64")
    private String eventId;

    @Schema(description = "操作者用户编号", example = "1001")
    private Long operatorId;

    @Schema(description = "重放结果：SUCCESS / FAILED", example = "SUCCESS")
    @Size(max = 16, message = "重放结果长度不能超过 16")
    @Pattern(regexp = "SUCCESS|FAILED", message = "重放结果只能是 SUCCESS 或 FAILED")
    private String result;

    @Schema(description = "审计创建时间起", example = "2026-09-26 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime fromTime;

    @Schema(description = "审计创建时间止", example = "2026-09-26 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime toTime;

}
