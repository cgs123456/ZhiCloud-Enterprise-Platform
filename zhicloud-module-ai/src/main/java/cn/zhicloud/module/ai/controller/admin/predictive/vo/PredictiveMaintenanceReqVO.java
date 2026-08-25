package cn.zhicloud.module.ai.controller.admin.predictive.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 预测性维护 Request VO")
@Data
public class PredictiveMaintenanceReqVO {

    @Schema(description = "设备编号", required = true, example = "1024")
    @NotNull(message = "设备编号不能为空")
    private Long deviceId;

}
