package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 工资条模板薪资项 VO")
@Data
public class HrmSalarySlipTemplateOptionVO {

    @Schema(description = "工资项名称")
    private String name;

    @Schema(description = "工资项类型")
    private Integer type;

    @Schema(description = "工资项编码")
    private Integer code;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "父工资项编码")
    private Integer parentCode;

    @Schema(description = "是否隐藏")
    private Boolean hidden;

    @Schema(description = "排序")
    private Integer sort;

}
