package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glvoucher;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 会计凭证 Response VO（P0-7）
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - ERP 会计凭证 Response VO")
@Data
public class ErpGlVoucherRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "凭证字号", example = "记-202607-001")
    private String voucherNo;

    @Schema(description = "凭证日期")
    private LocalDate voucherDate;

    @Schema(description = "会计期间编号", example = "1024")
    private Long periodId;

    @Schema(description = "会计期间编码", example = "202607")
    private String periodCode;

    @Schema(description = "凭证类型", example = "40")
    private Integer voucherType;

    @Schema(description = "凭证类型名称", example = "记账")
    private String voucherTypeName;

    @Schema(description = "附件张数", example = "1")
    private Integer attachmentCount;

    @Schema(description = "凭证摘要", example = "本月销售收款汇总")
    private String summary;

    @Schema(description = "借方合计", example = "1000.00")
    private BigDecimal debitTotal;

    @Schema(description = "贷方合计", example = "1000.00")
    private BigDecimal creditTotal;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "状态名称", example = "草稿")
    private String statusName;

    @Schema(description = "制单人", example = "admin")
    private String preparedBy;

    @Schema(description = "审核人", example = "admin")
    private String approvedBy;

    @Schema(description = "审核时间")
    private LocalDateTime approvedTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "账簿 ID（多账簿支持，空表示默认主账簿）", example = "1")
    private Long accountBookId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "分录列表")
    private List<ErpGlVoucherEntryRespVO> entries;

}
