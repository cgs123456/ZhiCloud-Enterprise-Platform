package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpConsolidationEliminationTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 合并报表抵消分录创建/更新 Request VO（P0-14）
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - ERP 合并报表抵消分录创建/更新 Request VO")
@Data
public class ErpConsolidationEntrySaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "合并任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CONS-202607")
    @NotBlank(message = "合并任务编号不能为空")
    private String consolidationNo;

    @Schema(description = "会计期间编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "会计期间编号不能为空")
    private Long periodId;

    @Schema(description = "期间编码（冗余）", example = "202607")
    private String periodCode;

    @Schema(description = "抵消类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "抵消类型不能为空")
    @InEnum(ErpConsolidationEliminationTypeEnum.class)
    private Integer eliminationType;

    @Schema(description = "借方科目编号", example = "1")
    private Long debitAccountId;

    @Schema(description = "借方科目编码（冗余）", example = "1122")
    private String debitAccountCode;

    @Schema(description = "借方科目名称（冗余）", example = "应收账款")
    private String debitAccountName;

    @Schema(description = "贷方科目编号", example = "2")
    private Long creditAccountId;

    @Schema(description = "贷方科目编码（冗余）", example = "2202")
    private String creditAccountCode;

    @Schema(description = "贷方科目名称（冗余）", example = "应付账款")
    private String creditAccountName;

    @Schema(description = "抵消金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    @NotNull(message = "抵消金额不能为空")
    @Positive(message = "抵消金额必须大于 0")
    private BigDecimal eliminationAmount;

    @Schema(description = "状态（10 草稿 / 20 已审核）", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "集团内部应收应付抵消")
    private String remark;

}
