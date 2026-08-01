package cn.iocoder.yudao.module.wms.controller.admin.md.sn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - WMS 序列号生成 Request VO")
@Data
public class WmsSnGenerateReqVO {

    @Schema(description = "商品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "商品编号不能为空")
    private Long productId;

    @Schema(description = "生成数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "生成数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "序列号前缀", example = "SN")
    @Size(max = 16, message = "前缀长度不能超过 16 个字符")
    private String prefix;

}