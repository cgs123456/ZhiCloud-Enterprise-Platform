package cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 会计凭证分录 Request VO（P0-7）
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 会计凭证分录 Request VO")
@Data
public class ErpGlVoucherEntryReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "科目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "科目编号不能为空")
    private Long accountId;

    @Schema(description = "摘要", example = "销售收款")
    private String summary;

    @Schema(description = "借方金额", example = "1000.00")
    @DecimalMin(value = "0", message = "借方金额不能小于 0")
    private BigDecimal debitAmount;

    @Schema(description = "贷方金额", example = "0")
    @DecimalMin(value = "0", message = "贷方金额不能小于 0")
    private BigDecimal creditAmount;

    @Schema(description = "排序号", example = "1")
    private Integer sort;

}
