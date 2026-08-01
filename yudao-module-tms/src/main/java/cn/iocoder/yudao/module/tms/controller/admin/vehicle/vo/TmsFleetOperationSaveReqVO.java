package cn.iocoder.yudao.module.tms.controller.admin.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - TMS 车队运营新增/修改 Request VO")
@Data
public class TmsFleetOperationSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "车辆编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "车辆不能为空")
    private Long vehicleId;

    @Schema(description = "运营日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08-01")
    @NotNull(message = "运营日期不能为空")
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

    @Schema(description = "备注", example = "正常运营")
    private String remark;

}
