package cn.iocoder.yudao.module.mes.controller.admin.md.bom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES BOM 递归展开结果 Response VO")
@Data
public class MesBomExplodeRespVO {

    @Schema(description = "根产品编号", example = "1")
    private Long productId;

    @Schema(description = "根产品需求数量", example = "10")
    private BigDecimal quantity;

    @Schema(description = "展开层级最大深度")
    private Integer maxDepth;

    @Schema(description = "展开后的全部子件需求（已按损耗率放大）")
    private List<BomRequirement> requirements;

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class BomRequirement {

        @Schema(description = "产品编号")
        private Long productId;

        @Schema(description = "层级（1=第一层子件）")
        private Integer level;

        @Schema(description = "需求数量（含损耗）")
        private BigDecimal quantity;

        @Schema(description = "单位")
        private String unit;
    }

}