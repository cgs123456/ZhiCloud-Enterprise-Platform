package cn.zhicloud.module.erp.controller.admin.bi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 供应链 BI 呆滞库存分析 Response VO")
@Data
public class ScmBiSlowMovingRespVO {

    @Schema(description = "产品编号", example = "4096")
    private Long productId;

    @Schema(description = "产品名称", example = "键盘")
    private String productName;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "库存数量", example = "100")
    private BigDecimal quantity;

    @Schema(description = "最近一次出入库时间")
    private LocalDateTime lastMoveTime;

    @Schema(description = "停滞天数", example = "90")
    private Integer idleDays;

}
