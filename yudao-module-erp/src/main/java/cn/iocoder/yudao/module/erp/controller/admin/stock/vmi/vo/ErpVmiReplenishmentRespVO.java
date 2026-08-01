package cn.iocoder.yudao.module.erp.controller.admin.stock.vmi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP VMI 补货建议 Response VO")
@Data
public class ErpVmiReplenishmentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "补货建议单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "VMI20240101001")
    private String no;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long supplierId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long warehouseId;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "合计数量", example = "100")
    private BigDecimal totalQuantity;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "补货建议明细列表")
    private List<ErpVmiReplenishmentItemRespVO> items;

}
