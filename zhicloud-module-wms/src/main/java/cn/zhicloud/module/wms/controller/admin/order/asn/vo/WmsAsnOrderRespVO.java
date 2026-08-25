package cn.zhicloud.module.wms.controller.admin.order.asn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - WMS ASN 到货通知单 Response VO")
@Data
public class WmsAsnOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "ASN 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASN202605110001")
    private String no;

    @Schema(description = "供应商编号", example = "1024")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "某某公司")
    private String supplierName;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "北京仓")
    private String warehouseName;

    @Schema(description = "月台编号", example = "1024")
    private Long dockId;

    @Schema(description = "月台名称", example = "1号收货月台")
    private String dockName;

    @Schema(description = "预计到货时间")
    private LocalDateTime expectedArrivalTime;

    @Schema(description = "实际到货时间")
    private LocalDateTime actualArrivalTime;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "总数量", example = "100.00")
    private BigDecimal totalQuantity;

    @Schema(description = "总金额", example = "1000.00")
    private BigDecimal totalAmount;

    @Schema(description = "运输方式", example = "10")
    private Integer transportMode;

    @Schema(description = "承运商", example = "顺丰物流")
    private String carrierName;

    @Schema(description = "车牌号", example = "京A12345")
    private String vehicleNo;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "ASN 明细")
    private List<WmsAsnOrderDetailRespVO> details;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "创建者", example = "1")
    private String creator;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

}
