package cn.iocoder.yudao.module.wms.controller.admin.order.crossdock.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - WMS 越库单 Response VO")
@Data
public class WmsCrossDockOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "越库单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CD202605110001")
    private String no;

    @Schema(description = "源头供应商编号", example = "1024")
    private Long sourceSupplierId;
    @Schema(description = "源头供应商名称", example = "某某供应商")
    private String sourceSupplierName;

    @Schema(description = "目标客户编号", example = "2048")
    private Long targetCustomerId;
    @Schema(description = "目标客户名称", example = "某某客户")
    private String targetCustomerName;

    @Schema(description = "关联入库单号", example = "RK202605110001")
    private String receiptOrderNo;

    @Schema(description = "关联出库单号", example = "CK202605110001")
    private String shipmentOrderNo;

    @Schema(description = "越库状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "总数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal totalQuantity;

    @Schema(description = "总金额", example = "1000.00")
    private BigDecimal totalAmount;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "越库明细")
    private List<WmsCrossDockOrderDetailRespVO> details;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "创建者", example = "1")
    private String creator;
    @Schema(description = "创建者名称", example = "芋道")
    private String creatorName;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

    @Schema(description = "更新者", example = "1")
    private String updater;
    @Schema(description = "更新者名称", example = "芋道")
    private String updaterName;

}
