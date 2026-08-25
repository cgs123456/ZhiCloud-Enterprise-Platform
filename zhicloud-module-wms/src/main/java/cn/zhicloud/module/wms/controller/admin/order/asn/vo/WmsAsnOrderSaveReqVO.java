package cn.zhicloud.module.wms.controller.admin.order.asn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - WMS ASN 到货通知单保存 Request VO")
@Data
public class WmsAsnOrderSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "ASN 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASN202605110001")
    @NotBlank(message = "ASN 编号不能为空")
    @Size(max = 64, message = "ASN 编号长度不能超过 64 个字符")
    private String no;

    @Schema(description = "供应商编号", example = "1024")
    private Long supplierId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @Schema(description = "月台编号", example = "1024")
    private Long dockId;

    @Schema(description = "预计到货时间")
    private LocalDateTime expectedArrivalTime;

    @Schema(description = "运输方式", example = "10")
    private Integer transportMode;

    @Schema(description = "承运商", example = "顺丰物流")
    @Size(max = 64, message = "承运商长度不能超过 64 个字符")
    private String carrierName;

    @Schema(description = "车牌号", example = "京A12345")
    @Size(max = 32, message = "车牌号长度不能超过 32 个字符")
    private String vehicleNo;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "ASN 明细")
    @Valid
    private List<WmsAsnOrderDetailSaveReqVO> details;

    @Schema(description = "总数量", example = "100.00")
    private BigDecimal totalQuantity;

    @Schema(description = "总金额", example = "1000.00")
    private BigDecimal totalAmount;

}
