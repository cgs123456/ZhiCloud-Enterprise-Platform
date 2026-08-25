package cn.zhicloud.module.mes.controller.admin.pro.mrp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - MES MRP 计划创建 Request VO")
@Data
public class MesProMrpPlanCreateReqVO {

    @Schema(description = "MRP 计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MRP-001")
    @NotEmpty(message = "MRP 计划编号不能为空")
    private String planNo;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
