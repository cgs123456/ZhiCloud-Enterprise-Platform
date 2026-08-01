package cn.iocoder.yudao.module.tms.controller.admin.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - TMS 运单站点 Response VO")
@Data
public class TmsShipmentStopRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "运单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long shipmentId;

    @Schema(description = "站点顺序", example = "1")
    private Integer sequenceNo;

    @Schema(description = "站点地址", example = "北京市朝阳区中转站")
    private String address;

    @Schema(description = "到达时间")
    private LocalDateTime arrivalTime;

    @Schema(description = "离开时间")
    private LocalDateTime departureTime;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
