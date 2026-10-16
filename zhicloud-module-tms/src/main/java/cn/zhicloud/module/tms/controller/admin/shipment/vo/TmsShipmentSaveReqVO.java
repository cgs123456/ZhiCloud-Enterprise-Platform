package cn.zhicloud.module.tms.controller.admin.shipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - TMS 运单新增/修改 Request VO")
@Data
public class TmsShipmentSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "运单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TMS20240101001")
    @NotEmpty(message = "运单号不能为空")
    private String no;

    @Schema(description = "承运商编号", example = "2048")
    private Long carrierId;

    @Schema(description = "车辆编号", example = "3072")
    private Long vehicleId;

    @Schema(description = "司机编号", example = "4096")
    private Long driverId;

    @Schema(description = "起点地址", example = "北京市朝阳区")
    private String originAddress;

    @Schema(description = "终点地址", example = "上海市浦东新区")
    private String destinationAddress;

    @Schema(description = "运单类型（10 采购入库 / 20 销售出库 / 30 调拨 / 40 退货）", example = "20")
    private Integer shipmentType;

    @Schema(description = "来源单据号", example = "SO20240101001")
    private String sourceOrderNo;

    @Schema(description = "来源 WMS 出库单编号", example = "1")
    private Long shipmentOrderId;

    @Schema(description = "合计数量", example = "100")
    private BigDecimal totalQuantity;

    @Schema(description = "合计重量", example = "500.0")
    private BigDecimal totalWeight;

    @Schema(description = "合计体积", example = "20.0")
    private BigDecimal totalVolume;

    @Schema(description = "运费金额", example = "1500.00")
    private BigDecimal freightAmount;

    @Schema(description = "发车时间")
    private LocalDateTime departureTime;

    @Schema(description = "预计到达时间")
    private LocalDateTime estimatedArrivalTime;

    @Schema(description = "实际到达时间")
    private LocalDateTime actualArrivalTime;

    // 刻意不暴露 status：运单状态是状态机的产物，只能由 dispatch/confirmArrival/confirmSign
    // 推进。此前这里有一个可编辑的 status 字段（前端表单还给配了输入框），调用方能把运单
    // 直接改回「待发车」再重复派车——并发 CAS（§12.231）的前提是状态单调不回退，
    // 留这个口子等于在自己的防护上凿洞。同 MesProWorkOrderSaveReqVO 的处理。

    @Schema(description = "备注", example = "随便")
    private String remark;

}
