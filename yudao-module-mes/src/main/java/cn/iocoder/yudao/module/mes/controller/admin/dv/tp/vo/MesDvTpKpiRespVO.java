package cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES TPM KPI 指标 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesDvTpKpiRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long equipmentId;

    @Schema(description = "周期", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @ExcelProperty("周期")
    private String period;

    @Schema(description = "平均故障间隔时间（MTBF）", example = "720.00")
    @ExcelProperty("MTBF")
    private BigDecimal mtbf;

    @Schema(description = "平均修复时间（MTTR）", example = "2.50")
    @ExcelProperty("MTTR")
    private BigDecimal mttr;

    @Schema(description = "OEE 改善值", example = "5.00")
    @ExcelProperty("OEE 改善值")
    private BigDecimal oeeImprovement;

    @Schema(description = "计划停机时间", example = "8.00")
    @ExcelProperty("计划停机时间")
    private BigDecimal plannedDowntime;

    @Schema(description = "非计划停机时间", example = "3.00")
    @ExcelProperty("非计划停机时间")
    private BigDecimal unplannedDowntime;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}