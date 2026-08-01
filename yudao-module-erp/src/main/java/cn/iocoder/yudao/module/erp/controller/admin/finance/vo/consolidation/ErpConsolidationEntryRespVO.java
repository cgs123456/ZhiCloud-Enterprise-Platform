package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 合并报表抵消分录 Response VO（P0-14）
 *
 * <p>包含抵消类型与状态的中文名称。
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - ERP 合并报表抵消分录 Response VO")
@Data
public class ErpConsolidationEntryRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "合并任务编号", example = "CONS-202607")
    private String consolidationNo;

    @Schema(description = "会计期间编号", example = "1")
    private Long periodId;

    @Schema(description = "期间编码", example = "202607")
    private String periodCode;

    @Schema(description = "抵消类型", example = "10")
    private Integer eliminationType;

    @Schema(description = "抵消类型名称", example = "投资权益抵消")
    private String eliminationTypeName;

    @Schema(description = "借方科目编号", example = "1")
    private Long debitAccountId;

    @Schema(description = "借方科目编码", example = "1122")
    private String debitAccountCode;

    @Schema(description = "借方科目名称", example = "应收账款")
    private String debitAccountName;

    @Schema(description = "贷方科目编号", example = "2")
    private Long creditAccountId;

    @Schema(description = "贷方科目编码", example = "2202")
    private String creditAccountCode;

    @Schema(description = "贷方科目名称", example = "应付账款")
    private String creditAccountName;

    @Schema(description = "抵消金额", example = "10000.00")
    private BigDecimal eliminationAmount;

    @Schema(description = "状态（10 草稿 / 20 已审核）", example = "10")
    private Integer status;

    @Schema(description = "状态名称", example = "草稿")
    private String statusName;

    @Schema(description = "备注", example = "集团内部应收应付抵消")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
