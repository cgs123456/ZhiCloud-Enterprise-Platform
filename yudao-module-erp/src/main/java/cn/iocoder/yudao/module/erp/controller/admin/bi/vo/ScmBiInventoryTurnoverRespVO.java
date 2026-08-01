package cn.iocoder.yudao.module.erp.controller.admin.bi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 供应链 BI 库存周转率 Response VO")
@Data
public class ScmBiInventoryTurnoverRespVO {

    @Schema(description = "出库总成本", example = "100000.00")
    private BigDecimal outCostAmount;

    @Schema(description = "平均库存金额", example = "50000.00")
    private BigDecimal avgInventoryAmount;

    @Schema(description = "库存周转率", example = "2.00")
    private BigDecimal turnoverRate;

}
