package cn.zhicloud.module.erp.controller.admin.collaboration.cpfr.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - ERP CPFR 异常处理 Request VO")
@Data
public class ErpCpfrExceptionHandleReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "处理状态（10 待处理 / 20 处理中 / 30 已解决）", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "处理状态不能为空")
    private Integer handlingStatus;

    @Schema(description = "处理人编号", example = "1")
    private Long handlerUserId;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
