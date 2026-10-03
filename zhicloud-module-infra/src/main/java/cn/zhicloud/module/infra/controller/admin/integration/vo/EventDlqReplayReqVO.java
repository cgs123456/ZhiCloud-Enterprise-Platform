package cn.zhicloud.module.infra.controller.admin.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;
import static cn.zhicloud.framework.integration.service.EventDlqService.MAX_BATCH_SIZE;

@Schema(description = "管理后台 - 死信协调重放 Request VO")
@Data
public class EventDlqReplayReqVO {

    @Schema(description = "待重放事件编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "事件编号不能为空")
    @Size(max = MAX_BATCH_SIZE, message = "单次最多重放 100 个事件")
    private List<@NotBlank(message = "事件编号不能为空")
            @Size(max = 64, message = "事件编号长度不能超过 64") String> eventIds;

    @Schema(description = "重放原因，必须可追溯", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "consumer fixed in change-20260926")
    @NotBlank(message = "重放原因不能为空")
    @Size(max = 512, message = "重放原因长度不能超过 512")
    private String reason;

    @Schema(description = "事件业务发生时间起", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2026-09-25 00:00:00")
    @NotNull(message = "事件业务发生时间起不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime fromTime;

    @Schema(description = "事件业务发生时间止", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "2026-09-26 23:59:59")
    @NotNull(message = "事件业务发生时间止不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime toTime;

}