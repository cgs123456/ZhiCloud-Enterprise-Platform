package cn.zhicloud.module.wms.controller.admin.order.dock.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - WMS 月台保存 Request VO")
@Data
public class WmsDockSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @Schema(description = "月台编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "DOCK001")
    @NotBlank(message = "月台编号不能为空")
    @Size(max = 64, message = "月台编号长度不能超过 64 个字符")
    private String dockCode;

    @Schema(description = "月台名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "1号收货月台")
    @NotBlank(message = "月台名称不能为空")
    @Size(max = 128, message = "月台名称长度不能超过 128 个字符")
    private String dockName;

    @Schema(description = "月台类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "月台类型不能为空")
    private Integer dockType;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}
