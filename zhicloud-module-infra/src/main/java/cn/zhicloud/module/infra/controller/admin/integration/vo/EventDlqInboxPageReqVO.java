package cn.zhicloud.module.infra.controller.admin.integration.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - Inbox 死信分页 Request VO")
@Data
public class EventDlqInboxPageReqVO extends PageParam {

    @Schema(description = "事件编号", example = "evt-20260926-0001")
    @Size(max = 64, message = "事件编号长度不能超过 64")
    private String eventId;

    @Schema(description = "消费者名称", example = "wms.sale-out")
    @Size(max = 128, message = "消费者名称长度不能超过 128")
    private String consumerName;

    @Schema(description = "事件类型，模糊匹配", example = "erp.sale-out")
    @Size(max = 128, message = "事件类型长度不能超过 128")
    private String eventType;

    @Schema(description = "接收时间起", example = "2026-09-26 00:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime fromTime;

    @Schema(description = "接收时间止", example = "2026-09-26 23:59:59")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime toTime;

}