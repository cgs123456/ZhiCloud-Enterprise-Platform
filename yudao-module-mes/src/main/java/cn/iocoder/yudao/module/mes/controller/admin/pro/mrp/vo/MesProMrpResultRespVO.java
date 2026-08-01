package cn.iocoder.yudao.module.mes.controller.admin.pro.mrp.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES MRP 计算结果 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesProMrpResultRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "MRP 计划编号", example = "100")
    @ExcelProperty("MRP 计划编号")
    private Long planId;

    @Schema(description = "物料编号", example = "200")
    @ExcelProperty("物料编号")
    private Long productId;

    @Schema(description = "需求量", example = "100.00")
    @ExcelProperty("需求量")
    private BigDecimal requirementQty;

    @Schema(description = "库存量", example = "30.00")
    @ExcelProperty("库存量")
    private BigDecimal stockQty;

    @Schema(description = "净需求", example = "70.00")
    @ExcelProperty("净需求")
    private BigDecimal netRequirement;

    @Schema(description = "计划订单量", example = "70.00")
    @ExcelProperty("计划订单量")
    private BigDecimal plannedOrderQty;

    @Schema(description = "计划订单日期")
    @ExcelProperty("计划订单日期")
    private LocalDateTime plannedOrderDate;

    @Schema(description = "供应商编号", example = "400")
    @ExcelProperty("供应商编号")
    private Long supplierId;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

}
