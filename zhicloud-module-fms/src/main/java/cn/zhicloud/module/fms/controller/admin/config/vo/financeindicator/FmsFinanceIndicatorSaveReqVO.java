package cn.zhicloud.module.fms.controller.admin.config.vo.financeindicator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 财务指标 创建/修改 Request VO")
@Data
public class FmsFinanceIndicatorSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "指标名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "指标名称不能为空")
    private String name;

    @Schema(description = "指标编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "指标编码不能为空")
    private String code;

    @Schema(description = "取数报表类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "取数报表类型不能为空")
    private Integer type;

    @Schema(description = "指标公式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "指标公式不能为空")
    private String formula;

    @Schema(description = "展示顺序")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

}
