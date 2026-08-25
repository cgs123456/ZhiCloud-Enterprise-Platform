package cn.zhicloud.module.mes.controller.admin.md.bom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES BOM 明细 Response VO")
@Data
public class MesBomDetailRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "BOM 主数据编号", example = "1")
    private Long bomId;

    @Schema(description = "子件产品编号", example = "2")
    private Long productId;

    @Schema(description = "子件产品编码", example = "C001")
    private String productCode;

    @Schema(description = "子件产品名称", example = "零件A")
    private String productName;

    @Schema(description = "用量", example = "2.5")
    private BigDecimal quantity;

    @Schema(description = "单位", example = "个")
    private String unit;

    @Schema(description = "损耗率（百分比）", example = "5")
    private BigDecimal scrapRate;

    @Schema(description = "标准单位成本", example = "12.50")
    private BigDecimal unitCost;

    @Schema(description = "备注", example = "备注")
    private String remark;

}