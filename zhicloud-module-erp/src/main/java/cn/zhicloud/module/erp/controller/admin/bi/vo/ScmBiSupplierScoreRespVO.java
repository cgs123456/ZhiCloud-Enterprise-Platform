package cn.zhicloud.module.erp.controller.admin.bi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 供应链 BI 供应商绩效评分 Response VO")
@Data
public class ScmBiSupplierScoreRespVO {

    @Schema(description = "供应商编号", example = "2048")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "供应商 A")
    private String supplierName;

    @Schema(description = "到货及时率", example = "0.90")
    private BigDecimal onTimeRate;

    @Schema(description = "质量合格率（按入库单数推算）", example = "0.95")
    private BigDecimal qualityRate;

    @Schema(description = "综合绩效评分", example = "92.50")
    private BigDecimal overallScore;

}
