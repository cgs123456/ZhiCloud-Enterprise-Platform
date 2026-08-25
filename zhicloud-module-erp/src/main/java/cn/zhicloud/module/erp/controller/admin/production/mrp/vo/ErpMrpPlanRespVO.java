package cn.zhicloud.module.erp.controller.admin.production.mrp.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 物料需求计划 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpMrpPlanRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MRP-001")
    @ExcelProperty("计划编号")
    private String no;

    @Schema(description = "计划名称", example = "2026年7月MRP")
    @ExcelProperty("计划名称")
    private String planName;

    @Schema(description = "计划日期")
    @ExcelProperty("计划日期")
    private LocalDate planDate;

    @Schema(description = "关联 MPS 主生产计划编号", example = "1")
    private Long mpsPlanId;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "总需求量", example = "1000.00")
    @ExcelProperty("总需求量")
    private BigDecimal totalDemandCount;

    @Schema(description = "总采购量", example = "600.00")
    @ExcelProperty("总采购量")
    private BigDecimal totalPurchaseCount;

    @Schema(description = "总生产量", example = "400.00")
    @ExcelProperty("总生产量")
    private BigDecimal totalProduceCount;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建人", example = "智云")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "MRP 结果列表")
    private List<ErpMrpResultRespVO> results;

}
