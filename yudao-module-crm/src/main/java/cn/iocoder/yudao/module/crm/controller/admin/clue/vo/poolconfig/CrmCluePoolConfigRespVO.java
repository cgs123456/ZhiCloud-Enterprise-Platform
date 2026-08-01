package cn.iocoder.yudao.module.crm.controller.admin.clue.vo.poolconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 线索公海规则 Response VO")
@Data
public class CrmCluePoolConfigRespVO {

    @Schema(description = "是否启用线索公海", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用线索公海不能为空")
    private Boolean enabled;

    @Schema(description = "未跟进放入公海天数", example = "2")
    private Integer contactExpireDays;

    @Schema(description = "是否开启提前提醒", example = "true")
    private Boolean notifyEnabled;

    @Schema(description = "提前提醒天数", example = "2")
    private Integer notifyDays;

}
