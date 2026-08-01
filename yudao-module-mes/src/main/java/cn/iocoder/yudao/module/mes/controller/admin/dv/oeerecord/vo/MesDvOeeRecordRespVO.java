package cn.iocoder.yudao.module.mes.controller.admin.dv.oeerecord.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES OEE 记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesDvOeeRecordRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "设备编号", example = "100")
    @ExcelProperty("设备编号")
    private Long machineryId;

    @Schema(description = "记录日期")
    @ExcelProperty("记录日期")
    private LocalDateTime recordDate;

    @Schema(description = "计划生产时间（分钟）", example = "480.00")
    @ExcelProperty("计划生产时间")
    private BigDecimal plannedProductionTime;

    @Schema(description = "实际运行时间（分钟）", example = "420.00")
    @ExcelProperty("实际运行时间")
    private BigDecimal runTime;

    @Schema(description = "理论节拍（分钟/件）", example = "0.50")
    @ExcelProperty("理论节拍")
    private BigDecimal idealCycleTime;

    @Schema(description = "总产量", example = "800.00")
    @ExcelProperty("总产量")
    private BigDecimal totalProduced;

    @Schema(description = "合格产量", example = "760.00")
    @ExcelProperty("合格产量")
    private BigDecimal goodProduced;

    @Schema(description = "可用率", example = "0.8750")
    @ExcelProperty("可用率")
    private BigDecimal availability;

    @Schema(description = "表现率", example = "0.9524")
    @ExcelProperty("表现率")
    private BigDecimal performance;

    @Schema(description = "质量率", example = "0.9500")
    @ExcelProperty("质量率")
    private BigDecimal quality;

    @Schema(description = "OEE 值", example = "0.7917")
    @ExcelProperty("OEE 值")
    private BigDecimal oee;

    @Schema(description = "ISO 22400-2 时间稼动率 TUR", example = "0.8750")
    @ExcelProperty("时间稼动率")
    private BigDecimal timeUtilizationRate;

    @Schema(description = "ISO 22400-2 机械效率 ME", example = "0.9048")
    @ExcelProperty("机械效率")
    private BigDecimal mechanicalEfficiency;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
