package cn.zhicloud.module.mes.controller.admin.md.bom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES BOM 明细创建/更新 Request VO")
@Data
public class MesBomDetailSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "BOM 主数据编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "BOM 主数据编号不能为空")
    private Long bomId;

    @Schema(description = "子件产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "子件产品编号不能为空")
    private Long productId;

    @Schema(description = "用量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2.5")
    @NotNull(message = "用量不能为空")
    private BigDecimal quantity;

    @Schema(description = "单位", example = "个")
    @Size(max = 20, message = "单位长度不能超过 20 个字符")
    private String unit;

    @Schema(description = "损耗率（百分比，0-100）", example = "5")
    private BigDecimal scrapRate;

    @Schema(description = "标准单位成本", example = "12.50")
    private BigDecimal unitCost;

    @Schema(description = "备注", example = "备注")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

}