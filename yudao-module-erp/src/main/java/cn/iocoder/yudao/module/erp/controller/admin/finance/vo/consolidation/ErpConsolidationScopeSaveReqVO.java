package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpConsolidationMethodEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 合并范围创建/修改 Request VO（P1-合并报表引擎）
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - ERP 合并范围创建/修改 Request VO")
@Data
public class ErpConsolidationScopeSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "母公司编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "母公司编号不能为空")
    private Long parentCompanyId;

    @Schema(description = "子公司编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "子公司编号不能为空")
    private Long subsidiaryCompanyId;

    @Schema(description = "持股比例（0~1）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.65")
    @NotNull(message = "持股比例不能为空")
    private BigDecimal holdingRatio;

    @Schema(description = "合并方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "合并方法不能为空")
    @InEnum(ErpConsolidationMethodEnum.class)
    private Integer consolidationMethod;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    private LocalDate expiryDate;

    @Schema(description = "状态（10 启用 / 20 禁用）", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "2026 年度合并范围")
    private String remark;

}
