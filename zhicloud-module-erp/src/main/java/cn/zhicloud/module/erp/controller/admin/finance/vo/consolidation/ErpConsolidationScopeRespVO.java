package cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 合并范围 Response VO（P1-合并报表引擎）
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 合并范围 Response VO")
@Data
public class ErpConsolidationScopeRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "母公司编号", example = "1")
    private Long parentCompanyId;

    @Schema(description = "子公司编号", example = "2")
    private Long subsidiaryCompanyId;

    @Schema(description = "持股比例", example = "0.65")
    private BigDecimal holdingRatio;

    @Schema(description = "合并方法", example = "10")
    private Integer consolidationMethod;

    @Schema(description = "合并方法名称", example = "完全合并")
    private String consolidationMethodName;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    private LocalDate expiryDate;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "状态名称", example = "启用")
    private String statusName;

    @Schema(description = "备注", example = "2026 年度合并范围")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
