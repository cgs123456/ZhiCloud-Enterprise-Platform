package cn.iocoder.yudao.module.mes.controller.admin.md.bom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - MES BOM 创建/更新 Request VO")
@Data
public class MesBomSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "BOM 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BOM001")
    @NotBlank(message = "BOM 编号不能为空")
    @Size(max = 64, message = "BOM 编号长度不能超过 64 个字符")
    private String bomNo;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "产品编号不能为空")
    private Long productId;

    @Schema(description = "BOM 类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "MANUFACTURING")
    @NotBlank(message = "BOM 类型不能为空")
    private String bomType;

    @Schema(description = "版本号", example = "V1.0")
    @Size(max = 32, message = "版本号长度不能超过 32 个字符")
    private String version;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

}