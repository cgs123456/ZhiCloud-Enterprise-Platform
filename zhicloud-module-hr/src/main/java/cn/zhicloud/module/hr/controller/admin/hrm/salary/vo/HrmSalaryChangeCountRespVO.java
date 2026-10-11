package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工异动数量 Response VO")
@Data
public class HrmSalaryChangeCountRespVO {

    @Schema(description = "异动类型")
    private Integer changeType;

    @Schema(description = "数量")
    private Long count;

}
