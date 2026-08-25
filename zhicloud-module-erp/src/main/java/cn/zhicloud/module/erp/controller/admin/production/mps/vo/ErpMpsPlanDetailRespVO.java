package cn.zhicloud.module.erp.controller.admin.production.mps.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 主生产计划明细 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpMpsPlanDetailRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "主生产计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long planId;

    @Schema(description = "时段开始日期")
    @ExcelProperty("时段开始日期")
    private LocalDate periodStart;

    @Schema(description = "时段结束日期")
    @ExcelProperty("时段结束日期")
    private LocalDate periodEnd;

    @Schema(description = "毛需求", example = "100.00")
    @ExcelProperty("毛需求")
    private BigDecimal grossRequirement;

    @Schema(description = "计划接收", example = "0.00")
    @ExcelProperty("计划接收")
    private BigDecimal scheduledReceipt;

    @Schema(description = "预计可用库存", example = "50.00")
    @ExcelProperty("预计可用库存")
    private BigDecimal projectedAvailableBalance;

    @Schema(description = "计划订单接收", example = "0.00")
    @ExcelProperty("计划订单接收")
    private BigDecimal plannedOrderReceipt;

    @Schema(description = "计划订单下达", example = "0.00")
    @ExcelProperty("计划订单下达")
    private BigDecimal plannedOrderRelease;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}