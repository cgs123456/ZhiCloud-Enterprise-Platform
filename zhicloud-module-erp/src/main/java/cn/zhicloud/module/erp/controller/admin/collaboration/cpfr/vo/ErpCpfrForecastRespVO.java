package cn.zhicloud.module.erp.controller.admin.collaboration.cpfr.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP CPFR 预测 Response VO")
@Data
public class ErpCpfrForecastRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "预测单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CPFR202401001")
    private String no;

    @Schema(description = "合作伙伴类型（10 供应商 / 20 客户）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer partnerType;

    @Schema(description = "合作伙伴编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long partnerId;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    private Long productId;

    @Schema(description = "产品名称", example = "键盘")
    private String productName;

    @Schema(description = "预测周期（yyyyMM）", requiredMode = Schema.RequiredMode.REQUIRED, example = "202401")
    private String forecastPeriod;

    @Schema(description = "预测数量", example = "1000")
    private BigDecimal forecastQuantity;

    @Schema(description = "实际数量", example = "950")
    private BigDecimal actualQuantity;

    @Schema(description = "偏差率", example = "0.05")
    private BigDecimal deviationRate;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
