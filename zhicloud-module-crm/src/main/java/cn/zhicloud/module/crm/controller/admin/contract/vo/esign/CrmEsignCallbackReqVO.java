package cn.zhicloud.module.crm.controller.admin.contract.vo.esign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 合同电子签回调 Request VO")
@Data
public class CrmEsignCallbackReqVO {

    @Schema(description = "电子签任务 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "ESIGN1690000000000")
    @NotEmpty(message = "电子签任务 ID 不能为空")
    private String esignTaskId;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "合同编号不能为空")
    private Long contractId;

    @Schema(description = "签署状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "签署状态不能为空")
    private Integer status;

    @Schema(description = "签署时间")
    private LocalDateTime signTime;

}
