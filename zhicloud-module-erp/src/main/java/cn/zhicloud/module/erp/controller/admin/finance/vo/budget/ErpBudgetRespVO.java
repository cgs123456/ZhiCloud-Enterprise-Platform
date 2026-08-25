package cn.zhicloud.module.erp.controller.admin.finance.vo.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 预算 Response VO（P0-14）
 *
 * <p>包含预算类型与状态的中文名称，以及明细列表。
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 预算 Response VO")
@Data
public class ErpBudgetRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "预算编号", example = "BUD-2026-001")
    private String budgetNo;

    @Schema(description = "预算年度", example = "2026")
    private Integer budgetYear;

    @Schema(description = "会计期间编号", example = "1")
    private Long periodId;

    @Schema(description = "期间编码", example = "202607")
    private String periodCode;

    @Schema(description = "部门编号", example = "1")
    private Long departmentId;

    @Schema(description = "预算类型", example = "10")
    private Integer budgetType;

    @Schema(description = "预算类型名称", example = "运营预算")
    private String budgetTypeName;

    @Schema(description = "预算总额", example = "100000.00")
    private BigDecimal totalAmount;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "状态名称", example = "草稿")
    private String statusName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "预算明细列表")
    private List<ErpBudgetDetailRespVO> details;

}
