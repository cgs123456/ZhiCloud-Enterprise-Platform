package cn.zhicloud.module.mes.controller.admin.pro.aps.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES 排产计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProApsPlanPageReqVO extends PageParam {

    @Schema(description = "排产计划编号", example = "APS-001")
    private String planNo;

    @Schema(description = "生产工单编号", example = "100")
    private Long workOrderId;

    @Schema(description = "产品编号", example = "200")
    private Long productId;

    @Schema(description = "工位编号", example = "300")
    private Long workstationId;

    @Schema(description = "优先级", example = "1")
    private Integer priority;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "计划开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] plannedStartTime;

}
