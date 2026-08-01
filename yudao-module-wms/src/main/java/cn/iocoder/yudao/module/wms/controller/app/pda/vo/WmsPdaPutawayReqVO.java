package cn.iocoder.yudao.module.wms.controller.app.pda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PDA 上架执行请求 VO
 *
 * @author 芋道源码
 */
@Schema(description = "用户 App - PDA 上架执行 Request VO")
@Data
public class WmsPdaPutawayReqVO {

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;

    @Schema(description = "商品 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "商品 SKU 编号不能为空")
    private Long skuId;

    @Schema(description = "上架数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "上架数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "批次号", example = "BATCH202605110001")
    @Size(max = 64, message = "批次号长度不能超过 64 个字符")
    private String batchNo;

    @Schema(description = "生产日期（FEFO 保质期管理用，与批次号配合使用）", example = "2026-05-11")
    private LocalDate productionDate;

    @Schema(description = "过期日期（空表示无保质期管理）", example = "2027-05-11")
    private LocalDate expiryDate;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}
