package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - HRM 工资条薪资项 VO")
@Data
public class HrmSalarySlipOptionVO {

    @Schema(description = "工资项名称")
    private String name;

    @Schema(description = "工资项类型")
    private Integer type;

    @Schema(description = "工资项编码")
    private Integer code;

    @Schema(description = "工资项金额")
    private BigDecimal value;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子工资条项")
    private List<HrmSalarySlipOptionVO> children;

}
