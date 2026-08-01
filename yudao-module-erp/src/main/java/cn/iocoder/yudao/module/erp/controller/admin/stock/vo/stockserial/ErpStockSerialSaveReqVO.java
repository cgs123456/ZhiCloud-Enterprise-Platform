package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stockserial;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockSerialStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - ERP 库存序列号新增/修改 Request VO")
@Data
public class ErpStockSerialSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "序列号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SN2026070001")
    @NotBlank(message = "序列号不能为空")
    private String serialNo;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "产品编号不能为空")
    private Long productId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;

    @Schema(description = "批次编号", example = "1")
    private Long batchId;

    @Schema(description = "序列号状态", example = "10")
    @InEnum(ErpStockSerialStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "高价值产品")
    private String remark;

}
