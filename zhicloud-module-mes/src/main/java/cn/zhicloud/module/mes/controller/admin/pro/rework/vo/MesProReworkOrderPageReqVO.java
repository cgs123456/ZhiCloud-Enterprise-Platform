package cn.zhicloud.module.mes.controller.admin.pro.rework.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES 返工工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProReworkOrderPageReqVO extends PageParam {

    @Schema(description = "返工工单号", example = "RW-001")
    private String code;

    @Schema(description = "原工单 ID", example = "100")
    private Long originalWorkOrderId;

    @Schema(description = "原工单号", example = "WO-001")
    private String originalWorkOrderCode;

    @Schema(description = "产品 ID", example = "200")
    private Long productId;

    @Schema(description = "返工类型", example = "10")
    private Integer reworkType;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "责任人 ID", example = "400")
    private Long responsiblePersonId;

    @Schema(description = "计划开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] plannedStartTime;

}
