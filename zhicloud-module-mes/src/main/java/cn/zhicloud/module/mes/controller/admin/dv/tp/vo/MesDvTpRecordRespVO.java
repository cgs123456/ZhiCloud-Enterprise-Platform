package cn.zhicloud.module.mes.controller.admin.dv.tp.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES TPM 执行记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesDvTpRecordRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "TPM 计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long planId;

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long equipmentId;

    @Schema(description = "执行日期")
    @ExcelProperty("执行日期")
    private LocalDate executeDate;

    @Schema(description = "执行人编号", example = "1")
    private Long executorId;

    @Schema(description = "结果", example = "10")
    @ExcelProperty("结果")
    private Integer result;

    @Schema(description = "发现问题", example = "无")
    @ExcelProperty("发现问题")
    private String issuesFound;

    @Schema(description = "已采取措施", example = "已清洁")
    @ExcelProperty("已采取措施")
    private String actionTaken;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建人", example = "智云")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}