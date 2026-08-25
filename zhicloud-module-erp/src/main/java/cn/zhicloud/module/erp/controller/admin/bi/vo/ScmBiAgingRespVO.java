package cn.zhicloud.module.erp.controller.admin.bi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 供应链 BI 库龄分析 Response VO")
@Data
public class ScmBiAgingRespVO {

    @Schema(description = "产品编号", example = "4096")
    private Long productId;

    @Schema(description = "产品名称", example = "键盘")
    private String productName;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "库存数量", example = "100")
    private BigDecimal quantity;

    @Schema(description = "库龄天数", example = "45")
    private Integer ageDays;

    @Schema(description = "库龄分桶（0-30 / 30-60 / 60-90 / 90+）", example = "30-60")
    private String agingBucket;

}
