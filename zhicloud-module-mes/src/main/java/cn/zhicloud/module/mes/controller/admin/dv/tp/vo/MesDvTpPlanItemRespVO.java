package cn.zhicloud.module.mes.controller.admin.dv.tp.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES TPM 计划项目 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesDvTpPlanItemRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "TPM 计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long planId;

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "清洁检查")
    @ExcelProperty("项目名称")
    private String itemName;

    @Schema(description = "项目内容", example = "清洁设备表面")
    @ExcelProperty("项目内容")
    private String itemContent;

    @Schema(description = "标准", example = "无灰尘")
    @ExcelProperty("标准")
    private String standard;

    @Schema(description = "方法", example = "10")
    @ExcelProperty("方法")
    private Integer method;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}