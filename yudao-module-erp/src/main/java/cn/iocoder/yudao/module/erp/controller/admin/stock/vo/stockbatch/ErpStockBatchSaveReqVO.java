package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stockbatch;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockBatchStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 库存批次新增/修改 Request VO")
@Data
public class ErpStockBatchSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED, example = "B2026070001")
    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "产品编号不能为空")
    private Long productId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;

    @Schema(description = "生产日期", example = "2026-07-01")
    private LocalDate productionDate;

    @Schema(description = "过期日期", example = "2027-07-01")
    private LocalDate expiryDate;

    @Schema(description = "批次数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "批次数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "批次状态", example = "10")
    @InEnum(ErpStockBatchStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "首批")
    private String remark;

}
