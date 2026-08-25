package cn.zhicloud.module.mes.controller.admin.dv.tp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES TPM KPI 计算 Request VO")
@Data
public class MesDvTpKpiCalculateReqVO {

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "设备编号不能为空")
    private Long equipmentId;

    @Schema(description = "周期（yyyyMM）", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @NotEmpty(message = "周期不能为空")
    private String period;

}