package cn.iocoder.yudao.module.erp.controller.admin.purchase.evaluation.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 供应商评估 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpSupplierEvaluationRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "供应商A")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "评估周期 yyyyMM", example = "202607")
    @ExcelProperty("评估周期")
    private String evaluationPeriod;

    @Schema(description = "质量评分", example = "90.00")
    @ExcelProperty("质量评分")
    private BigDecimal qualityScore;

    @Schema(description = "交期评分", example = "85.00")
    @ExcelProperty("交期评分")
    private BigDecimal deliveryScore;

    @Schema(description = "价格评分", example = "80.00")
    @ExcelProperty("价格评分")
    private BigDecimal priceScore;

    @Schema(description = "服务评分", example = "88.00")
    @ExcelProperty("服务评分")
    private BigDecimal serviceScore;

    @Schema(description = "综合评分", example = "86.00")
    @ExcelProperty("综合评分")
    private BigDecimal totalScore;

    @Schema(description = "等级 A/B/C/D", example = "A")
    @ExcelProperty("等级")
    private String grade;

    @Schema(description = "评估人", example = "张三")
    @ExcelProperty("评估人")
    private String evaluator;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建人", example = "芋道")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "评估指标项列表")
    private List<ErpSupplierEvaluationItemRespVO> items;

}
