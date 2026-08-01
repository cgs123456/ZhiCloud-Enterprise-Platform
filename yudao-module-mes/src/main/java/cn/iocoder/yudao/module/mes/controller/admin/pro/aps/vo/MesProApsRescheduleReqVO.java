package cn.iocoder.yudao.module.mes.controller.admin.pro.aps.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES 排产重排 Request VO")
@Data
public class MesProApsRescheduleReqVO {

    @Schema(description = "排产计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "排产计划编号不能为空")
    private Long planId;

    @Schema(description = "新的开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "新的开始时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime newStartTime;

}
