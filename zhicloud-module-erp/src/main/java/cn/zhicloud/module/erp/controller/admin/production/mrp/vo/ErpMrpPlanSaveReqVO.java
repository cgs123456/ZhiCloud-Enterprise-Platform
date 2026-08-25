package cn.zhicloud.module.erp.controller.admin.production.mrp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 物料需求计划新增/修改 Request VO")
@Data
public class ErpMrpPlanSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MRP-001")
    @NotEmpty(message = "计划编号不能为空")
    private String no;

    @Schema(description = "计划名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026年7月MRP")
    @NotEmpty(message = "计划名称不能为空")
    private String planName;

    @Schema(description = "计划日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划日期不能为空")
    private LocalDate planDate;

    @Schema(description = "关联 MPS 主生产计划编号", example = "1")
    private Long mpsPlanId;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
