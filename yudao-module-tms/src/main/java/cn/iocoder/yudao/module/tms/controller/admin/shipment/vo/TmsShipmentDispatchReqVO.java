package cn.iocoder.yudao.module.tms.controller.admin.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - TMS 运单调度 Request VO")
@Data
public class TmsShipmentDispatchReqVO {

    @Schema(description = "运单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "运单编号不能为空")
    private Long id;

    @Schema(description = "车辆编号", example = "3072")
    private Long vehicleId;

    @Schema(description = "司机编号", example = "4096")
    private Long driverId;

}
