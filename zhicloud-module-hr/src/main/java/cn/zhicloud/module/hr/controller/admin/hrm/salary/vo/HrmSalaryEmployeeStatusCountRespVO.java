package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工薪资状态数量 Response VO")
@Data
public class HrmSalaryEmployeeStatusCountRespVO {

    @Schema(description = "入职状态：1在职 2待入职 3待离职 4离职")
    private Integer status;

    @Schema(description = "数量")
    private Long count;

}
