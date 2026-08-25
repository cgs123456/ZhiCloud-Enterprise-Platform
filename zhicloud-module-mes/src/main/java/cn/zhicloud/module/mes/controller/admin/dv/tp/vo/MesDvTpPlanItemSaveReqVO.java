package cn.zhicloud.module.mes.controller.admin.dv.tp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES TPM 计划项目新增/修改 Request VO")
@Data
public class MesDvTpPlanItemSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "TPM 计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "TPM 计划编号不能为空")
    private Long planId;

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "清洁检查")
    @NotEmpty(message = "项目名称不能为空")
    private String itemName;

    @Schema(description = "项目内容", example = "清洁设备表面")
    private String itemContent;

    @Schema(description = "标准", example = "无灰尘")
    private String standard;

    @Schema(description = "方法", example = "10")
    private Integer method;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}