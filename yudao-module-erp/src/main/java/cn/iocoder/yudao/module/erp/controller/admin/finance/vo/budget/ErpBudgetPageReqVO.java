package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.budget;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 预算分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpBudgetPageReqVO extends PageParam {

    @Schema(description = "预算编号", example = "BUD-2026-001")
    private String budgetNo;

    @Schema(description = "预算年度", example = "2026")
    private Integer budgetYear;

    @Schema(description = "期间编号", example = "1")
    private Long periodId;

    @Schema(description = "部门编号", example = "1")
    private Long departmentId;

    @Schema(description = "预算类型", example = "10")
    private Integer budgetType;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
