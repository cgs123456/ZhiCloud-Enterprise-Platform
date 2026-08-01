package cn.iocoder.yudao.module.wms.controller.admin.order.crossdock.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - WMS 越库单保存 Request VO")
@Data
public class WmsCrossDockOrderSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "越库单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CD202605110001")
    @NotBlank(message = "越库单号不能为空")
    @Size(max = 64, message = "越库单号长度不能超过 64 个字符")
    private String no;

    @Schema(description = "源头供应商编号", example = "1024")
    private Long sourceSupplierId;

    @Schema(description = "目标客户编号", example = "2048")
    private Long targetCustomerId;

    @Schema(description = "关联入库单号", example = "RK202605110001")
    @Size(max = 64, message = "关联入库单号长度不能超过 64 个字符")
    private String receiptOrderNo;

    @Schema(description = "关联出库单号", example = "CK202605110001")
    @Size(max = 64, message = "关联出库单号长度不能超过 64 个字符")
    private String shipmentOrderNo;

    @Schema(description = "备注", example = "备注")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

    @Schema(description = "越库明细")
    @Valid
    private List<WmsCrossDockOrderDetailSaveReqVO> details;

}
