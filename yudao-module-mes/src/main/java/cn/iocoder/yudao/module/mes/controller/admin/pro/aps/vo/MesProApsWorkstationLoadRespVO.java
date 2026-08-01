package cn.iocoder.yudao.module.mes.controller.admin.pro.aps.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 工位负荷 Response VO")
@Data
public class MesProApsWorkstationLoadRespVO {

    @Schema(description = "工位编号", example = "300")
    private Long workstationId;

    @Schema(description = "工位编码", example = "WS-001")
    private String workstationCode;

    @Schema(description = "工位名称", example = "装配工位 A")
    private String workstationName;

    @Schema(description = "排产计划编号", example = "1024")
    private Long planId;

    @Schema(description = "排产计划编号", example = "APS-001")
    private String planNo;

    @Schema(description = "计划开始时间")
    private LocalDateTime plannedStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime plannedEndTime;

    @Schema(description = "排产数量", example = "100.00")
    private BigDecimal quantity;

}
