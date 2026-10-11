package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 调薪项 VO")
@Data
public class HrmSalaryChangeOptionVO {

    @Schema(description = "薪资项名称")
    private String name;

    @Schema(description = "薪资项编码")
    private Integer code;

}
