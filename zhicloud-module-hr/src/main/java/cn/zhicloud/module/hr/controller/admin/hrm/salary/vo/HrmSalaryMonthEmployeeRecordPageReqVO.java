package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HRM 员工月度工资分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmSalaryMonthEmployeeRecordPageReqVO extends PageParam {

    @Schema(description = "月度工资表编号")
    private Long monthRecordId;

    @Schema(description = "员工编号")
    private Long employeeId;

    @Schema(description = "员工姓名，模糊匹配")
    private String employeeName;

    @Schema(description = "工号，模糊匹配")
    private String jobNumber;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "员工异动类型")
    private Integer employeeChangeType;

    @Schema(description = "工资条是否已发放")
    private Boolean salarySlipSent;

    @Schema(description = "关键字（姓名/工号/手机号），模糊匹配")
    private String search;

}
