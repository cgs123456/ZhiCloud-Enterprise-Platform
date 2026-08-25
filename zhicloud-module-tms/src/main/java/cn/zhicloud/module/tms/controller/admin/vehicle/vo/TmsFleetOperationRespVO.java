package cn.zhicloud.module.tms.controller.admin.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - TMS 车队运营 Response VO")
@Data
public class TmsFleetOperationRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "车辆编号", example = "1")
    private Long vehicleId;

    @Schema(description = "运营日期", example = "2026-08-01")
    private LocalDate operationDate;

    @Schema(description = "行驶里程（公里）", example = "350.5")
    private BigDecimal mileage;

    @Schema(description = "油耗（升）", example = "45.2")
    private BigDecimal fuelConsumption;

    @Schema(description = "油费（元）", example = "360.00")
    private BigDecimal fuelCost;

    @Schema(description = "维修保养费（元）", example = "500.00")
    private BigDecimal maintenanceCost;

    @Schema(description = "保险费（元）", example = "0.00")
    private BigDecimal insuranceCost;

    @Schema(description = "年检费（元）", example = "0.00")
    private BigDecimal inspectionCost;

    @Schema(description = "运营收入（元）", example = "3000.00")
    private BigDecimal revenue;

    @Schema(description = "总运营成本（元）", example = "860.00")
    private BigDecimal totalCost;

    @Schema(description = "运营利润（元）", example = "2140.00")
    private BigDecimal profit;

    @Schema(description = "备注", example = "正常运营")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
