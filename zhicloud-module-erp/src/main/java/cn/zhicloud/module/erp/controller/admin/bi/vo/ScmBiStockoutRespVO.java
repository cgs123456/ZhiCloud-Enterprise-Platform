package cn.zhicloud.module.erp.controller.admin.bi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 供应链 BI 缺货率 Response VO")
@Data
public class ScmBiStockoutRespVO {

    @Schema(description = "产品总数", example = "200")
    private Integer totalProducts;

    @Schema(description = "缺货产品数（库存 <= 0）", example = "20")
    private Integer stockoutProducts;

    @Schema(description = "缺货率", example = "0.10")
    private BigDecimal stockoutRate;

}
