package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工部门统计 Response VO")
@Data
public class HrmEmployeeDeptStatisticsRespVO {

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "在职员工人数")
    private Long activeCount;

    @Schema(description = "全职员工人数")
    private Long fullTimeCount;

    @Schema(description = "非全职员工人数")
    private Long nonFullTimeCount;

}
