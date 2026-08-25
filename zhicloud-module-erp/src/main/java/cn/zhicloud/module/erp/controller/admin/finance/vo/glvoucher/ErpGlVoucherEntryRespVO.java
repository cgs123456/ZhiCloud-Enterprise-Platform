package cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 会计凭证分录 Response VO（P0-7）
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 会计凭证分录 Response VO")
@Data
public class ErpGlVoucherEntryRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "凭证编号", example = "1024")
    private Long voucherId;

    @Schema(description = "科目编号", example = "1024")
    private Long accountId;

    @Schema(description = "科目编码", example = "1001")
    private String accountCode;

    @Schema(description = "科目名称", example = "库存现金")
    private String accountName;

    @Schema(description = "摘要", example = "销售收款")
    private String summary;

    @Schema(description = "借方金额", example = "1000.00")
    private BigDecimal debitAmount;

    @Schema(description = "贷方金额", example = "0")
    private BigDecimal creditAmount;

    @Schema(description = "排序号", example = "1")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
