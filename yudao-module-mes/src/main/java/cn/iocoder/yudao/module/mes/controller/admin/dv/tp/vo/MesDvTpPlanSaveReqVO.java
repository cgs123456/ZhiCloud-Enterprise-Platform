package cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - MES TPM 计划新增/修改 Request VO")
@Data
public class MesDvTpPlanSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "设备编号不能为空")
    private Long equipmentId;

    @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TP-001")
    @NotEmpty(message = "计划编号不能为空")
    private String planNo;

    @Schema(description = "计划类型", example = "10")
    private Integer planType;

    @Schema(description = "周期类型", example = "30")
    private Integer cycleType;

    @Schema(description = "周期值", example = "1")
    private Integer cycleValue;

    @Schema(description = "下次执行日期")
    private LocalDate nextExecuteDate;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}