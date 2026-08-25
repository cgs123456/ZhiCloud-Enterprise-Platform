package cn.zhicloud.module.wms.controller.admin.md.location.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - WMS 库位新增/修改 Request VO")
@Data
public class WmsLocationSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "库区 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "库区 ID 不能为空")
    private Long zoneId;

    @Schema(description = "仓库 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "仓库 ID 不能为空")
    private Long warehouseId;

    @Schema(description = "库位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "L001")
    @NotEmpty(message = "库位编号不能为空")
    @Size(max = 64, message = "库位编号长度不能超过 64 个字符")
    private String code;

    @Schema(description = "库位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "A-01-01")
    @NotEmpty(message = "库位名称不能为空")
    private String name;

    @Schema(description = "库位条码", example = "100001")
    @Size(max = 64, message = "库位条码长度不能超过 64 个字符")
    private String barcode;

    @Schema(description = "库位类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "库位类型不能为空")
    private Integer type;

    @Schema(description = "承重容量（kg）", example = "1000.00")
    private BigDecimal capacityWeight;

    @Schema(description = "容积容量（m³）", example = "10.00")
    private BigDecimal capacityVolume;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
