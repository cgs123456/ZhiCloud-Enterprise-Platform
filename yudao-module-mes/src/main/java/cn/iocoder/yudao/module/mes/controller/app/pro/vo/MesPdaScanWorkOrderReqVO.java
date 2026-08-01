package cn.iocoder.yudao.module.mes.controller.app.pro.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "用户 APP - PDA 扫描工单 Request VO")
@Data
public class MesPdaScanWorkOrderReqVO {

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WO202503001")
    @NotEmpty(message = "工单编号不能为空")
    private String workOrderNo;

}
