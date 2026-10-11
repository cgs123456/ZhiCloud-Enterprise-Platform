package cn.zhicloud.module.hr.controller.admin.hrm.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 首页待办概览 VO")
@Data
public class HrmHrHomeTodoSurveyVO {

    @Schema(description = "待入职数量")
    private Long toEntryCount;

    @Schema(description = "待离职数量")
    private Long toLeaveCount;

    @Schema(description = "合同待到期数量")
    private Long toExpireContractCount;

    @Schema(description = "待转正数量")
    private Long toRegularCount;

    @Schema(description = "待核算工资表数量")
    private Long toSalaryComputeCount;

    @Schema(description = "本月生日员工数量")
    private Long toBirthdayCount;

}
