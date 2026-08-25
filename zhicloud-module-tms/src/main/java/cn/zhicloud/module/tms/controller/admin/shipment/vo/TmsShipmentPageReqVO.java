package cn.zhicloud.module.tms.controller.admin.shipment.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - TMS 运单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TmsShipmentPageReqVO extends PageParam {

    @Schema(description = "运单号", example = "TMS20240101001")
    private String no;

    @Schema(description = "承运商编号", example = "2048")
    private Long carrierId;

    @Schema(description = "运单类型（10 采购入库 / 20 销售出库 / 30 调拨 / 40 退货）", example = "20")
    private Integer shipmentType;

    @Schema(description = "状态（10 待发车 / 20 运输中 / 30 已到达 / 40 已签收 / 50 已取消）", example = "10")
    private Integer status;

    @Schema(description = "来源单据号", example = "SO20240101001")
    private String sourceOrderNo;

}
