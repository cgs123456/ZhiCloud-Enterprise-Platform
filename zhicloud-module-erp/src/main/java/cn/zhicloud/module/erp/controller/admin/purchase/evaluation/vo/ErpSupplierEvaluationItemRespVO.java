package cn.zhicloud.module.erp.controller.admin.purchase.evaluation.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 供应商评估指标项 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpSupplierEvaluationItemRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "评估编号", example = "1")
    private Long evaluationId;

    @Schema(description = "指标名称", example = "到货及时率")
    @ExcelProperty("指标名称")
    private String indicator;

    @Schema(description = "得分", example = "90.00")
    @ExcelProperty("得分")
    private BigDecimal score;

    @Schema(description = "权重", example = "30.00")
    @ExcelProperty("权重")
    private BigDecimal weight;

    @Schema(description = "加权得分", example = "27.00")
    @ExcelProperty("加权得分")
    private BigDecimal weightedScore;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
