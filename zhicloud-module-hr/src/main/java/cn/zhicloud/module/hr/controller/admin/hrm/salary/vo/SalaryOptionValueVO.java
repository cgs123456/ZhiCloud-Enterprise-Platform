package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HRM 薪资项值 VO")
@Data
public class SalaryOptionValueVO {

    @Schema(description = "薪资项编码")
    private Integer code;

    @Schema(description = "薪资项名称")
    private String name;

    @Schema(description = "薪资项金额")
    private BigDecimal value;

}
