package cn.zhicloud.module.tms.controller.admin.vehicle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - TMS 车辆新增/修改 Request VO")
@Data
public class TmsVehicleSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "车牌号", requiredMode = Schema.RequiredMode.REQUIRED, example = "京A12345")
    @NotEmpty(message = "车牌号不能为空")
    private String plateNo;

    @Schema(description = "车型", example = "厢式货车")
    private String vehicleType;

    @Schema(description = "承运商编号", example = "2048")
    private Long carrierId;

    @Schema(description = "载重", example = "5.0")
    private BigDecimal loadCapacity;

    @Schema(description = "容积", example = "20.0")
    private BigDecimal volume;

    @Schema(description = "司机编号", example = "4096")
    private Long driverUserId;

    // 刻意不暴露 status：车辆状态是 dispatch / 确认到达 / 签收的产物（§12.231/§12.233）。
    // 留一个可编辑的 status，调用方就能把「运输中」的车改回「可用」，绕开 dispatch 的可用性校验，
    // 同一台车会被两单同时派走。同 TmsShipmentSaveReqVO / TmsDriverSaveReqVO 的处理。

    @Schema(description = "备注", example = "随便")
    private String remark;

}
