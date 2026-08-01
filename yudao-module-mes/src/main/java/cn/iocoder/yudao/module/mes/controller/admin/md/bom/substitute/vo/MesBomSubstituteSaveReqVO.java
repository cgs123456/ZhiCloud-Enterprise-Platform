package cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - MES BOM 替代料创建/更新 Request VO")
@Data
public class MesBomSubstituteSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "BOM 主表 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "BOM 主表 ID 不能为空")
    private Long bomId;

    @Schema(description = "BOM 明细 ID（被替代的物料所在行）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "BOM 明细 ID 不能为空")
    private Long bomDetailId;

    @Schema(description = "替代物料 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "替代物料 ID 不能为空")
    private Long substituteItemId;

    @Schema(description = "替代比例（1 单位原物料 = ratio 单位替代料）", example = "1.500000")
    @Positive(message = "替代比例必须大于 0")
    private BigDecimal substituteRatio;

    @Schema(description = "优先级（1=首选，2=次选...）", example = "1")
    private Integer priority;

    @Schema(description = "生效日期", example = "2026-01-01")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2026-12-31")
    private LocalDate expiryDate;

    @Schema(description = "状态（0 启用 1 禁用）", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}