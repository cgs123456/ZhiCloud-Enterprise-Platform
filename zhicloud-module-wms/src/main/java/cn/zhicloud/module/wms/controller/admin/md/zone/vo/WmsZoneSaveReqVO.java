package cn.zhicloud.module.wms.controller.admin.md.zone.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - WMS 库区新增/修改 Request VO")
@Data
public class WmsZoneSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "仓库 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "仓库 ID 不能为空")
    private Long warehouseId;

    @Schema(description = "库区编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "Z001")
    @NotEmpty(message = "库区编号不能为空")
    @Size(max = 64, message = "库区编号长度不能超过 64 个字符")
    private String code;

    @Schema(description = "库区名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "存储区A")
    @NotEmpty(message = "库区名称不能为空")
    private String name;

    @Schema(description = "库区类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "库区类型不能为空")
    private Integer type;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
