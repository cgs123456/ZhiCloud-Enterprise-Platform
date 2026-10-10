package cn.zhicloud.module.fms.controller.admin.config.vo.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Schema(description = "管理后台 - FMS 币别 创建/修改 Request VO")
@Data
public class FmsCurrencySaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "币别编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "币别编码不能为空")
    private String code;

    @Schema(description = "币别名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "币别名称不能为空")
    private String name;

    @Schema(description = "汇率", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "汇率不能为空")
    private BigDecimal exchangeRate;

    @Schema(description = "是否本位币")
    private Boolean standard;

}
