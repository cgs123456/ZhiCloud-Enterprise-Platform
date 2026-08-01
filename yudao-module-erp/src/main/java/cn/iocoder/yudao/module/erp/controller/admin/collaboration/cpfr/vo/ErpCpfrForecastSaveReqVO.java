package cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP CPFR 预测新增/修改 Request VO")
@Data
public class ErpCpfrForecastSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "预测单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CPFR202401001")
    @NotEmpty(message = "预测单号不能为空")
    private String no;

    @Schema(description = "合作伙伴类型（10 供应商 / 20 客户）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "合作伙伴类型不能为空")
    private Integer partnerType;

    @Schema(description = "合作伙伴编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "合作伙伴编号不能为空")
    private Long partnerId;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "产品编号不能为空")
    private Long productId;

    @Schema(description = "产品名称", example = "键盘")
    private String productName;

    @Schema(description = "预测周期（yyyyMM）", requiredMode = Schema.RequiredMode.REQUIRED, example = "202401")
    @NotEmpty(message = "预测周期不能为空")
    private String forecastPeriod;

    @Schema(description = "预测数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    @NotNull(message = "预测数量不能为空")
    private BigDecimal forecastQuantity;

    @Schema(description = "实际数量", example = "950")
    private BigDecimal actualQuantity;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
