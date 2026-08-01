package cn.iocoder.yudao.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "用户 App - PDA 扫描 SKU Request VO")
@Data
public class WmsPdaScanSkuReqVO {

    @Schema(description = "SKU 条码/编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SKU001")
    @NotEmpty(message = "SKU 条码不能为空")
    private String scanCode;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

}
