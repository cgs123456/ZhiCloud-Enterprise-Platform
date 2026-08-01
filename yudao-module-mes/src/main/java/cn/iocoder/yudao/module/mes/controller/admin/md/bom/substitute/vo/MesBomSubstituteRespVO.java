package cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - MES BOM 替代料 Response VO")
@Data
public class MesBomSubstituteRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "BOM 主表 ID", example = "1")
    private Long bomId;

    @Schema(description = "BOM 明细 ID", example = "10")
    private Long bomDetailId;

    @Schema(description = "替代物料 ID", example = "20")
    private Long substituteItemId;

    @Schema(description = "替代物料编码", example = "C002")
    private String substituteItemCode;

    @Schema(description = "替代物料名称", example = "零件B")
    private String substituteItemName;

    @Schema(description = "替代比例", example = "1.500000")
    private BigDecimal substituteRatio;

    @Schema(description = "优先级", example = "1")
    private Integer priority;

    @Schema(description = "生效日期", example = "2026-01-01")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2026-12-31")
    private LocalDate expiryDate;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

}