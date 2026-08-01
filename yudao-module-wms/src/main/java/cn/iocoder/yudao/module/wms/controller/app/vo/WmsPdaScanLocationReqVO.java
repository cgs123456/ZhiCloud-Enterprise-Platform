package cn.iocoder.yudao.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "用户 App - PDA 扫描库位 Request VO")
@Data
public class WmsPdaScanLocationReqVO {

    @Schema(description = "库位条码/编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "LOC001")
    @NotEmpty(message = "库位条码不能为空")
    private String scanCode;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

}
