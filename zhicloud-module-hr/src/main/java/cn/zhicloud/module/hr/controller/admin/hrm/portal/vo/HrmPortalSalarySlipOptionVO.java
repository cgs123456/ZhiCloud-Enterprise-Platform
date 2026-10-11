package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 员工自助端 - 工资条薪资项 VO（树形）
 *
 * 对齐前端 SalarySlipOptionVO 契约。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 工资条薪资项 VO")
@Data
public class HrmPortalSalarySlipOptionVO {

    @Schema(description = "薪资项名称")
    private String name;

    @Schema(description = "薪资项类型")
    private Integer type;

    @Schema(description = "薪资项编码")
    private Integer code;

    @Schema(description = "薪资项金额")
    private BigDecimal value;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子薪资项")
    private List<HrmPortalSalarySlipOptionVO> children;

}
