package cn.zhicloud.module.mes.controller.admin.dv.tp.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - MES TPM 计划 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesDvTpPlanRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long equipmentId;

    @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TP-001")
    @ExcelProperty("计划编号")
    private String planNo;

    @Schema(description = "计划类型", example = "10")
    @ExcelProperty("计划类型")
    private Integer planType;

    @Schema(description = "周期类型", example = "30")
    @ExcelProperty("周期类型")
    private Integer cycleType;

    @Schema(description = "周期值", example = "1")
    @ExcelProperty("周期值")
    private Integer cycleValue;

    @Schema(description = "下次执行日期")
    @ExcelProperty("下次执行日期")
    private LocalDate nextExecuteDate;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建人", example = "智云")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "计划项目列表")
    private List<MesDvTpPlanItemRespVO> items;

}