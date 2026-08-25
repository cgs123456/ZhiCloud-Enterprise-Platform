package cn.zhicloud.module.wms.controller.admin.abc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * WMS ABC 分析请求 VO
 *
 * @author 智云
 */
@Schema(description = "管理后台 - WMS ABC 分析请求 VO")
@Data
public class WmsAbcAnalysisReqVO {

    @Schema(description = "统计开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @NotNull(message = "统计开始时间不能为空")
    private LocalDateTime startDate;

    @Schema(description = "统计结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @NotNull(message = "统计结束时间不能为空")
    private LocalDateTime endDate;

}
