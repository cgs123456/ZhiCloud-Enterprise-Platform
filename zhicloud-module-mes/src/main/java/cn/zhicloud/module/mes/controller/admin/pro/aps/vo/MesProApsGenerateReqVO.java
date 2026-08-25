package cn.zhicloud.module.mes.controller.admin.pro.aps.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES 排产生成 Request VO")
@Data
public class MesProApsGenerateReqVO {

    @Schema(description = "生产工单编号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[100,101]")
    @NotEmpty(message = "排产工单列表不能为空")
    private List<Long> workOrderIds;

    @Schema(description = "排产开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排产开始时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime startDate;

    @Schema(description = "排产结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排产结束时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endDate;

}
