package cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.erp.enums.finance.ErpConsolidationEliminationTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 合并工作底稿创建/修改 Request VO（P1-合并报表引擎）
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 合并工作底稿创建/修改 Request VO")
@Data
public class ErpConsolidationWorksheetSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "合并周期", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @NotBlank(message = "合并周期不能为空")
    private String consolidationPeriod;

    @Schema(description = "母公司编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "母公司编号不能为空")
    private Long parentCompanyId;

    @Schema(description = "子公司编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "子公司编号不能为空")
    private Long subsidiaryCompanyId;

    @Schema(description = "抵消类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "抵消类型不能为空")
    @InEnum(ErpConsolidationEliminationTypeEnum.class)
    private Integer eliminationType;

    @Schema(description = "抵消金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000.00")
    @NotNull(message = "抵消金额不能为空")
    private BigDecimal eliminationAmount;

    @Schema(description = "抵消描述", example = "投资权益抵消分录")
    private String description;

    @Schema(description = "状态（10 待审核 / 20 已审核 / 30 已驳回）", example = "10")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "集团内部投资抵消")
    private String remark;

}
