package cn.iocoder.yudao.module.ai.controller.admin.nl2sql.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - AI NL2SQL 自然语言查询 Request VO")
@Data
public class Nl2SqlQueryReqVO {

    @Schema(description = "自然语言问题", required = true, example = "查一下本月销售额前10的产品")
    @NotBlank(message = "自然语言问题不能为空")
    private String naturalLanguage;

    @Schema(description = "数据源标识（预留，用于多 schema 切换）", example = "default")
    private String dataSource;

}
